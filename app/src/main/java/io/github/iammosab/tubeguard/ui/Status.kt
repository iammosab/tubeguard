package io.github.iammosab.tubeguard.ui

import io.github.iammosab.tubeguard.state.Event

enum class Headline { PROTECTED, NOT_SET_UP, NOT_PROTECTED_NO_ADB, YOUTUBE_PRESENT, OFF }

/** Everything the screen shows, gathered on the background thread. */
data class Status(
    val adbConnected: Boolean,
    /** "on" after setup, "off" after Undo, null before setup. */
    val protection: String?,
    /** null when it couldn't be checked (no ADB). */
    val youtubePresent: Boolean?,
    val tizenTube: String? = null,
    val tizenTubeLatest: String? = null,
    val signatureMismatch: Boolean = false,
    val isTcl: Boolean = false,
    val tclMapped: Boolean = false,
    val takeOver: Boolean = false,
    val learnedKey: String? = null,
    val otherKeyServices: List<String> = emptyList(),
    val keepAdbOn: Boolean = true,
    val lastCheck: Long? = null,
    val removals: Int = 0,
    val appVersion: String? = null,
    val appLatest: String? = null,
    val log: List<Event> = emptyList(),
)

fun Status.headline(): Headline = when {
    protection == "off" -> Headline.OFF
    youtubePresent == true -> Headline.YOUTUBE_PRESENT
    !adbConnected -> Headline.NOT_PROTECTED_NO_ADB
    protection == null -> Headline.NOT_SET_UP
    else -> Headline.PROTECTED
}
