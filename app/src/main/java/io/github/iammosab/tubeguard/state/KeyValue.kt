package io.github.iammosab.tubeguard.state

import android.content.Context

/** The app's small persistent store: settings, the event log, values recorded for Undo. */
interface KeyValue {
    fun get(k: String): String?

    /** null removes the key. */
    fun put(k: String, v: String?)
}

class PrefsKeyValue(context: Context) : KeyValue {
    private val prefs = context.getSharedPreferences("tubeguard", Context.MODE_PRIVATE)
    override fun get(k: String): String? = prefs.getString(k, null)
    override fun put(k: String, v: String?) {
        prefs.edit().apply { if (v == null) remove(k) else putString(k, v) }.apply()
    }
}
