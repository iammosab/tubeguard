package io.github.iammosab.tubeguard.update

import org.json.JSONObject

data class Asset(val name: String, val url: String, val size: Long, val sha256: String?)

data class Release(val tag: String, val assets: List<Asset>)

/** A GitHub "latest release" response; [Asset.sha256] comes from GitHub's `digest` field when it has one. */
fun parseRelease(json: String): Release {
    val o = JSONObject(json)
    val assets = o.optJSONArray("assets")
    return Release(
        o.getString("tag_name"),
        (0 until (assets?.length() ?: 0)).map {
            val a = assets!!.getJSONObject(it)
            Asset(
                a.getString("name"),
                a.getString("browser_download_url"),
                a.optLong("size"),
                a.optString("digest").takeIf { d -> d.startsWith("sha256:") }?.removePrefix("sha256:"),
            )
        },
    )
}

/**
 * The 64-bit build only when the OS runs 64-bit apps: TVs like TCL's have 64-bit CPUs on a 32-bit-only system,
 * and those list just armeabi-v7a.
 */
fun pickTizenTube(release: Release, abis: List<String>): Asset? {
    val name = if ("arm64-v8a" in abis) "cobalt-arm64.apk" else "cobalt-arm.apk"
    return release.assets.firstOrNull { it.name == name }
}

/** Whether release [tag] (e.g. "v2.0.10") is newer than installed versionName [installed]; null means not installed. */
fun isNewer(tag: String, installed: String?): Boolean {
    if (installed == null) return true
    val a = numbers(tag)
    val b = numbers(installed)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

private fun numbers(version: String) =
    version.trim().removePrefix("v").split('.', '-', '+').map { it.takeWhile(Char::isDigit) }.takeWhile { it.isNotEmpty() }
        .map { it.toInt() }
