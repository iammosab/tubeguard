package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySettingTest {
    private val otherApp = "com.example.remote/.KeyService"

    @Test
    fun appendsToExisting() =
        assertEquals("$otherApp:$OUR_SERVICE", AccessibilitySetting.withService(otherApp, OUR_SERVICE))

    @Test
    fun nullOrEmptyBecomesOurs() {
        assertEquals(OUR_SERVICE, AccessibilitySetting.withService(null, OUR_SERVICE))
        assertEquals(OUR_SERVICE, AccessibilitySetting.withService("", OUR_SERVICE))
        assertEquals(OUR_SERVICE, AccessibilitySetting.withService("null", OUR_SERVICE))
    }

    @Test
    fun noDuplicate() =
        assertEquals("$otherApp:$OUR_SERVICE", AccessibilitySetting.withService("$otherApp:$OUR_SERVICE", OUR_SERVICE))

    @Test
    fun removeKeepsOthers() =
        assertEquals(otherApp, AccessibilitySetting.withoutService("$OUR_SERVICE:$otherApp", OUR_SERVICE))

    @Test
    fun removeLastGivesEmptyAndDisableDeletes() {
        assertEquals("", AccessibilitySetting.withoutService(OUR_SERVICE, OUR_SERVICE))
        val shell = FakeShell(Regex("^settings get") to io.github.iammosab.tubeguard.shell.ShellResult(0, OUR_SERVICE))
        AccessibilitySetting.disable(shell, OUR_SERVICE)
        assertTrue("settings delete secure enabled_accessibility_services" in shell.commands)
    }

    @Test
    fun enableWritesAppendedValue() {
        val shell = FakeShell(Regex("^settings get") to io.github.iammosab.tubeguard.shell.ShellResult(0, otherApp))
        AccessibilitySetting.enable(shell, OUR_SERVICE)
        assertTrue("settings put secure enabled_accessibility_services '$otherApp:$OUR_SERVICE'" in shell.commands)
        assertTrue("settings put secure accessibility_enabled 1" in shell.commands)
    }

    @Test
    fun enableRebindsWhenAlreadyListed() {
        // Android only (re)binds a service when the setting changes, so an entry already present is taken out first.
        val shell = FakeShell(Regex("^settings get") to io.github.iammosab.tubeguard.shell.ShellResult(0, "$otherApp:$OUR_SERVICE"))
        AccessibilitySetting.enable(shell, OUR_SERVICE)
        val puts = shell.commands.filter { it.startsWith("settings put secure enabled_accessibility_services") }
        assertEquals(
            listOf(
                "settings put secure enabled_accessibility_services '$otherApp'",
                "settings put secure enabled_accessibility_services '$otherApp:$OUR_SERVICE'",
            ),
            puts,
        )
    }
}
