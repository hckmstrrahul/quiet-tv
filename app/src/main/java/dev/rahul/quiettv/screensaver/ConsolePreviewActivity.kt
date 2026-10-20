package dev.rahul.quiettv.screensaver

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View

class ConsolePreviewActivity : Activity() {
    private lateinit var console: ConsoleView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        console = ConsoleView(this, intent.getStringExtra("config")?.let { ConsolePreferences.decode(it) } ?: ConsolePreferences.read(this))
        setContentView(console)
    }
    override fun onResume() { super.onResume(); console.start() }
    override fun onPause() { console.stop(); super.onPause() }
    override fun onDestroy() { console.stop(); super.onDestroy() }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode in listOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) {
            finish(); return true
        }
        return super.dispatchKeyEvent(event)
    }
}
