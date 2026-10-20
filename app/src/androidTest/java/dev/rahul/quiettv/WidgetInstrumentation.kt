package dev.rahul.quiettv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/** Separate test APK only. Publishes a clearly labelled media session with NO audio. */
class WidgetInstrumentation:Instrumentation() {
    private var appearanceOnly=false
    override fun onCreate(arguments:Bundle?) {super.onCreate(arguments);appearanceOnly=arguments?.getString("suite")=="appearance";start()}
    override fun onStart() {
        if(appearanceOnly){AppearanceChecks(this).run();return}
        var session:MediaSession?=null
        val events=CopyOnWriteArrayList<String>()
        val report=Bundle()
        try {
            val activity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            lateinit var widget:MusicWidget
            lateinit var backdrop:HomeBackdrop
            lateinit var weather:WeatherWidget
            lateinit var clock:HomeTimeWidget
            fun views(root:View):List<View> = listOf(root)+(if(root is ViewGroup)(0 until root.childCount).flatMap {views(root.getChildAt(it))} else emptyList())
            fun <T> ui(block:()->T):T {var result:Result<T>?=null;runOnMainSync {result=runCatching(block)};return result!!.getOrThrow()}
            fun await(label:String,check:()->Boolean) {
                val deadline=SystemClock.elapsedRealtime()+7000
                while(SystemClock.elapsedRealtime()<deadline) {if(ui(check))return;Thread.sleep(100)}
                error("Timed out: $label")
            }
            await("Home views") {views(activity.window.decorView).any {it is MusicWidget}}
            ui {
                widget=views(activity.window.decorView).filterIsInstance<MusicWidget>().first()
                backdrop=views(activity.window.decorView).filterIsInstance<HomeBackdrop>().first()
                weather=views(activity.window.decorView).filterIsInstance<WeatherWidget>().first()
                clock=views(activity.window.decorView).filterIsInstance<HomeTimeWidget>().first()
            }
            fun soundState():String {
                val out=java.io.StringWriter()
                activity.dump("",null,java.io.PrintWriter(out),null)
                return out.toString().lineSequence().first {it.startsWith("Navigation sound:")}
            }
            fun soundPlays()=Regex("plays=(\\d+)").find(soundState())!!.groupValues[1].toInt()
            await("foreground sound loaded") {activity.hasWindowFocus() && soundState().contains("ready=true")}
            check(ui {soundPlays()==0}) {"Navigation tone played during initial render"}
            ui {
                check(weather.left-clock.right==clock.left-widget.right) {"Widget gaps differ"}
                val header=views(activity.window.decorView).first {it.contentDescription=="TV settings"}
                val location=IntArray(2);header.getLocationOnScreen(location)
                fun pointer(action:Int) {
                    val event=android.view.MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,(location[0]+header.width/2).toFloat(),(location[1]+header.height/2).toFloat(),0)
                    event.source=android.view.InputDevice.SOURCE_MOUSE;activity.dispatchGenericMotionEvent(event);event.recycle()
                }
                pointer(android.view.MotionEvent.ACTION_HOVER_ENTER)
                check(soundPlays()==1) {"Pointer entry did not play the Home tone"}
                pointer(android.view.MotionEvent.ACTION_HOVER_MOVE)
                check(soundPlays()==1) {"Pointer movement repeated the tone within one item"}
                pointer(android.view.MotionEvent.ACTION_HOVER_EXIT)
            }
            Thread.sleep(150)
            var beforeKeys=0
            ui {
                val header=views(activity.window.decorView).first {it.contentDescription=="TV settings"}
                header.requestFocusFromTouch()
                beforeKeys=soundPlays();check(activity.hasWindowFocus())
            }
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
            await("D-pad focus and sound") {activity.currentFocus?.contentDescription=="Appearance" && soundPlays()==beforeKeys+1}
            Thread.sleep(150)
            ui {beforeKeys=soundPlays();check(activity.hasWindowFocus())}
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);waitForIdleSync()
            check(ui {soundPlays()==beforeKeys}) {"Blocked navigation played a tone"}
            val firstFrames=ui {clock.diagnostics()}
            Thread.sleep(250)
            check(ui {clock.diagnostics()!=firstFrames && clock.diagnostics().contains("running=true")}) {"Clock did not advance"}
            ui {
                val header=views(activity.window.decorView).first {it.contentDescription=="TV settings"}
                val event=android.view.MotionEvent.obtain(0,android.os.SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_HOVER_ENTER,10f,10f,0)
                event.source=android.view.InputDevice.SOURCE_MOUSE;header.dispatchGenericMotionEvent(event);event.recycle()
                check(views(header).filterIsInstance<TextView>().any {it.text.toString()=="TV settings" && it.visibility==View.VISIBLE}) {"Missing hover label"}
                val exit=android.view.MotionEvent.obtain(0,android.os.SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_HOVER_EXIT,10f,10f,0)
                exit.source=android.view.InputDevice.SOURCE_MOUSE;header.dispatchGenericMotionEvent(exit);exit.recycle()
                check(views(header).filterIsInstance<TextView>().all {it.visibility==View.INVISIBLE}) {"Hover label remained visible"}
            }
            fun labels()=views(widget).filterIsInstance<TextView>().map {it.text.toString()}
            fun click(label:String) {ui {check(views(widget).first {it.contentDescription==label}.performClick())}}
            val actions=PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO
            fun state(value:Int,position:Long=84_000)=PlaybackState.Builder().setActions(actions).setState(value,position,if(value==PlaybackState.STATE_PLAYING)1f else 0f,SystemClock.elapsedRealtime()).build()
            val artwork=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.rgb(130,82,195))}
            fun metadata(title:String)=MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST,"Controlled test · no audio")
                .putLong(MediaMetadata.METADATA_KEY_DURATION,222_000).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,artwork).build()
            ui {
                session=MediaSession(targetContext,"Quiet TV controlled widget test").apply {
                    setCallback(object:MediaSession.Callback() {
                        override fun onPause(){events.add("pause");setPlaybackState(state(PlaybackState.STATE_PAUSED))}
                        override fun onPlay(){events.add("play");setPlaybackState(state(PlaybackState.STATE_PLAYING))}
                        override fun onSkipToNext(){events.add("next");setMetadata(metadata("Quiet TV next test"))}
                        override fun onSkipToPrevious(){events.add("previous");setMetadata(metadata("Quiet TV test track"))}
                        override fun onSeekTo(pos:Long){events.add("seek");setPlaybackState(state(PlaybackState.STATE_PLAYING,pos))}
                    },Handler(Looper.getMainLooper()))
                    setMetadata(metadata("Quiet TV test track"));setPlaybackState(state(PlaybackState.STATE_PLAYING));isActive=true
                }
            }
            await("media title and duration") {"Quiet TV test track" in labels() && "3:42" in labels()}
            check(events.isEmpty()) {"Widget sent an unsolicited playback command"}
            waitForIdleSync();Thread.sleep(700)
            val screenshot=uiAutomation.takeScreenshot()
            File(targetContext.filesDir,"widget-test.png").outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()
            click("Pause");await("pause callback") {"pause" in events && views(widget).any {it.contentDescription=="Play"}}
            click("Play");await("play callback") {"play" in events}
            click("Next");await("next metadata") {"next" in events && "Quiet TV next test" in labels()}
            click("Previous");await("previous metadata") {"previous" in events && "Quiet TV test track" in labels()}
            ui {
                val seek=views(widget).filterIsInstance<SeekBar>().first();check(seek.isEnabled);seek.requestFocus()
                seek.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT))
                seek.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            await("seek callback") {"seek" in events}
            ui {activity.startActivity(Intent(Settings.ACTION_SETTINGS))}
            await("frozen backdrop under system settings") {backdrop.diagnostics().contains("active=false") && backdrop.diagnostics().contains("retained=true")}
            check(ui {widget.diagnostics().contains("active=false") && widget.diagnostics().contains("tick=false")})
            check(ui {weather.diagnostics().contains("active=false") && weather.diagnostics().contains("fetching=false")}) {"Weather work continued behind Settings"}
            check(ui {clock.diagnostics().contains("running=false")})
            check(ui {soundState().contains("active=false ready=false")}) {"Navigation audio resources retained behind Settings"}
            waitForIdleSync();Thread.sleep(1000)
            val stoppedClock=ui {clock.diagnostics()}
            Thread.sleep(300)
            check(ui {clock.diagnostics()==stoppedClock}) {"Clock still drawing behind Settings"}
            val settingsShot=uiAutomation.takeScreenshot()
            File(targetContext.filesDir,"widget-settings-test.png").outputStream().use {settingsShot.compress(Bitmap.CompressFormat.PNG,100,it)};settingsShot.recycle()
            ui {session?.release();session=null}
            ui {activity.startActivity(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
            await("session cleanup") {views(activity.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Nothing playing"}}
            report.putString("result","PASS: equal widget gaps; Home audio load, hover, no repeat within item, D-pad, no blocked-navigation tone, resource release; metadata, duration, play/pause, previous/next, seek, frozen backdrop, stopped background listeners/ticker/weather/clock, clock animation, hover labels, session cleanup")
            report.putString("events",events.joinToString(","))
            finish(Activity.RESULT_OK,report)
        } catch(error:Throwable) {
            session?.release()
            report.putString("result","FAIL: "+error.stackTraceToString())
            finish(Activity.RESULT_CANCELED,report)
        }
    }
}
