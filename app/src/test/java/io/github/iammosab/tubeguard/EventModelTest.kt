package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.state.Event
import io.github.iammosab.tubeguard.ui.Tone
import io.github.iammosab.tubeguard.ui.describe
import org.junit.Assert.assertEquals
import org.junit.Test

class EventModelTest {
    @Test
    fun removalIsGoodNews() {
        val d = Event(0, "removed", "package_added: com.google.android.youtube.tv installer=com.android.vending").describe()
        assertEquals(R.string.event_removed, d.title)
        assertEquals(Tone.GOOD, d.tone)
    }

    @Test
    fun failuresAreBad() {
        listOf("remove_failed", "update_failed", "setup_failed", "undo_failed", "update_blocked").forEach {
            assertEquals(it, Tone.BAD, Event(0, it, "x").describe().tone)
        }
    }

    @Test
    fun unknownKindsStillShow() {
        val d = Event(0, "something_new", "detail").describe()
        assertEquals(R.string.event_other, d.title)
        assertEquals(Tone.NEUTRAL, d.tone)
    }

    @Test
    fun youtubeRemovalSaysWhoBroughtItBack() =
        assertEquals("Play Store", Event(0, "removed", "periodic: com.google.android.youtube.tv installer=com.android.vending").describe().source)

    @Test
    fun updateDetailsUseTheAppsNames() {
        assertEquals("TubeGuard: release check: HTTP 404", Event(0, "update_failed", "SELF: release check: HTTP 404").describe().detail)
        assertEquals("TizenTube: 2.0.3 → v2.0.4", Event(0, "updated", "TIZENTUBE: 2.0.3 → v2.0.4").describe().detail)
    }
}
