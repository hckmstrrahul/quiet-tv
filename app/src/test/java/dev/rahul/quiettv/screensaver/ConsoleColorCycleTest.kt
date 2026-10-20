package dev.rahul.quiettv.screensaver

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class ConsoleColorCycleTest {
    @Test fun everyPanelKeepsGettingNewColorsAcrossManyRounds() {
        for (count in listOf(1, 23, 48, 84)) for (seed in 0..12) {
            val cycle = ConsoleColorCycle(count, 17, Random(seed))
            val palettes = IntArray(count) { it % 17 }
            repeat(100) {
                val visited = mutableSetOf<Int>()
                repeat(count) {
                    val panel = cycle.nextPanel()
                    assertTrue(panel in palettes.indices)
                    assertTrue("A panel must not be skipped or repeated within a round", visited.add(panel))
                    val color = cycle.nextPalette(palettes[panel])
                    assertTrue(color in 0..16)
                    assertNotEquals(palettes[panel], color)
                    palettes[panel] = color
                }
                assertEquals(count, visited.size)
            }
        }
    }
}
