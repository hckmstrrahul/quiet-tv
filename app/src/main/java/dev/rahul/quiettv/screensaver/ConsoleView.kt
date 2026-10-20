package dev.rahul.quiettv.screensaver

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/** Native port of the 30 MK-IV instruments. All values are procedural, not telemetry. */
class ConsoleView @JvmOverloads constructor(context: Context, initial: ConsoleConfig = ConsoleConfig()) : View(context) {
    private data class Panel(val tile: ConsoleTile, var type: Int, var palette: Int, var phase: Float, var label: String, val noise: FloatArray) {
        var previousColors: Colors? = null
        var transitionAt = 0L
    }
    private data class Colors(val bg: Int, val fg: Int, val accent: Int, val grid: Int)
    private val colors = ConsoleCatalog.palettes.map { Colors(Color.parseColor(it.bg), Color.parseColor(it.fg), Color.parseColor(it.accent), Color.parseColor(it.grid)) }
    private var config = initial.sanitized()
    private var random = Random(config.seed)
    private var panels = emptyList<Panel>()
    private lateinit var colorCycle: ConsoleColorCycle
    private var paletteChanges = 0L
    private var rebuildCount = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = resources.getFont(dev.rahul.quiettv.R.font.google_sans) }
    private val path = Path()
    private val oval = RectF()
    private var requested = false
    var animating = false
        private set
    var frameCount = 0L
        private set
    private var started = 0L
    private var nextReplace = 0L
    private var nextLayout = 0L
    private var layoutNumber = 0L
    private var now = 0L
    private lateinit var canvas: Canvas
    private val frame = object : Runnable {
        override fun run() {
            if (!animating) return
            now = SystemClock.uptimeMillis()
            if (config.automatic && now >= nextReplace) {
                if (panels.isNotEmpty()) {
                    val panel = panels[colorCycle.nextPanel()]
                    if (config.mixedPalettes) {
                        panel.previousColors = displayedColors(panel)
                        panel.palette = colorCycle.nextPalette(panel.palette)
                        panel.transitionAt = now
                        paletteChanges++
                    }
                    if (config.replaceInstruments) {
                        panel.type = random.nextInt(30)
                        panel.phase = random.nextFloat() * 100
                        panel.label = newLabel()
                    }
                }
                nextReplace = now + (config.replacementMillis * (.7 + random.nextDouble() * .6)).toLong()
            }
            if (config.automatic && config.layoutSeconds > 0 && now >= nextLayout) {
                layoutNumber++
                rebuild()
                nextLayout = now + config.layoutSeconds * 1000L
            }
            invalidate()
            postDelayed(this, 1000L / config.fps)
        }
    }
    init { isFocusable = false; contentDescription = "MK-IV animated procedural data console"; rebuild() }
    private fun newLabel(): String {
        val labels = ConsoleCatalog.topics[config.topic].labels
        return labels[random.nextInt(labels.size)] + " " + random.nextInt(1, 100).toString().padStart(2, '0')
    }
    private fun rebuild() {
        rebuildCount++
        val types = (0 until 30).shuffled(random)
        val palettes = colors.indices.shuffled(random)
        panels = ConsoleLayout.pack(config.columns, config.rows, config.seed + layoutNumber).mapIndexed { i, tile ->
            Panel(tile, types[i % types.size], if (config.mixedPalettes) palettes[i % palettes.size] else config.theme, random.nextFloat() * 100, newLabel(), FloatArray(96) { random.nextFloat() })
        }
        colorCycle = ConsoleColorCycle(panels.size, colors.size, random)
    }
    fun configure(value: ConsoleConfig) {
        val old = config
        config = value.sanitized()
        if (config.seed != old.seed || config.density != old.density) {
            random = Random(config.seed); layoutNumber = 0; rebuild()
        } else {
            if (config.theme != old.theme || config.mixedPalettes != old.mixedPalettes) {
                val palettes = colors.indices.shuffled(random)
                panels.forEachIndexed { i, panel ->
                    panel.previousColors = displayedColors(panel)
                    panel.palette = if (config.mixedPalettes) palettes[i % palettes.size] else config.theme
                    panel.transitionAt = now
                }
            }
            if (config.topic != old.topic) panels.forEach { it.label = newLabel() }
        }
        nextReplace = now + config.replacementMillis; nextLayout = now + config.layoutSeconds * 1000L
        invalidate()
    }
    private fun displayedColors(panel: Panel): Colors {
        val target = colors[panel.palette]
        val from = panel.previousColors ?: return target
        val progress = ((now - panel.transitionAt) / 900f).coerceIn(0f, 1f)
        if (progress >= 1f) { panel.previousColors = null; return target }
        val t = progress * progress * (3 - 2 * progress)
        fun blend(a: Int, b: Int): Int = Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).roundToInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).roundToInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).roundToInt())
        return Colors(blend(from.bg, target.bg), blend(from.fg, target.fg),
            blend(from.accent, target.accent), blend(from.grid, target.grid))
    }
    fun diagnostics(): String = "paletteChanges=$paletteChanges rebuilds=$rebuildCount " +
        "structure=${panels.map { listOf(it.tile, it.type, it.phase, it.label, it.noise.contentHashCode()) }.hashCode()} " +
        "palettes=${panels.map { it.palette }.hashCode()} distinctPalettes=${panels.map { it.palette }.distinct().size} " +
        "animationMs=${(now - started).coerceAtLeast(0)} replaceInstruments=${config.replaceInstruments} layoutSeconds=${config.layoutSeconds}"
    fun start() { requested = true; updateRunning() }
    fun stop() { requested = false; animating = false; removeCallbacks(frame) }
    private fun updateRunning() {
        val run = requested && isAttachedToWindow && windowVisibility == VISIBLE
        if (run && !animating) {
            val resumedAt = SystemClock.uptimeMillis()
            started = if (started == 0L) resumedAt else started + resumedAt - now
            now = resumedAt; animating = true
            nextReplace = now + config.replacementMillis; nextLayout = now + config.layoutSeconds * 1000L
            frame.run()
        } else if (!run) { animating = false; removeCallbacks(frame) }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateRunning() }
    override fun onDetachedFromWindow() { animating = false; removeCallbacks(frame); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); updateRunning() }
    private fun pen(color: Int, width: Float = 1f, alpha: Int = 255, fill: Boolean = false) {
        paint.color = color; paint.alpha = alpha; paint.strokeWidth = width
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
    }
    private fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 1f, alpha: Int = 255) {
        pen(color, width, alpha); canvas.drawLine(x1, y1, x2, y2, paint)
    }
    private fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, alpha: Int = 255) {
        pen(color, alpha = alpha, fill = true); canvas.drawRect(x, y, x + w, y + h, paint)
    }
    private fun circle(x: Float, y: Float, r: Float, color: Int, fill: Boolean = false, alpha: Int = 255, width: Float = 1f) {
        pen(color, width, alpha, fill); canvas.drawCircle(x, y, max(0f, r), paint)
    }
    private fun text(value: String, x: Float, y: Float, size: Float, color: Int, center: Boolean = false, alpha: Int = 255) {
        pen(color, alpha = alpha, fill = true); paint.textSize = size
        paint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        canvas.drawText(value, x, y, paint)
    }
    private fun grid(w: Float, h: Float, col: Int) {
        for (i in 1..3) { line(w * i / 4, 0f, w * i / 4, h, col, alpha = 30); line(0f, h * i / 4, w, h * i / 4, col, alpha = 30) }
    }
    private fun pathDraw(color: Int, fill: Boolean = false, alpha: Int = 255, width: Float = 1.3f) {
        pen(color, width, alpha, fill); canvas.drawPath(path, paint)
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c); canvas = c; frameCount++
        c.drawColor(colors[config.theme].bg)
        val vw = 1200f; val vh = vw * height / width.coerceAtLeast(1)
        c.save(); c.scale(width / vw, height / vh)
        val cw = vw / config.columns; val ch = vh / config.rows
        val elapsed = ((now - started).coerceAtLeast(0) / 1000f) * floatArrayOf(.45f, 1f, 2.3f)[config.speed]
        // Slow subpixel drift moves static labels without changing system display settings.
        val driftX = sin(elapsed * .04f) * 1.5f; val driftY = cos(elapsed * .031f) * 1.5f
        for (p in panels) {
            val t = p.tile; val x = t.x * cw + 2; val y = t.y * ch + 2
            val w = t.width * cw - 4; val h = t.height * ch - 4; val color = displayedColors(p)
            c.save(); c.clipRect(x, y, x + w, y + h); c.translate(x, y)
            rect(0f, 0f, w, h, color.bg)
            pen(color.grid); c.drawRect(.5f, .5f, w - .5f, h - .5f, paint)
            c.save(); c.translate(4 + driftX, 17 + driftY)
            c.clipRect(0f, 0f, w - 8, h - 20)
            drawInstrument(p, w - 8, h - 20, elapsed + p.phase, color)
            c.restore()
            text(p.label, 6 + driftX, 11 + driftY, 7.5f, color.fg, alpha = 210)
            c.restore()
        }
        if (config.scanlines) {
            var y = 0f
            while (y < vh) { line(0f, y, vw, y, Color.BLACK, alpha = 12); y += 4 }
        }
        if (config.brightness < 100) rect(0f, 0f, vw, vh, Color.BLACK, ((100 - config.brightness) * 2.55f).toInt())
        c.restore()
    }
    private fun drawInstrument(p: Panel, w: Float, h: Float, t: Float, c: Colors) {
        val cx = w / 2; val cy = h / 2; val r = min(w, h) * .35f
        val fg = c.fg; val ac = c.accent
        when (p.type) {
            0 -> { grid(w, h, fg); path.reset(); for(i in 0..80) { val x = w*i/80; val y = cy + sin(x*.05f+t*1.6f)*h*.3f*sin(t*.16f); if(i==0)path.moveTo(x,y) else path.lineTo(x,y) }; pathDraw(fg, width=1.5f) }
            1, 15 -> { val n=if(p.type==1)20 else 16; val bw=w/n; for(i in 0 until n) { val bh=abs(sin(i*.5f+t*2))*h*.85f; rect(i*bw+1,h-bh,bw-2,bh,fg); if(p.type==15)rect(i*bw+1,h-bh-4,bw-2,2f,ac) } }
            2 -> { val value=(sin(t)+1)/2; oval.set(cx-r,cy-r*.5f,cx+r,cy+r*1.5f); pen(fg,3f,50);canvas.drawArc(oval,180f,180f,false,paint);pen(ac,3f);canvas.drawArc(oval,180f,value*180,false,paint);text("${(value*100).toInt()}%",cx,cy+r*.3f,13f,fg,true) }
            3 -> { for(ring in 1..3) { path.reset();for(i in 0..5) { val a=i*2*PI.toFloat()/5-PI.toFloat()/2; val rad=r*ring/3; if(i==0)path.moveTo(cx+cos(a)*rad,cy+sin(a)*rad) else path.lineTo(cx+cos(a)*rad,cy+sin(a)*rad) };pathDraw(fg,alpha=60) };path.reset();for(i in 0..5) {val a=i*2*PI.toFloat()/5-PI.toFloat()/2;val rad=r*(.4f+abs(sin(t+i%5))*.6f);if(i==0)path.moveTo(cx+cos(a)*rad,cy+sin(a)*rad) else path.lineTo(cx+cos(a)*rad,cy+sin(a)*rad)};pathDraw(ac,true,65);pathDraw(ac) }
            4 -> { grid(w,h,fg);path.reset();for(i in 0..70) {val v=.5f+sin(t+i*.1f)*.2f+p.noise[(i+(t*8).toInt()).mod(96)]*.2f;val x=i*w/70;val y=h*(1-v);if(i==0)path.moveTo(x,y) else path.lineTo(x,y)};pathDraw(fg) }
            5 -> {for(x in 0..7)for(y in 0..4)rect(x*w/8+2,y*h/5+2,w/8-4,h/5-4,if(sin(x*.5f+y*.5f+t)>.5f)ac else fg,if(sin(x*.5f+y*.5f+t)>.5f)255 else 35) }
            6 -> { val s=r*.8f;val points=Array(8){i->val x=if(i and 1==0)-1f else 1f;val y=if(i and 2==0)-1f else 1f;val z=if(i and 4==0)-1f else 1f;val xx=x*cos(t)-z*sin(t);val zz=x*sin(t)+z*cos(t);floatArrayOf(cx+xx*s,cy+(y*cos(t*.5f)-zz*sin(t*.5f))*s)};for(i in 0..7)for(bit in intArrayOf(1,2,4))if(i and bit==0){val j=i xor bit;line(points[i][0],points[i][1],points[j][0],points[j][1],fg)} }
            7 -> { val cols=(w/12).toInt().coerceAtMost(28);val rows=(h/12).toInt().coerceAtMost(20);val beat=(t*3).toInt();for(x in 0 until cols)for(y in 0 until rows){val v=p.noise[(x*7+y*13+beat).mod(96)];text(if(v>.5f)"1" else "0",x*12f,y*12f+10,9f,if(v>.93f)ac else fg)} }
            8 -> {for(i in 1..5)circle(cx,cy,r*i/5,if(i%2==0)ac else fg,width=if(t.toInt().mod(5)==i-1)2.5f else 1f);line(cx-r*.6f,cy,cx+r*.6f,cy,fg);line(cx,cy-r*.6f,cx,cy+r*.6f,fg) }
            9 -> {text("0x"+(t*1000).toLong().toString(16).uppercase().takeLast(4).padStart(4,'0'),cx,cy+4,min(27f,w/4.5f),fg,true);text("MEM ADDR",cx,cy+20,8f,ac,true) }
            10 -> {grid(w,h,fg);for(i in 0 until 20){val x=(p.noise[i]+sin(t*.14f+i)*.08f).mod(1f)*w;val y=(p.noise[i+20]+cos(t*.13f+i)*.08f).mod(1f)*h;circle(x,y,2f,ac,true)} }
            11 -> {path.reset();path.moveTo(0f,h);for(i in 0..70){val x=i*w/70;path.lineTo(x,cy+sin(x*.02f+t)*sin(x*.05f)*h*.3f)};path.lineTo(w,h);path.close();pathDraw(fg,true,35);pathDraw(fg) }
            12 -> {var start=sin(t*.3f)*12;for((i,part)in floatArrayOf(.2f,.3f,.1f,.4f).withIndex()){oval.set(cx-r,cy-r,cx+r,cy+r);pen(if(i==3)ac else fg,alpha=if(i==1)85 else 255,fill=true);canvas.drawArc(oval,start,part*360,true,paint);start+=part*360} }
            13 -> {var y=8f;while(y<h-6){val off=sin(y*.05f+t)*w*.25f;line(cx+off,y,cx-off,y,fg,alpha=55);circle(cx+off,y,2.5f,fg,true);circle(cx-off,y,2.5f,ac,true);y+=10} }
            14 -> {for((i,l)in listOf("CPU","RAM","GPU","SSD").withIndex()){val y=(i+1)*h/5;val value=(sin(t*.5f+i)+1)/2;text(l,1f,y+2,8f,fg);rect(29f,y-4,w-33,6f,fg,35);rect(29f,y-4,(w-33)*value,6f,ac)} }
            16 -> {circle(cx,cy,r,fg);for(i in 0..5){val rx=abs(r*sin(t*.15f+i*PI.toFloat()/6));oval.set(cx-rx,cy-r,cx+rx,cy+r);pen(fg,alpha=70);canvas.drawOval(oval,paint)};line(cx-r,cy,cx+r,cy,fg,alpha=70) }
            17 -> {val verbs=listOf("PARSING","LOADING","SYNCING","PINGING","COMPILING");val nouns=listOf("DATA","PACKET","KERNEL","BUFFER","THREAD");val n=(h/12).toInt().coerceAtMost(10);val step=(t*.7f).toInt();for(i in 0 until n){val j=(step+i).mod(5);text("> ${verbs[j]} ${nouns[(j+i).mod(5)]} OK",4f,12f+i*12,7.5f,if(i==n-1)ac else fg)} }
            18 -> {var x=10f;while(x<w){var y=10f;while(y<h){val a=sin(x*.01f+t)+cos(y*.01f);line(x-cos(a)*5,y-sin(a)*5,x+cos(a)*5,y+sin(a)*5,fg,alpha=120);y+=20};x+=20} }
            19 -> {for(i in 0..7){val a=i*PI.toFloat()/4+t;circle(cx+cos(a)*r*.65f,cy+sin(a)*r*.65f,3f,if(i==0)ac else fg,true)} }
            20 -> {val cols=(w/6).toInt().coerceAtMost(50);val rows=(h/6).toInt().coerceAtMost(40);val step=(t*3).toInt();for(y in 0 until rows)for(x in 0 until cols)if(p.noise[(x*17+(y-step)*7).mod(96)]>.85f)rect(x*6f,y*6f,4f,4f,fg,85) }
            21 -> {circle(cx,cy,4f,ac,true);for(i in 0..5){val a=i*PI.toFloat()/3+sin(t*.4f)*.5f;val dist=r*(.8f+sin(t*.2f+i)*.2f);val x=cx+cos(a)*dist;val y=cy+sin(a)*dist;line(cx,cy,x,y,fg);circle(x,y,3f,fg,true)} }
            22 -> {var x=5f;var i=0;while(x<w-5){val v=p.noise[(i+(t*.6f).toInt()).mod(96)];val bw=v*4+1;if(v>.35f)rect(x,cy-15,bw,30f,fg);x+=bw+1;i++};rect(5+(t*12).mod(max(1f,w-10)),cy-17,2f,34f,ac) }
            23 -> {circle(cx,cy,r,fg);for(i in 0..7){val a=i*PI.toFloat()/4;line(cx+cos(a)*(r-5),cy+sin(a)*(r-5),cx+cos(a)*r,cy+sin(a)*r,fg)};canvas.save();canvas.rotate(sin(t*.3f)*100,cx,cy);path.reset();path.moveTo(cx,cy-r+5);path.lineTo(cx-5,cy+5);path.lineTo(cx+5,cy+5);path.close();pathDraw(ac,true);canvas.restore();text("HEADING",cx,cy+r+12,7f,fg,true) }
            24 -> {canvas.save();path.reset();path.addCircle(cx,cy,r,Path.Direction.CW);canvas.clipPath(path);rect(cx-r,cy-r,2*r,2*r,fg,35);path.reset();path.moveTo(cx-r,cy+r);for(i in 0..40){val x=cx-r+2*r*i/40;path.lineTo(x,cy-sin(t*.3f)*r*.8f+sin(x*.05f+t*2)*4)};path.lineTo(cx+r,cy+r);path.close();pathDraw(ac,true);canvas.restore();circle(cx,cy,r,fg,width=1.5f) }
            25 -> {for(i in 0..60){val x=i*w/60;val v=sin(x*.1f+t*3)*p.noise[(i+(t*4).toInt()).mod(96)]*h*.4f;line(x,cy-v,x,cy+v,ac)} }
            26 -> {circle(cx,cy,7f,fg,true);circle(cx,cy,r,fg,alpha=55);circle(cx+cos(t*.6f)*r,cy+sin(t*.6f)*r,5f,ac,true) }
            27 -> {oval.set(cx-r,cy-r,cx+r,cy+r);pen(fg,alpha=110,fill=true);for(i in 0..2)canvas.drawArc(oval,t*80+i*120,60f,true,paint);circle(cx,cy,4f,ac,true) }
            28 -> {grid(w,h,fg);val x=(sin(t*.4f)*.4f+.5f)*w;val y=(cos(t*.28f)*.4f+.5f)*h;line(x,0f,x,h,ac);line(0f,y,w,y,ac);circle(x,y,8f,ac) }
            29 -> {for(i in 0..9)if(i%2==0)rect(i*w/10,0f,w/10,h,fg,30);val y=(sin(t)*.5f+.5f)*h;line(0f,y,w,y,ac,2f) }
        }
    }
}
