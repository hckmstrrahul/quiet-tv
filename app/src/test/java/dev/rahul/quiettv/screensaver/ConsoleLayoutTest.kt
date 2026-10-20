package dev.rahul.quiettv.screensaver

import org.junit.Assert.*
import org.junit.Test

class ConsoleLayoutTest {
    @Test fun layoutsFillEveryCellExactlyOnceAcrossDensitiesAndSeeds() {
        for (density in 0..2) for(seed in 0L..80L) {
            val c=ConsoleConfig(density=density)
            val counts=IntArray(c.columns*c.rows)
            val tiles=ConsoleLayout.pack(c.columns,c.rows,seed)
            for(t in tiles) {
                assertTrue(t.width in 1..2 && t.height in 1..2)
                assertTrue(t.x>=0 && t.y>=0 && t.x+t.width<=c.columns && t.y+t.height<=c.rows)
                for(y in t.y until t.y+t.height)for(x in t.x until t.x+t.width)counts[y*c.columns+x]++
            }
            assertTrue(counts.all { it==1 })
            assertEquals(tiles,ConsoleLayout.pack(c.columns,c.rows,seed))
        }
    }
    @Test fun malformedPreferencesStayWithinRenderingBounds() {
        val c=ConsoleConfig(theme=99,topic=-7,speed=33,density=-9,cadence=42,fps=240,brightness=200,layoutSeconds=-1).sanitized()
        assertEquals(16,c.theme);assertEquals(0,c.topic);assertEquals(2,c.speed)
        assertEquals(8,c.columns);assertEquals(4,c.rows);assertEquals(5000,c.replacementMillis)
        assertEquals(20,c.fps);assertEquals(100,c.brightness);assertEquals(0,c.layoutSeconds)
    }
    @Test fun allReferenceFamiliesAndThemesArePresent() {
        assertEquals(30,ConsoleCatalog.visualizations.distinct().size)
        assertEquals(17,ConsoleCatalog.palettes.size)
        assertEquals(4,ConsoleCatalog.topics.size)
        assertTrue(ConsoleCatalog.palettes.all { p -> listOf(p.bg,p.fg,p.accent,p.grid).all { it.matches(Regex("#[0-9a-fA-F]{6}")) } })
    }
}
