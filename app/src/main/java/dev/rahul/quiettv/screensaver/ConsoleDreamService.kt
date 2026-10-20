package dev.rahul.quiettv.screensaver

import android.service.dreams.DreamService
import java.io.FileDescriptor
import java.io.PrintWriter

class ConsoleDreamService : DreamService() {
    private var console: ConsoleView? = null
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        console = ConsoleView(this, ConsolePreferences.read(this))
        setContentView(console!!)
    }
    override fun onDreamingStarted() { super.onDreamingStarted(); console?.start() }
    override fun onDreamingStopped() { console?.stop(); super.onDreamingStopped() }
    override fun onDetachedFromWindow() { console?.stop(); console = null; super.onDetachedFromWindow() }
    override fun onDestroy() { console?.stop(); console = null; super.onDestroy() }
    override fun dump(fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        super.dump(fd, writer, args)
        writer.println("MKIV: attached=${console != null} animating=${console?.animating ?: false} frames=${console?.frameCount ?: 0} ${console?.diagnostics() ?: ""}")
    }
}
