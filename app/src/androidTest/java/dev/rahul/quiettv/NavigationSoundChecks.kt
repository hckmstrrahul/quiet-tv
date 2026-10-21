package dev.rahul.quiettv

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File

/** Exercises real sound settings and playback suppression, restoring the user's choices. */
class NavigationSoundChecks(private val test:Instrumentation) {
    private fun <T> ui(block:()->T):T {
        var result:Result<T>?=null
        test.runOnMainSync {result=runCatching(block)}
        return result!!.getOrThrow()
    }
    private fun views(view:View):List<View> = listOf(view)+(if(view is ViewGroup)(0 until view.childCount).flatMap {views(view.getChildAt(it))} else emptyList())
    private fun await(label:String,check:()->Boolean) {
        val until=SystemClock.uptimeMillis()+7000
        while(SystemClock.uptimeMillis()<until) {if(ui(check))return;Thread.sleep(100)}
        error("Timed out: $label")
    }
    fun run() {
        val context=test.targetContext
        val prefs=context.getSharedPreferences("home",0)
        val before=prefs.all.filterKeys {it!="wallpaperCurrent"}
        val enabledKey=HomeNavigationSound.ENABLED_KEY
        val volumeKey=HomeNavigationSound.VOLUME_KEY
        val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streams=listOf(AudioManager.STREAM_MUSIC,AudioManager.STREAM_SYSTEM)
        val systemVolumes=streams.map {audio.getStreamVolume(it)}
        var sound:HomeNavigationSound?=null
        var settings:AlertDialog?=null
        var failure:Throwable?=null
        try {
            test.setInTouchMode(false)
            ui {prefs.edit().remove(enabledKey).remove(volumeKey).commit()}
            val home=test.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            await("Home") {home.hasWindowFocus() && views(home.window.decorView).any {it is WeatherWidget}}
            sound=ui {HomeNavigationSound(context).also {it.start()}}
            val tone=sound
            await("tone loaded") {tone.diagnostics().contains("ready=true")}
            check(ui {tone.enabled && tone.volume==50 && tone.diagnostics().contains("gain=0.5")}) {"Default must be enabled at 50%"}
            fun count()=Regex("plays=(\\d+)").find(tone.diagnostics())!!.groupValues[1].toInt()
            ui {tone.play();check(count()==1)}
            Thread.sleep(110)
            ui {
                tone.enabled=false;tone.play();check(count()==1) {"Off played a tone"}
                tone.volume=80;tone.play();check(count()==1 && !tone.enabled) {"Changing volume unmuted Off"}
                tone.enabled=true;tone.volume=0;tone.play();check(count()==1) {"Zero volume played a tone"}
                tone.volume=30;tone.play();check(count()==2 && tone.diagnostics().contains("gain=0.3"))
                tone.volume=200;check(tone.volume==100)
                tone.volume=-1;check(tone.volume==0)
                tone.volume=50;tone.stop();tone.start()
                check(tone.volume==50 && tone.enabled)
            }
            await("restarted sound") {tone.diagnostics().contains("ready=true")}
            ui {tone.stop();tone.play();check(count()==2)}
            ui {views(home.window.decorView).first {it.contentDescription=="Appearance"}.performClick()}
            await("Home settings") {
                settings=home.javaClass.getDeclaredField("homeSettings").apply {isAccessible=true}.get(home) as? AlertDialog
                settings?.isShowing==true
            }
            fun row(prefix:String)=views(settings!!.window!!.decorView).filterIsInstance<TextView>().first {it.isClickable && it.text.startsWith(prefix)}
            ui {
                row("Sound").performClick()
                check(row("Navigation sounds").text.endsWith("On"))
                check(row("Sound volume").text.endsWith("50%"))
                row("Navigation sounds").performClick()
                check(!prefs.getBoolean(enabledKey,true) && row("Navigation sounds").text.endsWith("Off"))
                row("Navigation sounds").performClick()
                row("Sound volume").requestFocus()
                row("Sound volume").performClick()
            }
            test.waitForIdleSync();Thread.sleep(250)
            // The picker opens at 50%; Down selects 60% and OK commits it.
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            await("volume selection") {prefs.getInt(volumeKey,-1)==60 && row("Sound volume").text.endsWith("60%")}
            ui {settings!!.dismiss();views(home.window.decorView).first {it.contentDescription=="Appearance"}.performClick()}
            await("reopened settings") {
                settings=home.javaClass.getDeclaredField("homeSettings").apply {isAccessible=true}.get(home) as? AlertDialog
                settings?.isShowing==true
            }
            ui {row("Sound").performClick();check(row("Sound volume").text.endsWith("60%"));check(HomeNavigationSound(context).volume==60)}
            test.waitForIdleSync();Thread.sleep(300)
            test.uiAutomation.takeScreenshot().also {bitmap->
                File(context.filesDir,"sound-settings-test.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
            check(streams.map {audio.getStreamVolume(it)}==systemVolumes) {"System volume was changed"}
        } catch(error:Throwable) {failure=IllegalStateException("Volume=${prefs.getInt(volumeKey,-1)}; focus=${test.uiAutomation.rootInActiveWindow?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)?.text}",error)}
        finally {
            ui {
                settings?.dismiss();sound?.stop()
                prefs.edit().apply {
                    if(before.containsKey(enabledKey))putBoolean(enabledKey,before[enabledKey] as Boolean) else remove(enabledKey)
                    if(before.containsKey(volumeKey))putInt(volumeKey,before[volumeKey] as Int) else remove(volumeKey)
                }.commit()
            }
        }
        if(failure==null && prefs.all.filterKeys {it!="wallpaperCurrent"}!=before)failure=IllegalStateException("Saved Home choices changed")
        test.finish(if(failure==null)Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply {
            putString("result",failure?.let {"FAIL: ${it.stackTraceToString()}"} ?: "PASS: default 50%; Off and zero suppress playback; volume change preserves Off; gain and range; restart/persistence; real Sound rows and D-pad volume picker; unchanged system volume and restored Home preferences")
        })
    }
}
