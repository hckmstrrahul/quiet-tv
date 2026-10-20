package dev.rahul.quiettv.screensaver

import kotlin.random.Random

data class ConsoleConfig(
    val theme: Int = 0,
    val topic: Int = 3,
    val speed: Int = 0,
    val automatic: Boolean = true,
    val mixedPalettes: Boolean = true,
    val density: Int = 2,
    val cadence: Int = 1,
    val fps: Int = 20,
    val brightness: Int = 85,
    val scanlines: Boolean = true,
    val layoutSeconds: Int = 0,
    val seed: Long = 42,
    val replaceInstruments: Boolean = false
) {
    fun sanitized() = copy(theme = theme.coerceIn(0, 16), topic = topic.coerceIn(0, 3),
        speed = speed.coerceIn(0, 2), density = density.coerceIn(0, 2), cadence = cadence.coerceIn(0, 2),
        fps = if (fps == 30) 30 else 20, brightness = brightness.coerceIn(35, 100),
        layoutSeconds = if (layoutSeconds in listOf(0, 30, 60, 120)) layoutSeconds else 0)
    val randomColorsForever get() = automatic && mixedPalettes
    fun withRandomColorsForever(enabled: Boolean) = if (enabled)
        copy(automatic = true, mixedPalettes = true)
        else copy(automatic = false)
    val columns get() = listOf(8, 10, 12)[density]
    val rows get() = listOf(4, 6, 7)[density]
    val replacementMillis get() = listOf(450L, 1800L, 5000L)[cadence]
}

data class ConsoleTile(val x: Int, val y: Int, val width: Int, val height: Int)
object ConsoleLayout {
    fun pack(columns: Int, rows: Int, seed: Long): List<ConsoleTile> {
        require(columns in 1..20 && rows in 1..12)
        val random = Random(seed)
        val filled = BooleanArray(columns * rows)
        val result = mutableListOf<ConsoleTile>()
        for (y in 0 until rows) for (x in 0 until columns) {
            if (filled[y * columns + x]) continue
            var w = if (random.nextFloat() < .25f && x + 1 < columns && !filled[y * columns + x + 1]) 2 else 1
            var h = if (random.nextFloat() < .25f && y + 1 < rows) 2 else 1
            if (h == 2 && (x until x + w).any { filled[(y + 1) * columns + it] }) h = 1
            for (dy in 0 until h) for (dx in 0 until w) filled[(y + dy) * columns + x + dx] = true
            result.add(ConsoleTile(x, y, w, h))
        }
        return result
    }
}
