package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.shell.ShellResult
import java.io.File

/** Answers each command with the first rule whose regex matches it, else exit 0 and no output. */
class FakeShell(vararg rules: Pair<Regex, ShellResult>) : Shell {
    init { noPauses }

    val rules = rules.toMutableList()
    val commands = mutableListOf<String>()

    override fun run(cmd: String): ShellResult {
        commands += cmd
        return answer(cmd)
    }

    override fun install(apk: File): ShellResult {
        val cmd = "install ${apk.name}"
        commands += cmd
        return answer(cmd)
    }

    private fun answer(cmd: String) =
        rules.firstOrNull { it.first.containsMatchIn(cmd) }?.second ?: ShellResult(0, "")
}
