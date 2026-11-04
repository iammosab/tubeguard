package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.shell.ShellResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeShellTest {
    @Test
    fun returnsFirstMatchingRuleAndRecords() {
        val shell = FakeShell(
            Regex("^pm list") to ShellResult(0, "package:a"),
            Regex("^pm") to ShellResult(1, "other"),
        )
        assertEquals(ShellResult(0, "package:a"), shell.run("pm list packages"))
        assertEquals(listOf("pm list packages"), shell.commands)
    }
}
