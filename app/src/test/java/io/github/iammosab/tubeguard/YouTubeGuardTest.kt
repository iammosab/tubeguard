package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.guard.GuardResult
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.ShellResult
import io.github.iammosab.tubeguard.state.EventLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeGuardTest {
    private val log = EventLog(MapKeyValue())
    private val uninstall = "pm uninstall -k --user 0 com.google.android.youtube.tv"

    private fun listing(out: String, uninstallOut: String = "Success", uninstallExit: Int = 0) = FakeShell(
        Regex("^pm list packages") to ShellResult(0, out),
        Regex("^pm uninstall") to ShellResult(uninstallExit, uninstallOut),
    )

    @Test
    fun absentWhenNotListed() {
        val shell = listing("")
        assertEquals(GuardResult.Absent, YouTubeGuard(shell, log).enforce("boot"))
        assertTrue(shell.commands.none { it.startsWith("pm uninstall") })
    }

    @Test
    fun tvMusicIsNotYoutube() {
        val shell = listing("package:com.google.android.youtube.tvmusic  installer=null")
        assertEquals(GuardResult.Absent, YouTubeGuard(shell, log).enforce("boot"))
        assertTrue(shell.commands.none { it.startsWith("pm uninstall") })
    }

    @Test
    fun removesWhenPresent() {
        val shell = listing("package:com.google.android.youtube.tv  installer=com.android.vending")
        val result = YouTubeGuard(shell, log).enforce("package_added")
        assertEquals(GuardResult.Removed("installer=com.android.vending"), result)
        assertTrue(uninstall in shell.commands)
        val events = log.recent()
        assertEquals(1, events.size)
        assertEquals("removed", events[0].kind)
        assertTrue(events[0].detail.contains("package_added"))
    }

    @Test
    fun disabledYoutubeIsStillRemoved() {
        // A disabled-but-installed app is still listed by pm list packages; it must be removed, not skipped.
        val shell = listing("package:com.google.android.youtube.tv  installer=null")
        assertTrue(YouTubeGuard(shell, log).enforce("periodic") is GuardResult.Removed)
        assertTrue(uninstall in shell.commands)
    }

    @Test
    fun failureIsLoggedAndReturned() {
        val shell = listing(
            "package:com.google.android.youtube.tv",
            uninstallOut = "Failure [DELETE_FAILED_INTERNAL_ERROR]", uninstallExit = 1,
        )
        val result = YouTubeGuard(shell, log).enforce("boot")
        assertTrue(result is GuardResult.Failed)
        assertEquals("remove_failed", log.recent().single().kind)
    }

    @Test
    fun noAdbIsAFailureNotAbsent() {
        val shell = FakeShell(Regex(".") to ShellResult(-100, "ADB not connected"))
        assertTrue(YouTubeGuard(shell, log).enforce("boot") is GuardResult.Failed)
    }

    @Test
    fun doesNothingBeforeSetup() {
        // Opening the app (or a boot) before the owner ran setup must not remove anything.
        val shell = listing("package:com.google.android.youtube.tv")
        val result = YouTubeGuard(shell, log, store = MapKeyValue()).enforce("app_open")
        assertEquals(GuardResult.Absent, result)
        assertTrue(shell.commands.isEmpty())
    }

    @Test
    fun enforcesOnceSetUp() {
        val shell = listing("package:com.google.android.youtube.tv")
        val result = YouTubeGuard(shell, log, store = MapKeyValue("protection" to "on")).enforce("boot")
        assertTrue(result is GuardResult.Removed)
    }
}
