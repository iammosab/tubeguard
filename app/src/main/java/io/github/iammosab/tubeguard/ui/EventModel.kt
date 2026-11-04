package io.github.iammosab.tubeguard.ui

import androidx.annotation.StringRes
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.state.Event

data class EventView(@StringRes val title: Int, val tone: Tone, val detail: String, val source: String? = null)

/** What a log event means in plain words; [EventView.source] names what brought YouTube back, when known. */
fun Event.describe(): EventView {
    val (title, tone) = when (kind) {
        "removed" -> R.string.event_removed to Tone.GOOD
        "remove_failed" -> R.string.event_remove_failed to Tone.BAD
        "updated" -> R.string.event_updated to Tone.GOOD
        "update_failed" -> R.string.event_update_failed to Tone.BAD
        "update_blocked" -> R.string.event_update_blocked to Tone.BAD
        "adopted" -> R.string.event_adopted to Tone.GOOD
        "setup_failed" -> R.string.event_setup_failed to Tone.BAD
        "undone" -> R.string.event_undone to Tone.NEUTRAL
        "undo_failed" -> R.string.event_undo_failed to Tone.BAD
        "watchdog_restored" -> R.string.event_watchdog_restored to Tone.GOOD
        "button_learned" -> R.string.event_button_learned to Tone.NEUTRAL
        "tizentube_reinstall" -> R.string.event_reinstall to Tone.NEUTRAL
        else -> R.string.event_other to Tone.NEUTRAL
    }
    val installer = Regex("installer=(\\S+)").find(detail)?.groupValues?.get(1)
    val source = when (installer) {
        null, "null" -> null
        "com.android.vending" -> "Play Store"
        else -> installer
    }
    val readable = detail.replace(Regex("^SELF:"), "TubeGuard:").replace(Regex("^TIZENTUBE:"), "TizenTube:")
    return EventView(title, tone, readable, source)
}
