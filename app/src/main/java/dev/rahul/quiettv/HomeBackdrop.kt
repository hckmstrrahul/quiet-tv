package dev.rahul.quiettv

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File

/** Two bounded 1080p images; no work or timers survive leaving Home. */
class HomeBackdrop(context:Context,private val prefs:SharedPreferences):FrameLayout(context) {
    private val handler=Handler(Looper.getMainLooper())
    private val worker=ForegroundTasks {handler.post {it()}}
    private val pictures=Array(2) {ImageView(context).apply {scaleType=ImageView.ScaleType.CENTER_CROP}}
    private var front=0
    private var loaded=false
    private var revision=0
    private val deck=WallpaperShuffle(WallpaperCatalog.pictures.map {it.id})
    var currentId=""
        private set
    var changes=0
        private set
    var scheduled=false
        private set
    private val rotate=Runnable {scheduled=false;show(deck.next(currentId),true)}
    init {
        setBackgroundColor(Color.rgb(10,14,21))
        pictures.forEach {addView(it,LayoutParams(-1,-1))}
        addView(View(context).apply {background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0x60081018,0x35081018,0x70081018))},LayoutParams(-1,-1))
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
    fun start() {
        stop();worker.start()
        val chosen=prefs.getString("wallpaperId",WallpaperCatalog.defaultId).takeIf {WallpaperCatalog.valid(it)} ?: WallpaperCatalog.defaultId
        val current=prefs.getString("wallpaperCurrent",chosen).takeIf {WallpaperCatalog.valid(it)} ?: chosen
        show(if(prefs.getBoolean("photo",false))"custom" else if(prefs.getBoolean("wallpaperShuffle",false))current else chosen,false)
    }
    private fun show(id:String,fade:Boolean) {
        if(!worker.isActive)return
        val ticket=revision
        worker.submit({
            try {
                if(id=="custom") BitmapFactory.decodeFile(File(context.filesDir,"wallpaper.jpg").path)
                else context.assets.open("wallpapers/$id.jpg").use {BitmapFactory.decodeStream(it)}
            } catch (_:Exception) {null}
        }) {bitmap->
            if(ticket!=revision)return@submit
            if(bitmap==null) {if(id!=WallpaperCatalog.defaultId)show(WallpaperCatalog.defaultId,false);return@submit}
            val previous=pictures[front];val incoming=pictures[1-front]
            previous.animate().cancel();incoming.animate().cancel()
            incoming.setImageBitmap(bitmap);incoming.alpha=1f
            // Keep the old layer on top while it fades, avoiding a black midpoint.
            if(loaded && fade && ValueAnimator.areAnimatorsEnabled()) {
                previous.bringToFront()
                scrimToFront()
                previous.animate().alpha(0f).setDuration(250).setInterpolator(PathInterpolator(.23f,1f,.32f,1f)).withEndAction {previous.setImageDrawable(null)}.start()
            } else {previous.alpha=0f;previous.setImageDrawable(null);scrimToFront()}
            front=1-front;loaded=true;currentId=id;changes++
            if(id!="custom")prefs.edit().putString("wallpaperCurrent",id).apply()
            handler.removeCallbacks(rotate)
            scheduled=prefs.getBoolean("wallpaperShuffle",false) && !prefs.getBoolean("photo",false)
            if(scheduled)handler.postDelayed(rotate,10_000)
        }
    }
    private fun scrimToFront() {for(i in 0 until childCount)if(getChildAt(i) !is ImageView){getChildAt(i).bringToFront();break}}
    // A translucent system panel pauses Home without hiding it. Freeze the current
    // frame there; release images only once the activity is fully stopped.
    fun pause() {
        revision++;handler.removeCallbacks(rotate);scheduled=false;worker.stop()
        pictures.forEachIndexed { i, picture ->
            picture.animate().cancel();picture.animate().withEndAction(null)
            if(i==front && loaded)picture.alpha=1f
            else {picture.alpha=0f;picture.setImageDrawable(null)}
        }
        scrimToFront()
    }
    fun stop() {
        pause()
        pictures.forEach {it.setImageDrawable(null);it.alpha=0f}
        loaded=false
    }
    fun diagnostics()="active=${worker.isActive} scheduled=$scheduled current=$currentId changes=$changes retained=$loaded"
}
