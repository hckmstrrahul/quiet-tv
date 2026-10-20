package dev.rahul.quiettv

import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Test

class ForegroundTasksTest {
    @Test fun delayedUiResultCannotRenderAfterPauseOrInANewSession() {
        val callbacks = LinkedBlockingQueue<() -> Unit>()
        val tasks = ForegroundTasks { callbacks.add(it) }
        var delivered = ""
        try {
            tasks.start()
            tasks.submit({ "old" }) { delivered = it }
            val oldCallback = callbacks.poll(2, TimeUnit.SECONDS)!!
            tasks.stop()
            oldCallback()
            assertEquals("", delivered)
            tasks.start()
            oldCallback()
            assertEquals("", delivered)
            tasks.submit({ "new" }) { delivered = it }
            callbacks.poll(2, TimeUnit.SECONDS)!!()
            assertEquals("new", delivered)
        } finally { tasks.stop() }
    }

    @Test fun stoppingInterruptsRunningWorkAndDropsQueuedImages() {
        val entered = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val queuedRan = AtomicBoolean(false)
        val callbacks = LinkedBlockingQueue<() -> Unit>()
        val tasks = ForegroundTasks { callbacks.add(it) }
        try {
            tasks.start()
            tasks.submit({
                entered.countDown()
                try { CountDownLatch(1).await() }
                catch (_: InterruptedException) { interrupted.countDown() }
            }) { fail("Cancelled work delivered a result") }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            tasks.submit({ queuedRan.set(true) }) { fail("Queued image delivered") }
            tasks.stop()
            assertTrue(interrupted.await(2, TimeUnit.SECONDS))
            assertFalse(tasks.isActive)
            assertFalse(queuedRan.get())
            assertNull(callbacks.poll(100, TimeUnit.MILLISECONDS))
        } finally { tasks.stop() }
    }
}
