package io.github.iammosab.tubeguard.button

import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.state.KeyValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * TCL's own remote-button table (global settings `CustomKeyList` and `CustomKey_<code>`): the YouTube button opens
 * TizenTube on key-down and sends a broadcast nobody handles on key-up. Without the key-up entry, Google TV's
 * launcher still sees the release and opens YouTube's Play Store page. The value each key had before is recorded
 * once (`orig.CustomKey_<code>`), so Undo can put back TCL's own and leave every other key alone.
 */
class TclKeys(private val shell: Shell, private val store: KeyValue) {
    /** When the settings can't be read the brand is unknown, and counts as TCL: our keys are harmless elsewhere. */
    fun isTcl(): Boolean = try {
        get(LIST) != null || shell.run("getprop ro.product.manufacturer").out.contains("TCL", ignoreCase = true)
    } catch (e: ReadFailed) {
        true
    }

    fun entry(code: Int): String {
        val missing = JSONObject().put("type", 3).put("intent", "intent:#Intent;action=$PKG.KEY_TARGET_MISSING;end")
        val down = JSONObject().put("keyaction", 1).put("type", 1).put(
            "intent",
            "intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LEANBACK_LAUNCHER;" +
                "launchFlags=0x10200000;package=$PKG;component=$PKG/dev.cobalt.app.MainActivity;end",
        ).put("appnotinstall", missing)
        val up = JSONObject().put("keyaction", 3).put("type", 3)
            .put("intent", "intent:#Intent;action=$PKG.KEY_RELEASE_NOOP;package=$PKG;end")
            .put("appnotinstall", JSONObject(missing.toString()))
        return JSONObject().put("keycode", code).put("behavior", JSONArray().put(down).put(up)).toString()
    }

    /** Every default YouTube code is listed and opens TizenTube, however it got there. */
    fun isMapped(codes: List<Int> = CODES): Boolean = try {
        val list = list()
        list != null && codes.all { it.toString() in list && isOurs(get("CustomKey_$it")) }
    } catch (e: ReadFailed) {
        false
    }

    /**
     * Points [codes] at TizenTube; false, with nothing written, if TCL's list isn't something we understand or
     * can't be read. Every value is read before anything is written, so a failed read changes nothing.
     */
    fun apply(codes: List<Int> = CODES): Boolean {
        val list: List<String>
        val current: Map<Int, String?>
        try {
            list = list() ?: return false
            current = codes.associateWith { get("CustomKey_$it") }
        } catch (e: ReadFailed) {
            return false
        }
        for (code in codes) {
            val key = "CustomKey_$code"
            val value = current[code]
            if (store.get("orig.$key") == null) {
                store.put("orig.$key", if (value == null) ABSENT else if (isOurs(value)) OURS else value)
            }
            if (!shell.run("settings put global $key '${entry(code)}'").ok) return false
        }
        val merged = list + codes.map { it.toString() }.filter { it !in list }
        // TCL keeps the table in memory and rereads it only when the list changes: empty it, then set it.
        shell.run("settings put global $LIST '[]'")
        return shell.run("settings put global $LIST '${JSONArray(merged)}'").ok
    }

    /** Removes our keys (key entries before the list) and puts back any TCL value recorded before us. */
    fun undo(codes: List<Int> = CODES): Boolean {
        val list = try {
            list() ?: return false
        } catch (e: ReadFailed) {
            return false
        }
        var ok = true
        val keep = list.toMutableList()
        for (code in codes) {
            val key = "CustomKey_$code"
            val orig = store.get("orig.$key")
            val restore = orig != null && orig != ABSENT && orig != OURS
            ok = ok and if (restore) shell.run("settings put global $key ${quote(orig!!)}").ok
            else shell.run("settings delete global $key").ok
            if (!restore) keep.remove(code.toString())
        }
        ok = ok and if (keep.isEmpty()) shell.run("settings delete global $LIST").ok
        else shell.run("settings put global $LIST '${JSONArray(keep)}'").ok
        if (ok) codes.forEach { store.put("orig.CustomKey_$it", null) }
        return ok
    }

    private fun isOurs(value: String?): Boolean {
        val behavior = runCatching { JSONObject(value ?: return false).getJSONArray("behavior") }.getOrNull()
            ?: return false
        return (0 until behavior.length()).any {
            behavior.getJSONObject(it).optString("intent").contains("component=$PKG/")
        }
    }

    /** The codes in `CustomKeyList` (empty when unset), or null when it holds something that isn't a JSON list. */
    private fun list(): List<String>? {
        val raw = get(LIST) ?: return emptyList()
        val a = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        return (0 until a.length()).map { a.get(it).toString() }
    }

    /** The value, or null when unset; throws [ReadFailed] when the command itself failed (e.g. ADB dropped). */
    private fun get(key: String): String? {
        val out = shell.run("settings get global $key")
        if (!out.ok) throw ReadFailed()
        return out.out.trim().takeUnless { it.isEmpty() || it == "null" }
    }

    private class ReadFailed : Exception()

    /** Single-quoted for the shell, whatever the value holds. */
    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"

    companion object {
        const val PKG = "io.gh.reisxd.tizentube.cobalt"
        const val LIST = "CustomKeyList"
        const val ABSENT = "absent"
        const val OURS = "ours"
        /** TCL's YouTube key, and KEYCODE_BUTTON_3, which TCL remotes send for it. */
        val CODES = listOf(4065, 190)
    }
}
