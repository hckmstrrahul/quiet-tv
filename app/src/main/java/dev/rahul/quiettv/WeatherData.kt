package dev.rahul.quiettv

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** API parsing and freshness rules shared by the cache and native card. */
object WeatherData {
    const val REFRESH = 30 * 60_000L
    const val RETRY = 5 * 60_000L
    const val STALE = 90 * 60_000L
    const val EXPIRE = 6 * 60 * 60_000L
    val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    data class Conditions(val temperature:Int,val code:Int,val day:Boolean,val high:Int?,val low:Int?,val at:Long,val date:Long?)
    data class Air(val value:Int,val at:Long)
    fun usable(at:Long,now:Long)=at>0 && now-at in -15*60_000L..EXPIRE
    fun stale(at:Long,now:Long)=now-at>STALE
    fun due(next:Long,now:Long)=next<=now || next-now>REFRESH
    fun sameDay(a:Long,b:Long)=Instant.ofEpochMilli(a).atZone(zone).toLocalDate()==Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
    private fun number(value:Any?):Double?=(value as? Number)?.toDouble()?.takeIf {it.isFinite()}
    fun weather(raw:String?):Conditions?=try {
        val root=JSONObject(raw ?: "");val current=root.getJSONObject("current")
        val temperature=number(current.opt("temperature_2m"))?.takeIf {it in -100.0..65.0}
        val code=number(current.opt("weather_code"))?.toInt()
        val at=number(current.opt("time"))?.toLong()?.times(1000)
        val daily=root.optJSONObject("daily")
        fun dailyNumber(key:String)=number(daily?.optJSONArray(key)?.opt(0))
        if(temperature==null || code==null || at==null || at<=0)null
        else Conditions(temperature.roundToInt(),code,current.optInt("is_day",1)==1,
            dailyNumber("temperature_2m_max")?.roundToInt(),dailyNumber("temperature_2m_min")?.roundToInt(),at,dailyNumber("time")?.toLong()?.times(1000))
    }catch(_:Exception){null}
    fun air(raw:String?):Air?=try {
        val current=JSONObject(raw ?: "").getJSONObject("current")
        val value=number(current.opt("us_aqi"))?.takeIf {it>=0}
        val at=number(current.opt("time"))?.toLong()?.times(1000)
        if(value==null || at==null || at<=0)null else Air(value.roundToInt(),at)
    }catch(_:Exception){null}
    fun condition(code:Int,day:Boolean)=when(code) {
        0->if(day)"Sunny" else "Clear"
        1->"Mostly clear";2->"Partly cloudy";3->"Cloudy"
        45,48->"Fog";51,53,55,56,57->"Drizzle";61,63,65,66,67->"Rain"
        71,73,75,77,85,86->"Snow";80,81,82->"Showers";95,96,99->"Thunderstorm"
        else->"Conditions --"
    }
    fun airBand(value:Int)=when {value<=50->0;value<=100->1;value<=150->2;value<=200->3;value<=300->4;else->5}
    fun airLabel(value:Int)=listOf("Good","Moderate","Unhealthy for sensitive groups","Unhealthy","Very unhealthy","Hazardous")[airBand(value)]
    /** Equal-width category segments, with continuous position inside each band. */
    fun airPosition(value:Int):Float {
        val edges=intArrayOf(0,50,100,150,200,300,500);val band=airBand(value)
        return (band+(value-edges[band]).toFloat()/(edges[band+1]-edges[band])).coerceIn(0f,6f)/6f
    }
}
