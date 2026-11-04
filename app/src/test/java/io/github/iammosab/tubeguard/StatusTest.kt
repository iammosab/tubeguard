package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.ui.Headline
import io.github.iammosab.tubeguard.ui.Status
import io.github.iammosab.tubeguard.ui.headline
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusTest {
    private val good = Status(adbConnected = true, protection = "on", youtubePresent = false)

    @Test
    fun allGoodIsProtected() = assertEquals(Headline.PROTECTED, good.headline())

    @Test
    fun youtubePresentWinsOverEverything() =
        assertEquals(Headline.YOUTUBE_PRESENT, good.copy(youtubePresent = true).headline())

    @Test
    fun noAdbIsNotProtected() =
        assertEquals(Headline.NOT_PROTECTED_NO_ADB, good.copy(adbConnected = false, youtubePresent = null).headline())

    @Test
    fun undoneIsOffEvenWithYoutubeBack() =
        assertEquals(Headline.OFF, good.copy(protection = "off", youtubePresent = true).headline())

    @Test
    fun neverSetUpWithAdbIsNotSetUp() =
        assertEquals(Headline.NOT_SET_UP, good.copy(protection = null).headline())

    @Test
    fun neverSetUpWithoutAdbIsStillNotSetUp() =
        // Before setup the app doesn't even try the TV's ADB, so "can't reach it" would be the wrong story.
        assertEquals(Headline.NOT_SET_UP, good.copy(protection = null, adbConnected = false, youtubePresent = null).headline())

    @Test
    fun watchdogOffIsNotProtected() =
        assertEquals(Headline.WATCHDOG_OFF, good.copy(watchdogOk = false).headline())

    @Test
    fun cantCheckYoutubeIsNotProtected() =
        assertEquals(Headline.CHECK_FAILED, good.copy(youtubePresent = null).headline())

    @Test
    fun undoThatDidNotBringYoutubeBackIsIncomplete() {
        assertEquals(Headline.UNDO_INCOMPLETE, good.copy(protection = "off", youtubePresent = false).headline())
        assertEquals(Headline.UNDO_INCOMPLETE, good.copy(protection = "off", youtubePresent = null).headline())
    }
}
