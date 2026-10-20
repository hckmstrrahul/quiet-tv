package dev.rahul.quiettv

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.TextView
import android.text.method.LinkMovementMethod
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.min

/** Compact, vector weather card. Canvas avoids nested layouts and bitmap weather assets. */
class WeatherWidget(context:Context):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val regular=resources.getFont(R.font.google_sans)
    private val medium=if(android.os.Build.VERSION.SDK_INT>=28)Typeface.create(regular,600,false) else Typeface.create(regular,Typeface.BOLD)
    private val colors=intArrayOf(0xFF7FCA89.toInt(),0xFFF0D469.toInt(),0xFFF2A15B.toInt(),0xFFE36A70.toInt(),0xFFAE7BD6.toInt(),0xFF97536A.toInt())
    private val baseFill=LinearGradient(0f,0f,90f,110f,0xED343D4D.toInt(),0xF018202C.toInt(),Shader.TileMode.CLAMP)
    private val focusFill=LinearGradient(0f,0f,90f,110f,0xF0485365.toInt(),0xF018202C.toInt(),Shader.TileMode.CLAMP)
    private val repository=WeatherRepository(context) {refresh()}
    private var details:AlertDialog?=null
    init {
        id=generateViewId();isFocusable=true;isClickable=true
        setOnFocusChangeListener {_,_->invalidate()}
        setOnClickListener {showDetails()}
        refresh()
    }
    fun start()=repository.start()
    fun stop() {repository.stop();details?.dismiss();details=null}
    fun refreshIfDue()=repository.refreshIfDue()
    fun diagnostics()=repository.diagnostics()
    private fun refresh() {
        val now=System.currentTimeMillis();val w=repository.conditions?.takeIf {WeatherData.usable(it.at,now)};val a=repository.air?.takeIf {WeatherData.usable(it.at,now)}
        contentDescription="Bengaluru weather. "+(w?.let {"${it.temperature} degrees Celsius, ${WeatherData.condition(it.code,it.day)}. "} ?: "Weather unavailable. ")+
            (a?.let {"US AQI ${it.value}, ${WeatherData.airLabel(it.value)}. "} ?: "Air quality unavailable. ")+"Select for updates and sources."
        invalidate()
    }
    private fun text(c:Canvas,value:String,x:Float,y:Float,size:Float,bold:Boolean=false,color:Int=Color.WHITE,max:Float=90f,right:Boolean=false) {
        paint.shader=null;paint.style=Paint.Style.FILL;paint.color=color;paint.typeface=if(bold)medium else regular;paint.textSize=size;paint.textAlign=if(right)Paint.Align.RIGHT else Paint.Align.LEFT
        val length=paint.measureText(value);if(length>max)paint.textSize=size*max/length
        c.drawText(value,x,y,paint)
    }
    override fun onDraw(c:Canvas) {
        super.onDraw(c);val scale=min(width/110f,height/110f);c.save();c.scale(scale,scale)
        paint.style=Paint.Style.FILL
        paint.shader=if(hasFocus())focusFill else baseFill
        c.drawRoundRect(0f,0f,110f,110f,12f,12f,paint);paint.shader=null
        paint.style=Paint.Style.STROKE;paint.strokeWidth=.6f;paint.color=0x507F8B9D;c.drawRoundRect(.5f,.5f,109.5f,109.5f,12f,12f,paint)
        val now=System.currentTimeMillis();val w=repository.conditions?.takeIf {WeatherData.usable(it.at,now)};val a=repository.air?.takeIf {WeatherData.usable(it.at,now)}
        text(c,"Bengaluru",11f,17f,10.5f,true)
        text(c,w?.let {"${it.temperature}°"} ?: "--",10f,53f,33f)
        val stale=repository.failed || (w?.let {WeatherData.stale(it.at,now)} ?: false) || (a?.let {WeatherData.stale(it.at,now)} ?: false)
        if(stale && (w!=null || a!=null))text(c,"Cached",62f,39f,7f,color=0xFFBFC8D4.toInt(),max=25f)
        if(w!=null)weatherIcon(c,w.code,w.day,11f,63f)
        text(c,w?.let {WeatherData.condition(it.code,it.day)} ?: "Weather unavailable",if(w==null)11f else 27f,73f,9f,true,max=if(w==null)77f else 61f)
        val today=w?.date?.let {WeatherData.sameDay(it,now)}==true
        text(c,"H:${if(today)w?.high?.toString() ?: "--" else "--"}° L:${if(today)w?.low?.toString() ?: "--" else "--"}°",11f,97f,8f,max=49f)
        text(c,"US AQI ${a?.value ?: "--"}",100f,97f,8f,true,color=0xFFDBE1E9.toInt(),max=37f,right=true)
        paint.style=Paint.Style.FILL
        // Low AQI at the bottom, increasing upward; the numeric row stays below the scale.
        for(i in 0..5) {val top=76f-i*10f;paint.color=if(a==null)0xFF4D5663.toInt() else colors[i];c.drawRoundRect(96f,top,100f,top+9f,2f,2f,paint)}
        if(a!=null){val y=83f-WeatherData.airPosition(a.value)*55f;paint.color=0xFF17202C.toInt();c.drawCircle(98f,y,3.3f,paint);paint.color=Color.WHITE;c.drawCircle(98f,y,2.1f,paint)}
        c.restore()
    }
    private fun weatherIcon(c:Canvas,code:Int,day:Boolean,x:Float,y:Float) {
        c.save();c.translate(x,y);paint.style=Paint.Style.FILL;paint.color=Color.WHITE
        when(code) {
            0,1 -> {c.drawCircle(6f,5f,4f,paint);if(!day){paint.color=0xFF2C3543.toInt();c.drawCircle(8f,3f,3.8f,paint)}}
            45,48 -> {paint.strokeWidth=1.6f;paint.strokeCap=Paint.Cap.ROUND;for(i in 0..2)c.drawLine(0f,i*3f,12f,i*3f,paint)}
            else -> {
                c.drawRoundRect(0f,4f,13f,9f,2.5f,2.5f,paint);c.drawCircle(5f,4f,3.5f,paint);c.drawCircle(9f,5f,2.5f,paint)
                if(code>=51){paint.color=0xFF9CCBEE.toInt();paint.strokeWidth=1f;for(i in 0..2)c.drawLine(3f+i*4f,10f,2f+i*4f,12f,paint)}
            }
        };c.restore()
    }
    private fun showDetails() {
        val w=repository.conditions;val a=repository.air
        fun time(at:Long?)=at?.let {DateTimeFormatter.ofPattern("d MMM, h:mm a").withZone(WeatherData.zone).format(Instant.ofEpochMilli(it))} ?: "Unavailable"
        val body="Bengaluru · °C\nWeather data: ${time(w?.at)}\nAir-quality data: ${time(a?.at)}\n\nUS AQI: ${a?.let {"${it.value} · ${WeatherData.airLabel(it.value)}"} ?: "Unavailable"}\nGreen → yellow → orange → red → purple → maroon.\n\nWeather is model-based current conditions. AQI is a regional CAMS estimate, using the US scale, not India's official AQI or a nearby sensor reading.\n\nRefreshes every 30 minutes while Home is visible. Cached readings are marked when stale or a refresh fails; values older than 6 hours are hidden.\n\nWeather: Open-Meteo\nAir quality: CAMS / Open-Meteo"
        val links=SpannableString(body).apply {
            val index=body.indexOf("Weather: Open-Meteo")+9;setSpan(URLSpan("https://open-meteo.com/"),index,index+10,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val start=body.indexOf("CAMS / Open-Meteo");setSpan(URLSpan("https://open-meteo.com/en/docs/air-quality-api"),start,body.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val density=resources.displayMetrics.density
        val label=TextView(context).apply {text=links;textSize=14f;setTextColor(Color.WHITE);movementMethod=LinkMovementMethod.getInstance();setPadding((24*density).toInt(),0,(24*density).toInt(),0)}
        val scroll=android.widget.ScrollView(context).apply {addView(label)}
        details=AlertDialog.Builder(context).setTitle("Weather & air quality").setView(scroll).setPositiveButton("Close",null).show().also {
            DialogUi.style(it,650)
            it.window?.setLayout((650*density).toInt(),(450*density).toInt())
        }
    }
}
