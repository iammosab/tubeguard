package io.github.iammosab.tubeguard.shell

import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Bounds a blocking libadb-android call. Its stream open and write wait() without a timeout, and a reply can
 * slip past that wait, so without this one lost packet would stop every later check for good. The thread is
 * interrupted at the deadline (wait() gives way to that) and the call fails with an IOException.
 */
object Deadline {
    private val timer = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "adb-deadline").apply { isDaemon = true } }

    fun <T> run(ms: Long, block: () -> T): T {
        val caller = Thread.currentThread()
        val lock = Any()
        var active = true
        var fired = false
        val alarm = timer.schedule({
            synchronized(lock) {
                if (active) {
                    fired = true
                    caller.interrupt()
                }
            }
        }, ms, TimeUnit.MILLISECONDS)
        try {
            return block()
        } catch (e: InterruptedException) {
            throw IOException("timed out after $ms ms", e)
        } catch (e: IOException) {
            if (fired) throw IOException("timed out after $ms ms", e)
            throw e
        } finally {
            // Under the lock, so an alarm can't interrupt after we've moved on to the next call.
            synchronized(lock) { active = false }
            alarm.cancel(false)
            if (fired) Thread.interrupted()
        }
    }
}
