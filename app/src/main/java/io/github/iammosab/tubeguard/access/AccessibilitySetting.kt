package io.github.iammosab.tubeguard.access

import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.shell.ShellResult

const val OUR_SERVICE = "io.github.iammosab.tubeguard/io.github.iammosab.tubeguard.access.GuardService"

/** Adds or removes only our entry in `enabled_accessibility_services`; other apps' entries are kept as they are. */
object AccessibilitySetting {
    private const val KEY = "enabled_accessibility_services"

    fun withService(current: String?, ours: String): String {
        val list = entries(current)
        return (if (ours in list) list else list + ours).joinToString(":")
    }

    fun withoutService(current: String?, ours: String): String = (entries(current) - ours).joinToString(":")

    fun entries(current: String?): List<String> =
        current.orEmpty().trim().takeUnless { it == "null" }.orEmpty().split(':').filter { it.isNotBlank() }

    /** Lists our service and turns accessibility on. Android only binds a service when the setting changes, so an
     *  entry that is already there (e.g. after a force-stop left it unbound) is taken out and put back. */
    fun enable(shell: Shell, ours: String): ShellResult {
        val current = read(shell) ?: return ShellResult(1, "can't read $KEY")
        if (ours in entries(current)) write(shell, withoutService(current, ours))
        val put = write(shell, withService(current, ours))
        if (!put.ok) return put
        return shell.run("settings put secure accessibility_enabled 1")
    }

    fun disable(shell: Shell, ours: String): ShellResult {
        val current = read(shell) ?: return ShellResult(1, "can't read $KEY")
        return write(shell, withoutService(current, ours))
    }

    private fun write(shell: Shell, value: String): ShellResult =
        if (value.isEmpty()) shell.run("settings delete secure $KEY") else shell.run("settings put secure $KEY '$value'")

    private fun read(shell: Shell): String? = shell.run("settings get secure $KEY").takeIf { it.ok }?.out
}
