package dev.rahul.quiettv

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

/** One preloaded UI tone with its own gain. Never changes system volume or takes audio focus. */
class HomeNavigationSound(private val context:Context,
    private val prefs:SharedPreferences=context.getSharedPreferences("home",Context.MODE_PRIVATE)) {
    companion object {
        const val ENABLED_KEY="navigationSoundEnabled"
        const val VOLUME_KEY="navigationSoundVolume"
    }
    private var pool:SoundPool?=null
    private var soundId=0
    private var ready=false
    private var lastPlay=0L
    private var plays=0
    private var streamId=0
    var enabled:Boolean
        get()=prefs.getBoolean(ENABLED_KEY,true)
        set(value) {prefs.edit().putBoolean(ENABLED_KEY,value).apply();updateGain()}
    var volume:Int
        get()=prefs.getInt(VOLUME_KEY,50).coerceIn(0,100)
        set(value) {prefs.edit().putInt(VOLUME_KEY,value.coerceIn(0,100)).apply();updateGain()}
    private val gain get()=if(enabled)volume/100f else 0f
    private fun updateGain() {
        if(gain==0f) {pool?.stop(streamId);streamId=0}
        else pool?.setVolume(streamId,gain,gain)
    }

    fun start() {
        if(pool!=null)return
        val soundPool=SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
        pool=soundPool
        soundPool.setOnLoadCompleteListener {loaded,id,status->
            if(pool===loaded && id==soundId)ready=status==0
        }
        soundId=soundPool.load(context,R.raw.home_navigation,1)
    }
    fun play() {
        val active=pool ?: return
        val now=SystemClock.uptimeMillis()
        val level=gain
        if(!ready || level==0f || now-lastPlay<90)return
        streamId=active.play(soundId,level,level,1,0,1f)
        if(streamId!=0) {lastPlay=now;plays++}
    }
    fun stop() {
        val active=pool;pool=null;ready=false;soundId=0;lastPlay=0;streamId=0
        active?.release()
    }
    fun diagnostics()="active=${pool!=null} ready=$ready plays=$plays enabled=$enabled volume=$volume gain=$gain"
}
