package io.github.iammosab.tubeguard.state

import org.json.JSONArray
import org.json.JSONObject

data class Event(val time: Long, val kind: String, val detail: String)

/** What the app did and saw, newest first, the last [CAP] events. */
class EventLog(private val store: KeyValue) {
    @Synchronized
    fun add(kind: String, detail: String, now: Long = System.currentTimeMillis()) {
        val events = listOf(Event(now, kind, detail)) + recent(CAP - 1)
        val json = JSONArray()
        events.forEach { json.put(JSONObject().put("t", it.time).put("k", it.kind).put("d", it.detail)) }
        store.put(KEY, json.toString())
    }

    @Synchronized
    fun recent(n: Int = 50): List<Event> {
        val json = runCatching { JSONArray(store.get(KEY) ?: "[]") }.getOrElse { JSONArray() }
        return (0 until minOf(n, json.length())).map {
            val o = json.getJSONObject(it)
            Event(o.getLong("t"), o.getString("k"), o.getString("d"))
        }
    }

    companion object {
        const val CAP = 200
        private const val KEY = "log"
    }
}
