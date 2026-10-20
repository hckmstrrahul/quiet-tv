package dev.rahul.quiettv

/** No timer runs inside this model; the visible widget samples the playback clock. */
object PlaybackMath {
    fun position(base: Long, updated: Long, now: Long, speed: Float, playing: Boolean, duration: Long): Long {
        val elapsed = if(playing && updated>0) ((now-updated).coerceAtLeast(0)*speed).toLong() else 0L
        val position=(base.coerceAtLeast(0)+elapsed).coerceAtLeast(0)
        return if(duration>0) position.coerceAtMost(duration) else position
    }
    fun time(milliseconds: Long): String {
        val seconds=milliseconds.coerceAtLeast(0)/1000
        return if(seconds>=3600) "%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)
        else "%d:%02d".format(seconds/60,seconds%60)
    }
}
