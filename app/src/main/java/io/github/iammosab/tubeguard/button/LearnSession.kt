package io.github.iammosab.tubeguard.button

import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.shell.shellQuote
import io.github.iammosab.tubeguard.state.KeyValue
import org.json.JSONArray

/**
 * Learning the owner's YouTube button without anything else reacting to the press. Before listening it pauses
 * every other binding on the remote: TCL's button table (each `CustomKey_<code>` and the list) and the other
 * apps' accessibility services (other launchers or button apps). Afterwards it puts back exactly what was there, and on TCL
 * maps the learned key to TizenTube too. What it paused is saved first, so a session cut short (crash, force-stop,
 * power off) is undone by [restoreIfInterrupted] on the next start.
 */
class LearnSession(private val shell: Shell, private val store: KeyValue, private val keys: TclKeys) {
    /** Pauses the other bindings; false, with nothing changed, if the TV's settings couldn't be read. */
    fun start(): Boolean {
        if (store.get(ACTIVE) != null) restoreIfInterrupted()
        val services = read("secure", A11Y) ?: return false
        val list = read("global", LIST) ?: return false
        val codes = codes(list)
        val entries = mutableMapOf<String, String>()
        for (code in codes) entries[code] = read("global", "CustomKey_$code") ?: return false

        // Saved before anything changes, so it can always be put back.
        store.put(SAVED_A11Y, services)
        store.put(SAVED_LIST, list)
        entries.forEach { (code, value) -> store.put("$SAVED_KEY$code", value) }
        store.put(SAVED_CODES, JSONArray(entries.keys.toList()).toString())
        store.put(ACTIVE, "1")

        shell.run("settings put secure $A11Y ${shellQuote(OUR_SERVICE)}")
        // Key entries before the list, as TCL expects.
        entries.keys.forEach { shell.run("settings delete global CustomKey_$it") }
        if (list != NULL) shell.run("settings delete global $LIST")
        return true
    }

    /** Puts back what [start] paused; with [learned], also points that key at TizenTube on TCL. */
    fun finish(learned: Int?): Boolean {
        if (store.get(ACTIVE) == null) return false
        val list = store.get(SAVED_LIST) ?: NULL
        val codes = codes(store.get(SAVED_CODES) ?: "[]")
        var ok = true
        for (code in codes) {
            val value = store.get("$SAVED_KEY$code") ?: continue
            ok = ok and shell.run("settings put global CustomKey_$code ${shellQuote(value)}").ok
        }
        if (list != NULL) ok = ok and shell.run("settings put global $LIST ${shellQuote(list)}").ok
        if (learned != null && list != NULL) ok = ok and keys.apply(listOf(learned))

        val services = store.get(SAVED_A11Y)
        val restored = AccessibilitySetting.withService(services?.takeUnless { it == NULL }, OUR_SERVICE)
        ok = ok and shell.run("settings put secure $A11Y ${shellQuote(restored)}").ok

        if (ok) {
            codes.forEach { store.put("$SAVED_KEY$it", null) }
            listOf(SAVED_A11Y, SAVED_LIST, SAVED_CODES, ACTIVE).forEach { store.put(it, null) }
        }
        return ok
    }

    /** True when a cut-short session was found and put back. */
    fun restoreIfInterrupted(): Boolean = store.get(ACTIVE) != null && finish(learned = null)

    private fun read(table: String, key: String): String? =
        shell.run("settings get $table $key").takeIf { it.ok }?.out?.trim()?.ifEmpty { NULL }

    private fun codes(json: String): List<String> {
        if (json == NULL) return emptyList()
        val a = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until a.length()).map { a.get(it).toString() }
    }

    private companion object {
        const val A11Y = "enabled_accessibility_services"
        const val LIST = "CustomKeyList"
        const val NULL = "null"
        const val ACTIVE = "learn.active"
        const val SAVED_A11Y = "learn.saved.a11y"
        const val SAVED_LIST = "learn.saved.list"
        const val SAVED_CODES = "learn.saved.codes"
        const val SAVED_KEY = "learn.saved.key."
    }
}
