package io.github.iammosab.tubeguard.flow

import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.KeyValue

/**
 * Puts the TV back to its factory behaviour: official YouTube back, TCL's own button, our accessibility entry and
 * jobs gone. TizenTube stays. Every step is tried even when an earlier one fails.
 */
class Undo(
    private val shell: Shell,
    private val keys: TclKeys,
    private val log: EventLog,
    private val store: KeyValue,
    private val isTcl: Boolean,
    private val cancelJobs: () -> Unit,
) {
    fun run(): List<StepResult> {
        val steps = mutableListOf<StepResult>()
        // First, so the watchdog doesn't remove YouTube again the moment it is back.
        store.put(Setup.PROTECTION, "off")
        steps += StepResult("protection", true)

        val youtube = shell.run("cmd package install-existing ${YouTubeGuard.YOUTUBE}")
        steps += StepResult("youtube", youtube.ok && youtube.out.contains("installed"), youtube.out)
        if (isTcl) steps += StepResult("button", keys.undo())
        val a11y = AccessibilitySetting.disable(shell, OUR_SERVICE)
        steps += StepResult("accessibility", a11y.ok, a11y.out)
        cancelJobs()
        steps += StepResult("schedule", true)

        val failed = steps.filter { !it.ok }
        if (failed.isEmpty()) log.add("undone", "TV is back to its own YouTube and button")
        else log.add("undo_failed", failed.joinToString { "${it.name}: ${it.detail}" })
        return steps
    }
}
