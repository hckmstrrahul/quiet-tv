package dev.rahul.quiettv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.ImageView
import java.io.File

/** Checks the real Home render and crossfade without changing saved Home preferences. */
class CompanionChecks(private val test: Instrumentation) {
    fun run() {
        var backdrop: HomeBackdrop? = null
        try {
            val activity = test.startActivitySync(Intent(test.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            test.waitForIdleSync(); SystemClock.sleep(1600)
            test.runOnMainSync {
                val snapshot = activity.companionPreview()
                check(snapshot.getJSONArray("tiles").length() >= 2)
                check(snapshot.getString("header").startsWith("data:image/png;base64,"))
                File(activity.filesDir,"companion-preview.json").writeText(snapshot.toString())
                val preferences = activity.getSharedPreferences("companion-motion-test",0)
                preferences.edit().clear().putString("wallpaperId","wallpaper_03").putBoolean("wallpaperShuffle",true).commit()
                backdrop = HomeBackdrop(activity,preferences)
                activity.addContentView(backdrop,FrameLayout.LayoutParams(320,180))
                backdrop!!.start()
            }
            SystemClock.sleep(1300)
            var original = 0
            test.runOnMainSync { original=backdrop!!.changes;check(original==1);check(backdrop!!.scheduled) }
            SystemClock.sleep(18_000)
            test.runOnMainSync {check(backdrop!!.changes==original) {"Shuffle changed before 20 seconds"}}
            val until=SystemClock.uptimeMillis()+5000
            var changed=false
            while(!changed && SystemClock.uptimeMillis()<until) {
                test.runOnMainSync {changed=backdrop!!.changes>original};SystemClock.sleep(40)
            }
            check(changed) {"Shuffle did not advance"}
            SystemClock.sleep(450)
            test.runOnMainSync {
                val pictures=(0 until backdrop!!.childCount).map {backdrop!!.getChildAt(it)}.filterIsInstance<ImageView>()
                check(pictures.any {it.alpha in .05f.. .95f}) {"Missing intermediate crossfade frame"}
                check(pictures.any {it.alpha==1f}) {"Crossfade must retain an opaque underlying image"}
                backdrop!!.pause();check(!backdrop!!.scheduled)
                check(pictures.any {it.drawable!=null && it.alpha==1f}) {"Paused Home lost its wallpaper"}
                backdrop!!.stop();check(pictures.all {it.drawable==null})
            }
            test.finish(Activity.RESULT_OK,Bundle().apply {putString("result","Native preview, 20-second shuffle, crossfade, pause retention and stop cleanup passed")})
        } catch(error:Throwable) {
            test.finish(Activity.RESULT_CANCELED,Bundle().apply {putString("error",error.stackTraceToString())})
        } finally {
            test.runOnMainSync {backdrop?.stop();(backdrop?.parent as? android.view.ViewGroup)?.removeView(backdrop)}
            test.targetContext.getSharedPreferences("companion-motion-test",0).edit().clear().commit()
        }
    }
}
