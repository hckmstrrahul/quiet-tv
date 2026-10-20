package dev.rahul.quiettv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import java.util.Calendar
import kotlin.math.*

/** Dark square clock. Only this small view redraws at 20 fps while Home is resumed. */
class HomeTimeWidget @JvmOverloads constructor(context:Context,private val accent:Int=0xFFAADBCB.toInt()):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val regular=resources.getFont(R.font.google_sans)
    private val numerals=if(android.os.Build.VERSION.SDK_INT>=28)Typeface.create(regular,650,false) else Typeface.create(regular,Typeface.BOLD)
    private val fill=LinearGradient(0f,0f,110f,110f,0xFF252D38.toInt(),0xFF141A23.toInt(),Shader.TileMode.CLAMP)
    private val majorTicks=FloatArray(12*4)
    private val minorTicks=FloatArray(48*4)
    init {
        var major=0;var minor=0
        for(i in 0 until 60) {
            val angle=i*PI/30;val sx=sin(angle).toFloat();val sy=-cos(angle).toFloat()
            val radius=47f/(sx.pow(4)+sy.pow(4)).pow(.25f);val length=if(i%5==0)8f else 4f
            val lines=if(i%5==0)majorTicks else minorTicks
            var at=if(i%5==0)major else minor
            lines[at++]=55+sx*(radius-length);lines[at++]=55+sy*(radius-length)
            lines[at++]=55+sx*radius;lines[at++]=55+sy*radius
            if(i%5==0)major=at else minor=at
        }
    }
    private val handler=Handler(Looper.getMainLooper())
    private val now=Calendar.getInstance()
    private var running=false
    private var frames=0
    private val frame=object:Runnable {override fun run() {
        if(!running)return
        invalidate()
        handler.postDelayed(this,if(ValueAnimator.areAnimatorsEnabled())50L else 1000L-System.currentTimeMillis()%1000L)
    }}
    init {isFocusable=false;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES;refresh()}
    fun start(){stop();running=true;frame.run()}
    fun stop(){running=false;handler.removeCallbacksAndMessages(null)}
    fun refresh(){contentDescription=DateFormat.getTimeFormat(context).format(java.util.Date());invalidate()}
    fun diagnostics()="running=$running frames=$frames"
    override fun onDetachedFromWindow(){stop();super.onDetachedFromWindow()}
    override fun onDraw(c:Canvas) {
        super.onDraw(c);frames++
        val scale=min(width/110f,height/110f);c.save();c.scale(scale,scale)
        paint.style=Paint.Style.FILL;paint.shader=fill;c.drawRoundRect(0f,0f,110f,110f,12f,12f,paint);paint.shader=null
        paint.strokeCap=Paint.Cap.ROUND
        paint.color=0xFF6C7682.toInt();paint.strokeWidth=.7f;c.drawLines(minorTicks,paint)
        paint.color=0xFFC6CCD3.toInt();paint.strokeWidth=1f;c.drawLines(majorTicks,paint)
        paint.typeface=numerals;paint.textSize=15f;paint.textAlign=Paint.Align.CENTER;paint.color=0xFFF1F3F5.toInt()
        c.drawText("12",55f,30f,paint);c.drawText("3",83f,60f,paint);c.drawText("6",55f,91f,paint);c.drawText("9",27f,60f,paint)
        now.timeInMillis=System.currentTimeMillis()
        val seconds=now.get(Calendar.SECOND)+if(ValueAnimator.areAnimatorsEnabled())now.get(Calendar.MILLISECOND)/1000f else 0f
        val minute=now.get(Calendar.MINUTE)+seconds/60f;val hour=now.get(Calendar.HOUR)+minute/60f
        fun hand(degrees:Float,length:Float,tail:Float,width:Float,color:Int) {
            paint.color=color;paint.strokeWidth=width
            c.save();c.rotate(degrees,55f,55f);c.drawLine(55f,55f+tail,55f,55f-length,paint);c.restore()
        }
        hand(hour*30f,24f,2f,4.5f,0xFFF1F3F5.toInt());hand(minute*6f,39f,2f,3.5f,0xFFF1F3F5.toInt())
        hand(seconds*6f,44f,8f,1f,accent)
        paint.color=accent;c.drawCircle(55f,55f,2.8f,paint);paint.color=0xFF18202A.toInt();c.drawCircle(55f,55f,1.4f,paint)
        c.restore()
    }
}
