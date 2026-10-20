package dev.rahul.quiettv

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

/** One tiny, preloaded UI tone. No audio focus, volume changes, loops or background playback. */
class HomeNavigationSound(private val context:Context) {
    private var pool:SoundPool?=null
    private var soundId=0
    private var ready=false
    private var lastPlay=0L
    private var plays=0

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
        if(!ready || now-lastPlay<90)return
        if(active.play(soundId,.32f,.32f,1,0,1f)!=0) {lastPlay=now;plays++}
    }
    fun stop() {
        val active=pool;pool=null;ready=false;soundId=0;lastPlay=0
        active?.release()
    }
    fun diagnostics()="active=${pool!=null} ready=$ready plays=$plays"
}
