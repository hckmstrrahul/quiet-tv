package dev.rahul.quiettv.screensaver

import kotlin.random.Random

/** Shuffled rounds give every panel a turn, forever; colors never repeat consecutively. */
internal class ConsoleColorCycle(private val panelCount: Int, private val paletteCount: Int, private val random: Random) {
    init { require(panelCount > 0 && paletteCount > 1) }
    private var order = emptyList<Int>()
    private var cursor = 0
    fun nextPanel(): Int {
        if (cursor == order.size) { order = (0 until panelCount).shuffled(random); cursor = 0 }
        return order[cursor++]
    }
    fun nextPalette(current: Int): Int = (current + 1 + random.nextInt(paletteCount - 1)) % paletteCount
}
