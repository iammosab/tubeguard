package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.ui.screens.DeviceState
import io.github.iammosab.tubeguard.ui.screens.SettingsPage
import io.github.iammosab.tubeguard.ui.screens.SetupStep
import io.github.iammosab.tubeguard.ui.screens.done
import io.github.iammosab.tubeguard.ui.screens.page
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupGuideTest {
    @Test
    fun eachStepOpensTheRightPage() {
        assertEquals(SettingsPage.ABOUT, SetupStep.DEVELOPER.page())
        assertEquals(SettingsPage.DEVELOPER_OPTIONS, SetupStep.USB.page())
        assertEquals(SettingsPage.DEVELOPER_OPTIONS, SetupStep.PAIR.page())
        assertEquals(null, SetupStep.ALLOW.page())
    }

    @Test
    fun pagesUseTheStandardShortcuts() {
        assertEquals("android.settings.DEVICE_INFO_SETTINGS", SettingsPage.ABOUT.action)
        assertEquals("android.settings.APPLICATION_DEVELOPMENT_SETTINGS", SettingsPage.DEVELOPER_OPTIONS.action)
    }

    @Test
    fun stepsKnowWhenTheTvIsAlreadyThere() {
        val nothing = DeviceState(developerOptions = false, usbDebugging = false)
        val dev = DeviceState(developerOptions = true, usbDebugging = false)
        val both = DeviceState(developerOptions = true, usbDebugging = true)
        assertEquals(false, SetupStep.DEVELOPER.done(nothing))
        assertEquals(true, SetupStep.DEVELOPER.done(dev))
        assertEquals(false, SetupStep.USB.done(dev))
        assertEquals(true, SetupStep.USB.done(both))
        // Unknown for steps the TV can't report on.
        assertEquals(null, SetupStep.ALLOW.done(both))
    }
}
