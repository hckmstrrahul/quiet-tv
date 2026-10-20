package dev.rahul.quiettv

import org.junit.Assert.*
import org.junit.Test

class WeatherDataTest {
    @Test fun nullAndMalformedReadingsAreNeverZeroWeather() {
        assertNull(WeatherData.weather("{}"));assertNull(WeatherData.weather("not json"))
        assertNull(WeatherData.weather("""{"current":{"temperature_2m":null,"weather_code":3,"time":1700000000}}"""))
        assertNull(WeatherData.air("""{"current":{"us_aqi":null,"time":1700000000}}"""))
        assertNull(WeatherData.air("""{"current":{"us_aqi":-4,"time":1700000000}}"""))
    }
    @Test fun parsesApiUnitsAndKeepsMissingDailyValuesUnknown() {
        val w=WeatherData.weather("""{"current":{"temperature_2m":24.7,"weather_code":3,"is_day":0,"time":1700000000}}""")!!
        assertEquals(25,w.temperature);assertEquals(1700000000000L,w.at);assertFalse(w.day);assertNull(w.high)
        assertEquals(0,WeatherData.air("""{"current":{"us_aqi":0,"time":1700000000}}""")!!.value)
    }
    @Test fun cacheExpiryFutureClockAndRetryAreBounded() {
        val at=1700000000000L
        assertTrue(WeatherData.usable(at,at+WeatherData.EXPIRE))
        assertFalse(WeatherData.usable(at,at+WeatherData.EXPIRE+1))
        assertFalse(WeatherData.usable(at,at-16*60_000L))
        assertTrue(WeatherData.stale(at,at+WeatherData.STALE+1))
        assertFalse(WeatherData.due(at+WeatherData.RETRY,at))
        assertTrue(WeatherData.due(at+2*WeatherData.REFRESH,at))
    }
    @Test fun midnightHighLowUsesBengaluruDay() {
        val midnight=java.time.Instant.parse("2026-10-20T18:30:00Z").toEpochMilli()
        assertFalse(WeatherData.sameDay(midnight-1,midnight));assertTrue(WeatherData.sameDay(midnight,midnight+3600000))
    }
    @Test fun aqiBoundariesAndOverflowMatchIndicator() {
        assertEquals(listOf(0,1,2,3,4,5),listOf(50,100,150,200,300,500).map {WeatherData.airBand(it)})
        assertEquals(1,WeatherData.airBand(51));assertEquals(5,WeatherData.airBand(800))
        assertEquals(1f,WeatherData.airPosition(800));assertEquals(0f,WeatherData.airPosition(0))
        assertEquals(1f/6f,WeatherData.airPosition(50),.001f)
    }
}
