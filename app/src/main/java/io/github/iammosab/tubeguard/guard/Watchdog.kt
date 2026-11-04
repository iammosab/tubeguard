package io.github.iammosab.tubeguard.guard

import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.shell.AdbKeepAlive
import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.state.EventLog

/**
 * Checks that what keeps YouTube away is still in place, and puts it back if not. A force-stop (Settings, or a
 * TV maker's cleaner) takes our service out of the accessibility list and cancels our jobs, which would leave the
 * TV unguarded while everything else still looked fine.
 */
class Watchdog(
    private val shell: Shell,
    private val log: EventLog,
    private val jobsPending: () -> Boolean,
    private val schedule: () -> Unit,
    /** Whether our accessibility service is actually connected in this process right now. */
    private val serviceRunning: () -> Boolean,
) {
    /** True when everything was already in place; false when something was missing (and was repaired if possible). */
    fun ensure(): Boolean {
        AdbKeepAlive.allowAutoStart(shell)
        val services = shell.run("settings get secure enabled_accessibility_services")
        val enabled = shell.run("settings get secure accessibility_enabled")
        if (!services.ok || !enabled.ok) return false
        var healthy = true
        // Listed is not enough: after an update the TCL left the service listed but never started it again.
        if (OUR_SERVICE !in AccessibilitySetting.entries(services.out) || enabled.out.trim() != "1" || !serviceRunning()) {
            healthy = false
            // enable() takes an entry that is already there out and puts it back, which makes Android start it.
            val fixed = AccessibilitySetting.enable(shell, OUR_SERVICE)
            log.add("watchdog_restored", "accessibility service " + if (fixed.ok) "restarted" else "couldn't be restarted: ${fixed.out}")
        }
        if (!jobsPending()) {
            healthy = false
            schedule()
            log.add("watchdog_restored", "background checks rescheduled")
        }
        return healthy
    }
}
