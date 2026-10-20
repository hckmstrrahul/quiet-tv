package dev.rahul.quiettv

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/** Owner-thread lifecycle; results from an earlier foreground session are discarded. */
class ForegroundTasks(private val post: (() -> Unit) -> Unit) {
    @Volatile private var generation = 0L
    @Volatile var isActive = false
        private set
    private var executor: ExecutorService? = null

    fun start() {
        stop()
        executor = Executors.newSingleThreadExecutor { job -> Thread(job, "quiet-tv-images") }
        isActive = true
    }

    fun stop() {
        isActive = false
        generation++
        executor?.shutdownNow()
        executor = null
    }

    fun <T> submit(work: () -> T, deliver: (T) -> Unit) {
        val ticket = generation
        val pool = executor ?: return
        fun current() = isActive && ticket == generation
        try {
            pool.execute {
                if (current()) {
                    val result = work()
                    if (current()) post { if (current()) deliver(result) }
                }
            }
        } catch (_: RejectedExecutionException) {
            // A stopped session must never enqueue work on its replacement.
        }
    }
}
