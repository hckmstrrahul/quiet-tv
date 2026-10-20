package dev.rahul.quiettv

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class WallpaperShuffleTest {
    @Test fun everyRoundVisitsAllImagesWithoutAdjacentRepeats() {
        val ids=WallpaperCatalog.pictures.map {it.id};val deck=WallpaperShuffle(ids,Random(52))
        var current=ids.first()
        val first=mutableSetOf(current)
        repeat(ids.size-1){val next=deck.next(current);assertNotEquals(current,next);assertTrue(first.add(next));current=next}
        assertEquals(ids.toSet(),first)
        repeat(20) {
            val round=List(ids.size) {deck.next(current).also {assertNotEquals(current,it);current=it}}
            assertEquals(ids.toSet(),round.toSet())
        }
    }
    @Test fun singleImageDoesNotLoopTryingToFindAnother() {assertEquals("only",WallpaperShuffle(listOf("only")).next("only"))}
}
