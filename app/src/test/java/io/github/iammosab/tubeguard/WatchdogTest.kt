package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.guard.Watchdog
import io.github.iammosab.tubeguard.shell.ShellResult
import io.github.iammosab.tubeguard.state.EventLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchdogTest {
    private val log = EventLog(MapKeyValue())
    private var scheduled = 0

    private fun tv(services: String, enabled: String = "1") = FakeShell(
        Regex("^settings get secure enabled_accessibility_services") to ShellResult(0, services),
        Regex("^settings get secure accessibility_enabled") to ShellResult(0, enabled),
    )

    @Test
    fun healthyDoesNothing() {
        val shell = tv("com.example.remote/.KeyService:$OUR_SERVICE")
        assertTrue(Watchdog(shell, log, jobsPending = { true }, schedule = { scheduled++ }, serviceRunning = { true }).ensure())
        assertTrue(shell.commands.none { it.startsWith("settings put") })
        assertEquals(0, scheduled)
    }

    @Test
    fun entryDroppedByForceStopIsAddedBack() {
        val shell = tv("com.example.remote/.KeyService")
        assertFalse(Watchdog(shell, log, jobsPending = { true }, schedule = {}, serviceRunning = { true }).ensure())
        assertTrue("settings put secure enabled_accessibility_services 'com.example.remote/.KeyService:$OUR_SERVICE'" in shell.commands)
        assertEquals("watchdog_restored", log.recent().first().kind)
    }

    @Test
    fun accessibilityOffIsTurnedOn() {
        val shell = tv(OUR_SERVICE, enabled = "0")
        Watchdog(shell, log, jobsPending = { true }, schedule = {}, serviceRunning = { true }).ensure()
        assertTrue("settings put secure accessibility_enabled 1" in shell.commands)
    }

    @Test
    fun cancelledJobsAreRescheduled() {
        val shell = tv(OUR_SERVICE)
        assertFalse(Watchdog(shell, log, jobsPending = { false }, schedule = { scheduled++ }, serviceRunning = { true }).ensure())
        assertEquals(1, scheduled)
    }

    @Test
    fun unreadableSettingIsNotHealthy() {
        val shell = FakeShell(Regex(".") to ShellResult(-100, "ADB not connected"))
        assertFalse(Watchdog(shell, log, jobsPending = { true }, schedule = {}, serviceRunning = { true }).ensure())
    }

    @Test
    fun listedButNotRunningIsRestarted() {
        // Seen on the TCL after an update: still listed, accessibility on, but Android never started it again.
        val shell = tv("com.example.launcher/com.example.launcher.ButtonService:$OUR_SERVICE")
        assertFalse(Watchdog(shell, log, jobsPending = { true }, schedule = {}, serviceRunning = { false }).ensure())
        val puts = shell.commands.filter { it.startsWith("settings put secure enabled_accessibility_services") }
        assertEquals(
            listOf(
                "settings put secure enabled_accessibility_services 'com.example.launcher/com.example.launcher.ButtonService'",
                "settings put secure enabled_accessibility_services 'com.example.launcher/com.example.launcher.ButtonService:$OUR_SERVICE'",
            ),
            puts,
        )
    }

    @Test
    fun autoStartIsAllowedEveryTime() {
        val shell = tv(OUR_SERVICE)
        Watchdog(shell, log, jobsPending = { true }, schedule = {}, serviceRunning = { true }).ensure()
        assertTrue("appops set io.github.iammosab.tubeguard AUTO_START allow" in shell.commands)
    }
}
