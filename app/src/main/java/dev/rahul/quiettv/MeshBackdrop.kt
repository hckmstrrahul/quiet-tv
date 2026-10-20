package dev.rahul.quiettv

import android.graphics.*
import android.graphics.drawable.Drawable

/** Static overlapping color fields: no bitmap, animation loop, or blur layers. */
class MeshBackdrop(private val base: Int, accent: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun field(color: Int, alpha: Int) = RadialGradient(0f,0f,1f,
        intArrayOf((color and 0x00FFFFFF) or (alpha shl 24), (color and 0x00FFFFFF) or ((alpha/2) shl 24), color and 0x00FFFFFF),
        floatArrayOf(0f,.48f,1f),Shader.TileMode.CLAMP)
    private val nightBase=Color.rgb(Color.red(base)/3,Color.green(base)/3,Color.blue(base)/3)
    private val blue = field(Color.rgb(35,52,96),58)
    private val mint = field(accent,15)
    private val plum = field(Color.rgb(65,31,79),50)
    private val teal = field(Color.rgb(12,65,70),52)
    override fun draw(canvas: Canvas) {
        canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat())
        canvas.scale(bounds.width().toFloat(),bounds.height().toFloat())
        paint.shader=null;paint.color=nightBase;canvas.drawRect(0f,0f,1f,1f,paint)
        blob(canvas,blue,.12f,.08f,.8f,.95f)
        blob(canvas,mint,.9f,.1f,.6f,.8f)
        blob(canvas,plum,.18f,.93f,.7f,.8f)
        blob(canvas,teal,.83f,.82f,.7f,.9f)
        paint.shader=null;canvas.restore()
    }
    private fun blob(c: Canvas, shader: Shader, x: Float,y: Float,w: Float,h: Float) {
        c.save();c.translate(x,y);c.scale(w,h);paint.shader=shader;c.drawCircle(0f,0f,1f,paint);c.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha=alpha;invalidateSelf() }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
    @Deprecated("Drawable opacity is no longer used")
    override fun getOpacity() = PixelFormat.OPAQUE
}
