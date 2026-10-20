package dev.rahul.quiettv

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL

/** Two small HTTPS requests per half-hour; no service, GPS, alarms, or playback-time work. */
class WeatherRepository(context:Context,private val changed:()->Unit) {
    private val cache=context.getSharedPreferences("weather_bengaluru",Context.MODE_PRIVATE)
    private val handler=Handler(Looper.getMainLooper())
    private val worker=ForegroundTasks {task->handler.post {task()}}
    private val lock=Any()
    private var connection:HttpURLConnection?=null
    private var fetching=false
    @Volatile private var revision=0
    var conditions=WeatherData.weather(cache.getString("weather",null));private set
    var air=WeatherData.air(cache.getString("air",null));private set
    var failed=cache.getBoolean("failed",false);private set
    private val query="latitude=12.9716&longitude=77.5946&timezone=Asia%2FKolkata&forecast_days=1&timeformat=unixtime"
    fun start() {worker.start();refreshIfDue()}
    fun stop() {
        worker.stop();revision++;fetching=false
        synchronized(lock) {connection?.disconnect();connection=null}
        handler.removeCallbacksAndMessages(null)
    }
    fun refreshIfDue() {
        if(!worker.isActive || fetching)return
        changed()
        val now=System.currentTimeMillis()
        if(!WeatherData.due(cache.getLong("next",0),now))return
        // Also throttle rapid activity recreations, cancellations and failed connections.
        cache.edit().putLong("next",now+WeatherData.RETRY).apply()
        fetching=true
        val ticket=revision
        worker.submit({
            fun get(url:String):String? {
                if(Thread.currentThread().isInterrupted)return null
                val request=URL(url).openConnection() as HttpURLConnection
                synchronized(lock) {
                    if(ticket!=revision || !worker.isActive)return null
                    connection=request
                }
                return try {
                    request.connectTimeout=6000;request.readTimeout=6000
                    request.setRequestProperty("Accept","application/json")
                    if(request.responseCode==200)request.inputStream.bufferedReader().use {it.readText()} else null
                }catch(_:Exception){null}
                finally {request.disconnect();synchronized(lock){if(connection===request)connection=null}}
            }
            val weather=get("https://api.open-meteo.com/v1/forecast?$query&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min")
            val air=get("https://air-quality-api.open-meteo.com/v1/air-quality?$query&current=us_aqi")
            weather to air
        }) { (rawWeather,rawAir)->
            fetching=false
            val time=System.currentTimeMillis()
            val newWeather=WeatherData.weather(rawWeather)?.takeIf {WeatherData.usable(it.at,time)}
            val newAir=WeatherData.air(rawAir)?.takeIf {WeatherData.usable(it.at,time)}
            failed=newWeather==null || newAir==null
            cache.edit().apply {
                if(newWeather!=null){conditions=newWeather;putString("weather",rawWeather)}
                if(newAir!=null){air=newAir;putString("air",rawAir)}
                putBoolean("failed",failed);putLong("next",time+if(failed)WeatherData.RETRY else WeatherData.REFRESH)
            }.apply()
            changed()
        }
    }
    fun diagnostics()="active=${worker.isActive} fetching=$fetching failed=$failed weatherAt=${conditions?.at} airAt=${air?.at}"
}
