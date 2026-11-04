package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.flow.Undo
import io.github.iammosab.tubeguard.guard.GuardResult
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.ShellResult
import io.github.iammosab.tubeguard.state.EventLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoTest {
    private val store = MapKeyValue("protection" to "on")
    private val log = EventLog(store)
    private var cancelled = 0

    private fun undo(shell: FakeShell, isTcl: Boolean = true) =
        Undo(shell, TclKeys(shell, store), log, store, isTcl = isTcl, cancelJobs = { cancelled++ })

    private fun tv(installExisting: ShellResult = ShellResult(0, "Package com.google.android.youtube.tv installed for user: 0")) = FakeShell(
        Regex("^cmd package install-existing") to installExisting,
        Regex("^settings get global CustomKeyList") to ShellResult(0, "[\"4065\",\"190\"]"),
        Regex("^settings get secure") to ShellResult(0, "com.example.remote/.KeyService:$OUR_SERVICE"),
        Regex("^settings get") to ShellResult(0, "null"),
    )

    @Test
    fun restoresInOrder() {
        val shell = tv()
        val steps = undo(shell).run()
        assertEquals(listOf("protection", "youtube", "button", "accessibility", "schedule"), steps.map { it.name })
        assertTrue(steps.all { it.ok })
        assertEquals("off", store.get("protection"))
        assertTrue("cmd package install-existing com.google.android.youtube.tv" in shell.commands)
        assertTrue("settings delete global CustomKey_4065" in shell.commands)
        assertTrue("settings put secure enabled_accessibility_services 'com.example.remote/.KeyService'" in shell.commands)
        assertEquals(1, cancelled)
    }

    @Test
    fun protectionIsOffBeforeYoutubeComesBack() {
        // Otherwise the watchdog sees YouTube return and removes it again straight away.
        var protectionAtRestore: String? = null
        val shell = tv()
        val watching = object : io.github.iammosab.tubeguard.shell.Shell by shell {
            override fun run(cmd: String): ShellResult {
                if (cmd.startsWith("cmd package install-existing")) protectionAtRestore = store.get("protection")
                return shell.run(cmd)
            }
        }
        Undo(watching, TclKeys(watching, store), log, store, isTcl = true, cancelJobs = {}).run()
        assertEquals("off", protectionAtRestore)
    }

    @Test
    fun continuesAfterFailedStep() {
        val shell = tv(installExisting = ShellResult(1, "Error: package not found"))
        val steps = undo(shell).run()
        assertEquals(5, steps.size)
        assertEquals(listOf("youtube"), steps.filter { !it.ok }.map { it.name })
        assertTrue("settings delete global CustomKey_4065" in shell.commands)
        assertEquals(1, cancelled)
        assertEquals("undo_failed", log.recent().first { it.kind.startsWith("undo") }.kind)
    }

    @Test
    fun guardIsNoOpAfterUndo() {
        undo(tv()).run()
        val shell = FakeShell(Regex("^pm list packages") to ShellResult(0, "package:com.google.android.youtube.tv"))
        assertEquals(GuardResult.Absent, YouTubeGuard(shell, log, store = store).enforce("package_added"))
        assertTrue(shell.commands.isEmpty())
    }

    @Test
    fun nonTclLeavesButtonTableAlone() {
        val shell = tv()
        undo(shell, isTcl = false).run()
        assertTrue(shell.commands.none { it.contains("CustomKey") })
    }
}
