package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.shell.Deadline
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DeadlineTest {
    @Test
    fun waitWithoutTimeoutIsInterruptedAtTheDeadline() {
        // libadb-android's open and write wait() with no timeout; the deadline must break them out.
        val lock = Object()
        val start = System.currentTimeMillis()
        val failed = try {
            Deadline.run(300) { synchronized(lock) { lock.wait() } }
            false
        } catch (e: IOException) {
            true
        }
        assertTrue(failed)
        assertTrue(System.currentTimeMillis() - start < 3_000)
        assertFalse("no interrupt left behind", Thread.currentThread().isInterrupted)
    }

    @Test
    fun finishedWorkLeavesNoInterruptBehind() {
        repeat(50) { Deadline.run(1) { } }
        Thread.sleep(20)
        assertFalse(Thread.currentThread().isInterrupted)
    }
}
