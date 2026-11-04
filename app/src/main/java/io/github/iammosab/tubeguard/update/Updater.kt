package io.github.iammosab.tubeguard.update

import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.KeyValue
import java.io.File
import java.security.MessageDigest

enum class Target(val repo: String, val pkg: String) {
    TIZENTUBE("reisxd/TizenTubeCobalt", "io.gh.reisxd.tizentube.cobalt"),
    SELF("iammosab/tubeguard", "io.github.iammosab.tubeguard"),
}

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Installed(val tag: String) : UpdateResult()
    data class Deferred(val reason: String) : UpdateResult()
    /** The installed app is signed by someone else; updating would mean uninstalling it (and its sign-in) first. */
    object SignatureMismatch : UpdateResult()
    data class Failed(val reason: String) : UpdateResult()
}

/** Installs the latest GitHub release of a [Target] over ADB when it is newer than what is installed. */
class Updater(
    private val shell: Shell,
    private val http: Http,
    private val log: EventLog,
    private val cacheDir: File,
    private val store: KeyValue? = null,
) {
    /** [replace]: uninstall the installed copy (signing the owner out) once the new one is downloaded and checked. */
    @Synchronized
    fun update(
        target: Target,
        installedVersion: String?,
        abis: List<String>,
        force: Boolean = false,
        replace: Boolean = false,
        progress: (Progress) -> Unit = {},
    ): UpdateResult {
        val page = http.get("https://api.github.com/repos/${target.repo}/releases/latest")
        if (page.code != 200) return fail(target, "release check: HTTP ${page.code}")
        val release = runCatching { parseRelease(page.body) }.getOrElse { return fail(target, "release check: $it") }
        store?.put("latest.${target.name}", release.tag)
        if (!isNewer(release.tag, installedVersion)) return UpdateResult.UpToDate.also { store?.put(lastError(target), null) }
        val asset = when (target) {
            Target.TIZENTUBE -> pickTizenTube(release, abis)
            Target.SELF -> release.assets.firstOrNull { it.name == SELF_ASSET }
        } ?: return fail(target, "${release.tag} has no APK for this TV")
        if (target == Target.TIZENTUBE && !force && isResumed(target.pkg)) {
            return UpdateResult.Deferred("TizenTube is on screen")
        }

        val file = File(cacheDir, asset.name)
        try {
            var lastPercent = -1
            val got = http.download(asset.url, file) { bytes ->
                val percent = if (asset.size > 0) (bytes * 100 / asset.size).toInt().coerceIn(0, 100) else 0
                if (percent != lastPercent) { lastPercent = percent; progress(Progress.Downloading(percent / 100f)) }
            }
            if (got.code != 200) return fail(target, "download: HTTP ${got.code}")
            if (asset.sha256 != null && sha256(file) != asset.sha256.lowercase()) {
                return fail(target, "download of ${asset.name} is corrupt (checksum)")
            }
            progress(Progress.Installing)
            if (replace) shell.run("pm uninstall ${target.pkg}")
            var out = shell.install(file)
            if (out.out.contains("INSUFFICIENT_STORAGE")) {
                shell.run("pm trim-caches 999G")
                out = shell.install(file)
            }
            return when {
                out.out.contains("UPDATE_INCOMPATIBLE") -> {
                    log.add("update_blocked", "${target.name}: installed copy is signed differently")
                    UpdateResult.SignatureMismatch
                }
                out.ok -> {
                    store?.put(lastError(target), null)
                    log.add("updated", "${target.name}: ${installedVersion ?: "none"} → ${release.tag}")
                    UpdateResult.Installed(release.tag)
                }
                else -> fail(target, "install: ${out.out}")
            }
        } finally {
            file.delete()
        }
    }

    /** Whether [pkg] is on screen; when that can't be told, it counts as on screen, so nobody's video is cut off. */
    private fun isResumed(pkg: String): Boolean {
        val out = shell.run("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'")
        return !out.ok || out.out.contains("$pkg/")
    }

    /** Logs a failure only when it differs from the last one, so a TV offline for days doesn't flood the log. */
    private fun fail(target: Target, reason: String): UpdateResult {
        if (store == null || store.get(lastError(target)) != reason) log.add("update_failed", "${target.name}: $reason")
        store?.put(lastError(target), reason)
        return UpdateResult.Failed(reason)
    }

    private fun lastError(target: Target) = "lastUpdateError.${target.name}"

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val SELF_ASSET = "tubeguard.apk"
    }
}
