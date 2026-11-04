package io.github.iammosab.tubeguard.shell

import java.io.File

/** What a shell command printed (stdout and stderr together) and its exit code. */
data class ShellResult(val exit: Int, val out: String) {
    val ok get() = exit == 0
}

/** Runs commands as the TV's ADB shell user; everything privileged goes through here. */
interface Shell {
    fun run(cmd: String): ShellResult

    /** Installs or updates [apk] as shell (`cmd package install`); ok when the output says Success. */
    fun install(apk: File): ShellResult
}
