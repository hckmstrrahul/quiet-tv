package dev.rahul.quiettv.screensaver

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import kotlin.math.min
import kotlin.math.roundToInt

/** A static, lightweight TV surround. The child remains the actual live native console. */
@SuppressLint("ViewConstructor") // Programmatic wrapper; its existing live renderer is supplied by the activity.
class TvPreviewFrame(context: Context, private val screen: ConsoleView) : ViewGroup(context) {
    private fun dp(value: Float) = value * resources.displayMetrics.density
    private val bezel = dp(3f)
    private val bounds = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = RadialGradient(0f, 0f, 1f,
        intArrayOf(0x426FBAAE, 0x143A727C, Color.TRANSPARENT), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
    private val shadow = RadialGradient(0f, 0f, 1f,
        intArrayOf(0xB0000000.toInt(), 0x50000000, Color.TRANSPARENT), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    private val frameGradient = LinearGradient(0f, 0f, 0f, 1f,
        intArrayOf(0xFF6A7480.toInt(), 0xFF252D36.toInt(), 0xFF10151B.toInt()),
        floatArrayOf(0f, .08f, 1f), Shader.TileMode.CLAMP)
    private val frameMatrix = Matrix()

    init {
        setWillNotDraw(false)
        isFocusable = false
        screen.background = GradientDrawable().apply { setColor(Color.BLACK); cornerRadius = dp(1.5f) }
        screen.clipToOutline = true
        addView(screen)
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val w = resolveSize(dp(430f).roundToInt(), widthSpec)
        val h = resolveSize(dp(300f).roundToInt(), heightSpec)
        setMeasuredDimension(w, h)
        val availableWidth = (w - dp(28f) - bezel * 2).coerceAtLeast(1f)
        val availableHeight = (h - dp(66f) - bezel * 2).coerceAtLeast(1f)
        val screenWidth = min(availableWidth, availableHeight * 16f / 9f).roundToInt().coerceAtLeast(1)
        val screenHeight = (screenWidth * 9f / 16f).roundToInt().coerceAtLeast(1)
        screen.measure(MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val frameWidth = screen.measuredWidth + bezel * 2
        val frameHeight = screen.measuredHeight + bezel * 2
        val left = (width - frameWidth) / 2f
        val top = (height - frameHeight - dp(36f)) / 2f
        bounds.set(left, top, left + frameWidth, top + frameHeight)
        val sx = (left + bezel).roundToInt(); val sy = (top + bezel).roundToInt()
        screen.layout(sx, sy, sx + screen.measuredWidth, sy + screen.measuredHeight)
        frameMatrix.setScale(1f, bounds.height())
        frameMatrix.postTranslate(left, top)
        frameGradient.setLocalMatrix(frameMatrix)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bounds.isEmpty) return
        paint.style = Paint.Style.FILL
        fun ellipse(shader: Shader, y: Float, width: Float, height: Float) {
            canvas.save()
            canvas.translate(bounds.centerX(), y)
            canvas.scale(width, height)
            paint.shader = shader
            canvas.drawCircle(0f, 0f, 1f, paint)
            canvas.restore()
        }
        // Gradients give a soft light spill and contact shadow without blur layers or bitmaps.
        ellipse(glow, bounds.bottom + dp(24f), bounds.width() * .57f, dp(32f))
        ellipse(shadow, bounds.bottom + dp(24f), bounds.width() * .45f, dp(10f))
        paint.shader = null
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dp(3f)
        paint.color = 0xFF303D47.toInt()
        val footLeft = bounds.left + bounds.width() * .17f
        val footRight = bounds.right - bounds.width() * .17f
        canvas.drawLine(footLeft, bounds.bottom - dp(2f), footLeft - dp(15f), bounds.bottom + dp(21f), paint)
        canvas.drawLine(footRight, bounds.bottom - dp(2f), footRight + dp(15f), bounds.bottom + dp(21f), paint)
        paint.shader = frameGradient
        canvas.drawRoundRect(bounds, dp(4f), dp(4f), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(.5f)
        paint.color = 0xFF56636E.toInt()
        canvas.drawRoundRect(bounds, dp(4f), dp(4f), paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF99BEB9.toInt()
        canvas.drawCircle(bounds.centerX(), bounds.bottom - bezel / 2f, dp(.55f), paint)
    }
}
