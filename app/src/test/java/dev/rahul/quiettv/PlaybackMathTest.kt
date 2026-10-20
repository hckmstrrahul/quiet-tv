package dev.rahul.quiettv

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMathTest {
    @Test fun runningClockUsesSpeedAndClampsAtDuration() {
        assertEquals(12_000L,PlaybackMath.position(10_000,1000,2000,2f,true,60_000))
        assertEquals(60_000L,PlaybackMath.position(59_000,1000,3000,1f,true,60_000))
    }
    @Test fun pausedOrMissingTimestampDoesNotAdvance() {
        assertEquals(10_000L,PlaybackMath.position(10_000,1000,6000,1f,false,60_000))
        assertEquals(10_000L,PlaybackMath.position(10_000,0,6000,1f,true,60_000))
    }
    @Test fun unknownDurationAndClockSkewStayValid() {
        assertEquals(61_000L,PlaybackMath.position(60_000,1000,2000,1f,true,0))
        assertEquals(0L,PlaybackMath.position(-1,2000,1000,1f,true,0))
    }
    @Test fun timeSupportsLongMedia() {
        assertEquals("0:00",PlaybackMath.time(-1))
        assertEquals("3:42",PlaybackMath.time(222_000))
        assertEquals("1:01:01",PlaybackMath.time(3_661_000))
    }
}
