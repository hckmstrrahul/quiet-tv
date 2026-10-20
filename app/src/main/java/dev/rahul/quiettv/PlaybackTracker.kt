package dev.rahul.quiettv

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/** Foreground-scoped session subscriptions. Does not request audio focus or play audio. */
class PlaybackTracker(private val context: Context, private val changed: (MediaController?, Boolean) -> Unit) {
    private val manager=context.getSystemService(MediaSessionManager::class.java)
    private val handler=Handler(Looper.getMainLooper())
    private val component=ComponentName(context,MediaAccessService::class.java)
    private val controllers=linkedMapOf<MediaSession.Token,Pair<MediaController,MediaController.Callback>>()
    private var running=false
    private var listening=false
    private val listener=MediaSessionManager.OnActiveSessionsChangedListener { if(running)refresh() }
    private val accessObserver=object:ContentObserver(handler) {override fun onChange(selfChange:Boolean) {if(running)refresh()} }
    fun hasAccess():Boolean = if(android.os.Build.VERSION.SDK_INT>=27)
        context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component)
        else Settings.Secure.getString(context.contentResolver,"enabled_notification_listeners")?.split(':')?.any {ComponentName.unflattenFromString(it)==component}==true
    fun start() {
        stop();running=true
        context.contentResolver.registerContentObserver(Settings.Secure.getUriFor("enabled_notification_listeners"),false,accessObserver)
        refresh()
    }
    private fun refresh() {
        if(!running)return
        if(!hasAccess()) {clearControllers();if(listening)manager.removeOnActiveSessionsChangedListener(listener);listening=false;changed(null,false);return}
        try {
            if(!listening) {manager.addOnActiveSessionsChangedListener(listener,component,handler);listening=true}
            val sessions=manager.getActiveSessions(component)
            val tokens=sessions.map {it.sessionToken}.toSet()
            controllers.keys.filter {it !in tokens}.toList().forEach {remove(it)}
            sessions.forEach {controller->
                val token=controller.sessionToken
                if(token !in controllers) {
                    val callback=object:MediaController.Callback() {
                        override fun onMetadataChanged(metadata:android.media.MediaMetadata?) {deliver()}
                        override fun onPlaybackStateChanged(state:PlaybackState?) {deliver()}
                        override fun onSessionDestroyed() {remove(token);deliver()}
                    }
                    controllers[token]=controller to callback
                    controller.registerCallback(callback,handler)
                }
            }
            // Preserve Android's priority order for equally ranked sessions.
            val ordered=sessions.mapNotNull {controllers[it.sessionToken]}
            controllers.clear();ordered.forEach {controllers[it.first.sessionToken]=it}
            deliver()
        } catch(_:SecurityException) {clearControllers();changed(null,false)}
    }
    private fun deliver() {
        if(!running)return
        val eligible=controllers.values.map {it.first}.filter {
            it.playbackState?.state in listOf(PlaybackState.STATE_PLAYING,PlaybackState.STATE_BUFFERING,
                PlaybackState.STATE_CONNECTING,PlaybackState.STATE_PAUSED,PlaybackState.STATE_FAST_FORWARDING,PlaybackState.STATE_REWINDING)
        }
        val selected=eligible.minByOrNull {if(it.playbackState?.state==PlaybackState.STATE_PAUSED)1 else 0}
        changed(selected,true)
    }
    private fun remove(token:MediaSession.Token) {controllers.remove(token)?.let {it.first.unregisterCallback(it.second)} }
    private fun clearControllers() {controllers.values.forEach {it.first.unregisterCallback(it.second)};controllers.clear()}
    fun stop() {
        if(running)context.contentResolver.unregisterContentObserver(accessObserver)
        running=false
        if(listening)manager.removeOnActiveSessionsChangedListener(listener)
        listening=false;clearControllers();handler.removeCallbacksAndMessages(null)
    }
    fun diagnostics()="active=$running listening=$listening controllers=${controllers.size} access=${hasAccess()}"
}
