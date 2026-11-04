package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.state.EventLog
import org.junit.Assert.assertEquals
import org.junit.Test

class EventLogTest {
    @Test
    fun capsAt200() {
        val log = EventLog(MapKeyValue())
        repeat(250) { log.add("k", "e$it", now = it.toLong()) }
        val all = log.recent(1000)
        assertEquals(200, all.size)
        assertEquals("e249", all.first().detail)
        assertEquals("e50", all.last().detail)
    }

    @Test
    fun survivesReload() {
        val store = MapKeyValue()
        EventLog(store).add("removed", "boot", now = 5)
        assertEquals(5L, EventLog(store).recent().single().time)
    }
}
