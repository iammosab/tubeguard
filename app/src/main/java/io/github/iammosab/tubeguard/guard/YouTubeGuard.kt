package io.github.iammosab.tubeguard.guard

import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.KeyValue

sealed class GuardResult {
    object Absent : GuardResult()
    /** [how]: what pm said installed it, e.g. "installer=com.android.vending". */
    data class Removed(val how: String) : GuardResult()
    data class Failed(val reason: String) : GuardResult()
}

/** Keeps the official YouTube app removed for user 0. */
class YouTubeGuard(
    private val shell: Shell,
    private val log: EventLog,
    private val packages: List<String> = listOf(YOUTUBE),
    private val store: KeyValue? = null,
) {
    fun isPresent(pkg: String): Boolean = listing(pkg)?.let { it != NOT_LISTED } == true

    /** Removes every guarded package that is back; [trigger] says why we looked (boot, package_added, ...). */
    fun enforce(trigger: String): GuardResult {
        if (store?.get("protection") == "off") return GuardResult.Absent
        var result: GuardResult = GuardResult.Absent
        for (pkg in packages) {
            val how = listing(pkg)
            if (how == null) {
                result = GuardResult.Failed("can't list packages")
                continue
            }
            if (how == NOT_LISTED) continue
            val out = shell.run("pm uninstall -k --user 0 $pkg")
            result = if (out.ok && out.out.contains("Success")) {
                log.add("removed", "$trigger: $pkg $how".trim())
                GuardResult.Removed(how)
            } else {
                log.add("remove_failed", "$trigger: $pkg: ${out.out}")
                GuardResult.Failed(out.out)
            }
        }
        return result
    }

    /** The "installer=..." part of pm's line for exactly [pkg], [NOT_LISTED], or null when pm couldn't be asked. */
    private fun listing(pkg: String): String? {
        val out = shell.run("pm list packages -i --user 0 $pkg")
        if (!out.ok) return null
        val line = out.out.lineSequence().map { it.trim() }
            .firstOrNull { it == "package:$pkg" || it.startsWith("package:$pkg ") } ?: return NOT_LISTED
        return line.removePrefix("package:$pkg").trim()
    }

    companion object {
        const val YOUTUBE = "com.google.android.youtube.tv"
        private const val NOT_LISTED = "\u0000"
    }
}
