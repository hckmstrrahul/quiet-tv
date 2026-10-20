package dev.rahul.quiettv

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.rahul.quiettv.screensaver.*
import java.io.File

/** Checks actual native layouts and static rendering without saving a theme or changing Home values. */
class AppearanceChecks(private val runner:Instrumentation) {
    private fun views(v:View):List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap {views(v.getChildAt(it))} else emptyList())
    private fun <T> ui(block:()->T):T {var result:Result<T>?=null;runner.runOnMainSync {result=runCatching(block)};return result!!.getOrThrow()}
    private fun await(label:String,check:()->Boolean) {
        val until=SystemClock.uptimeMillis()+7000
        while(SystemClock.uptimeMillis()<until){if(ui(check))return;Thread.sleep(100)}
        error("Timed out: $label")
    }
    private fun dialog(activity:Activity,field:String)=activity.javaClass.getDeclaredField(field).apply {isAccessible=true}.get(activity) as? AlertDialog
    private fun shot(name:String) {
        runner.waitForIdleSync()
        Thread.sleep(350) // Let native dialog and scrolling transitions finish before capture.
        runner.uiAutomation.takeScreenshot().also {bitmap->File(runner.targetContext.filesDir,name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    }
    fun run() {
        var settings:ConsoleSettingsActivity?=null
        val result=Bundle()
        try {
            val context=runner.targetContext
            val saved=ConsolePreferences.read(context)
            val homePrefs=context.getSharedPreferences("home",0).all.toMap()
            val home=runner.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            await("Home") {home.hasWindowFocus() && views(home.window.decorView).any {it is WeatherWidget}}
            shot("appearance-home.png")
            ui {views(home.window.decorView).first {it.contentDescription=="Appearance"}.performClick()}
            await("Home settings") {dialog(home,"homeSettings")?.isShowing==true}
            ui {
                val window=dialog(home,"homeSettings")!!.window!!.decorView
                views(window).filterIsInstance<TextView>().first {it.isClickable && it.text.toString()=="Layout"}.performClick()
                val columns=context.getSharedPreferences("home",0).getInt("columns",6)
                check(views(window).filterIsInstance<TextView>().any {it.text.toString()=="Tile size  ·  ${listOf("Large","Medium","Small")[columns-4]} · $columns columns"})
            }
            shot("appearance-layout.png")
            ui {
                val window=dialog(home,"homeSettings")!!.window!!.decorView
                views(window).filterIsInstance<TextView>().first {it.isClickable && it.text.toString()=="Colors"}.performClick()
                check(views(window).filterIsInstance<TextView>().any {it.text.startsWith("Accent color  ·  ")})
                dialog(home,"homeSettings")!!.dismiss()
            }
            settings=runner.startActivitySync(Intent(context,ConsoleSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ConsoleSettingsActivity
            val activity=settings
            await("MK-IV settings") {activity.hasWindowFocus()}
            fun openThemes()=ui {views(activity.window.decorView).filterIsInstance<TextView>().first {it.text.startsWith("Theme  ·  ")}.performClick()}
            openThemes()
            await("Theme gallery") {dialog(activity,"themeDialog")?.window?.decorView?.hasWindowFocus()==true}
            lateinit var samples:List<ConsoleView>
            ui {
                val d=dialog(activity,"themeDialog")!!
                samples=views(d.window!!.decorView).filterIsInstance<ConsoleView>()
                check(samples.size==ConsoleCatalog.palettes.size)
                check(samples.none {it.animating})
                check(views(activity.window.decorView).filterIsInstance<ConsoleView>().none {it.animating})
                val hashes=samples.mapIndexed {index,sample->
                    val bitmap=Bitmap.createBitmap(sample.width,sample.height,Bitmap.Config.ARGB_8888)
                    sample.draw(Canvas(bitmap))
                    check(bitmap.getPixel(0,0)==android.graphics.Color.parseColor(ConsoleCatalog.palettes[index].bg))
                    val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                    pixels.contentHashCode().also {bitmap.recycle()}
                }
                check(hashes.distinct().size==samples.size) {"Theme previews are not distinct"}
            }
            runner.waitForIdleSync()
            val frames=ui {samples.map {it.frameCount}}
            Thread.sleep(350)
            check(ui {frames==samples.map {it.frameCount}}) {"Static gallery kept rendering frames"}
            shot("appearance-themes-selected.png")
            // Move to the first row, then through the grid to the last row and Close.
            repeat(6) {
                ui {check(dialog(activity,"themeDialog")!!.window!!.decorView.hasWindowFocus())}
                runner.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP)
            }
            ui {check(dialog(activity,"themeDialog")!!.currentFocus?.contentDescription.toString().removeSuffix(", selected") in ConsoleCatalog.palettes.take(3).map {it.name}) {"First gallery row is not reachable"}}
            shot("appearance-themes-top.png")
            repeat(7) {
                ui {check(dialog(activity,"themeDialog")!!.window!!.decorView.hasWindowFocus())}
                runner.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
            }
            ui {check(dialog(activity,"themeDialog")!!.currentFocus?.contentDescription=="Close")}
            // A theme choice updates only the draft. Reopen to verify its selection badge.
            ui {views(dialog(activity,"themeDialog")!!.window!!.decorView).first {it.contentDescription=="ENGINEERING" || it.contentDescription=="ENGINEERING, selected"}.performClick()}
            await("Theme draft") {dialog(activity,"themeDialog")==null && views(activity.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Theme  ·  ENGINEERING"}}
            check(ConsolePreferences.read(context)==saved)
            openThemes()
            ui {check(views(dialog(activity,"themeDialog")!!.window!!.decorView).any {it.contentDescription=="ENGINEERING, selected" && it.isSelected});dialog(activity,"themeDialog")!!.dismiss();activity.finish()}
            check(context.getSharedPreferences("home",0).all==homePrefs)
            result.putString("result","PASS: current Home values; all 17 distinct theme previews; no gallery or background preview animation; D-pad grid/Close routes; selected badge and draft update; saved Home/MK-IV preferences unchanged")
            runner.finish(Activity.RESULT_OK,result)
        } catch(error:Throwable) {
            ui {settings?.finish()}
            result.putString("result","FAIL: ${error.stackTraceToString()}");runner.finish(Activity.RESULT_CANCELED,result)
        }
    }
}
