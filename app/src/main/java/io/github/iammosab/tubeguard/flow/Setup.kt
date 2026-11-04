package io.github.iammosab.tubeguard.flow

import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.guard.GuardResult
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.AdbKeepAlive
import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.KeyValue
import io.github.iammosab.tubeguard.update.UpdateResult

/**
 * First run once the TV's ADB answers. A TV already set up by hand (YouTube gone, TizenTube installed, TCL button
 * mapped) is adopted as it is: the app only adds what it needs to keep it that way.
 */
class Setup(
    private val shell: Shell,
    private val guard: YouTubeGuard,
    private val keys: TclKeys,
    private val log: EventLog,
    private val store: KeyValue,
    private val tizenTubeVersion: () -> String?,
    private val installTizenTube: (abis: List<String>) -> UpdateResult,
    private val schedule: () -> Unit,
) {
    fun run(abis: List<String>, isTcl: Boolean): List<StepResult> {
        val steps = mutableListOf<StepResult>()
        val permission = shell.run("true").takeIf { !it.ok }
            ?.let { StepResult("permission", false, it.out) }
            ?: StepResult("permission", AdbKeepAlive.grantSelf(shell))
        steps += permission
        if (!permission.ok) return steps.also { log.add("setup_failed", "no ADB: ${permission.detail}") }
        store.put(PROTECTION, "on")

        val a11y = AccessibilitySetting.enable(shell, OUR_SERVICE)
        steps += StepResult("accessibility", a11y.ok, a11y.out)

        val youtubeGone = !guard.isPresent(YouTubeGuard.YOUTUBE)
        val tizenTube = tizenTubeVersion()
        if (youtubeGone && tizenTube != null && (!isTcl || keys.isMapped())) {
            log.add("adopted", "already set up: YouTube removed, TizenTube $tizenTube" + if (isTcl) ", button mapped" else "")
            steps += StepResult("adopted", true)
        } else {
            steps += when (val r = guard.enforce("setup")) {
                is GuardResult.Failed -> StepResult("youtube", false, r.reason)
                else -> StepResult("youtube", true, r.toString())
            }
            steps += if (tizenTube != null) StepResult("tizentube", true, "already installed ($tizenTube)")
            else when (val r = installTizenTube(abis)) {
                is UpdateResult.Installed -> StepResult("tizentube", true, r.tag)
                else -> StepResult("tizentube", false, r.toString())
            }
            if (isTcl && !keys.isMapped()) steps += StepResult("button", keys.apply())
        }

        schedule()
        steps += StepResult("schedule", true)
        steps.filter { !it.ok }.forEach { log.add("setup_failed", "${it.name}: ${it.detail}") }
        return steps
    }

    companion object {
        const val PROTECTION = "protection"
    }
}
