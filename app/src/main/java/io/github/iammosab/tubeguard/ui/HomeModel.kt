package io.github.iammosab.tubeguard.ui

import androidx.annotation.StringRes
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.update.isNewer

/** A string resource and its format arguments, resolved by the screen. */
data class Text(@StringRes val res: Int, val args: List<Any> = emptyList())

/** ATTENTION: not wrong, but something worth doing (the warm accent, never red). */
enum class Tone { GOOD, BAD, NEUTRAL, ATTENTION }

/** The one big button on the home screen; what it is depends on what needs doing. */
enum class PrimaryAction { CHECK_NOW, SET_UP, FIX, REMOVE_NOW, REPAIR, PLAY_STORE, TURN_BACK_ON }

enum class TileKind { YOUTUBE, TIZENTUBE, BUTTON, LAST_CHECK }

/** What selecting a tile does, for the few tiles that invite an action. */
enum class TileAction { CHOOSE_BUTTON, INSTALL_TIZENTUBE, FIX_BUTTON }

data class Tile(
    val kind: TileKind,
    @StringRes val label: Int,
    val value: Text,
    val tone: Tone,
    val support: Text? = null,
    val action: TileAction? = null,
    /** 0..1 while something runs for this tile (a download), else null. */
    val progress: Float? = null,
)

data class HomeModel(val tone: Tone, val title: Text, val body: Text, val primary: PrimaryAction, val tiles: List<Tile>)

fun Status.home(now: Long): HomeModel {
    val (tone, title, body, primary) = when (headline()) {
        Headline.PROTECTED -> Hero(
            Tone.GOOD, R.string.hero_protected,
            if (buttonMapped) R.string.hero_protected_body else R.string.hero_protected_body_no_button, PrimaryAction.CHECK_NOW,
        )
        Headline.NOT_SET_UP -> Hero(Tone.NEUTRAL, R.string.hero_not_set_up, R.string.hero_not_set_up_body, PrimaryAction.SET_UP)
        Headline.NOT_PROTECTED_NO_ADB -> Hero(Tone.BAD, R.string.hero_not_protected, R.string.hero_no_adb_body, PrimaryAction.FIX)
        Headline.YOUTUBE_PRESENT -> Hero(Tone.BAD, R.string.hero_youtube_back, R.string.hero_youtube_back_body, PrimaryAction.REMOVE_NOW)
        Headline.CHECK_FAILED -> Hero(Tone.BAD, R.string.hero_not_protected, R.string.hero_check_failed_body, PrimaryAction.CHECK_NOW)
        Headline.WATCHDOG_OFF -> Hero(Tone.BAD, R.string.hero_not_protected, R.string.hero_watchdog_body, PrimaryAction.REPAIR)
        Headline.UNDO_INCOMPLETE -> Hero(Tone.BAD, R.string.hero_undo_incomplete, R.string.hero_undo_incomplete_body, PrimaryAction.PLAY_STORE)
        Headline.OFF -> Hero(Tone.NEUTRAL, R.string.hero_off, R.string.hero_off_body, PrimaryAction.TURN_BACK_ON)
    }
    return HomeModel(tone, Text(title), Text(body), primary, tiles(now))
}

private val Status.buttonMapped get() = (isTcl && tclMapped) || takeOver

private data class Hero(val tone: Tone, @StringRes val title: Int, @StringRes val body: Int, val primary: PrimaryAction)

private fun Status.tiles(now: Long): List<Tile> {
    val youtube = if (protection == null) {
        Tile(TileKind.YOUTUBE, R.string.tile_youtube, Text(R.string.tile_not_checked), Tone.NEUTRAL)
    } else when (youtubePresent) {
        false -> Tile(TileKind.YOUTUBE, R.string.tile_youtube, Text(R.string.tile_youtube_removed), if (protection == "on") Tone.GOOD else Tone.NEUTRAL)
        true -> Tile(TileKind.YOUTUBE, R.string.tile_youtube, Text(R.string.tile_youtube_installed), if (protection == "off") Tone.NEUTRAL else Tone.BAD)
        null -> Tile(TileKind.YOUTUBE, R.string.tile_youtube, Text(R.string.tile_unknown), Tone.BAD)
    }
    val latest = tizenTubeLatest?.removePrefix("v")
    val work = tizenTubeWork
    val tizenTube = when {
        work != null -> Tile(
            TileKind.TIZENTUBE, R.string.tile_tizentube,
            if (tizenTube == null) Text(R.string.tile_tizentube_missing) else Text(R.string.plain, listOf(tizenTube)),
            Tone.ATTENTION,
            support = when (work) {
                is io.github.iammosab.tubeguard.update.Progress.Downloading ->
                    Text(R.string.tile_tizentube_downloading, listOf((work.fraction * 100).toInt()))
                io.github.iammosab.tubeguard.update.Progress.Installing -> Text(R.string.tile_tizentube_installing)
            },
            progress = (work as? io.github.iammosab.tubeguard.update.Progress.Downloading)?.fraction ?: 1f,
        )
        tizenTube == null && protection == "on" -> Tile(
            TileKind.TIZENTUBE, R.string.tile_tizentube, Text(R.string.tile_tizentube_missing), Tone.ATTENTION,
            support = Text(R.string.tile_tizentube_install), action = TileAction.INSTALL_TIZENTUBE,
        )
        tizenTube == null -> Tile(TileKind.TIZENTUBE, R.string.tile_tizentube, Text(R.string.tile_tizentube_missing), Tone.NEUTRAL)
        latest != null && isNewer(latest, tizenTube) -> Tile(
            TileKind.TIZENTUBE, R.string.tile_tizentube, Text(R.string.plain, listOf(tizenTube)), Tone.NEUTRAL,
            support = Text(R.string.tile_tizentube_update, listOf(latest)),
        )
        else -> Tile(
            TileKind.TIZENTUBE, R.string.tile_tizentube, Text(R.string.plain, listOf(tizenTube)), Tone.NEUTRAL,
            support = if (latest != null) Text(R.string.tile_tizentube_current) else null,
        )
    }
    val button = when {
        // After Undo TubeGuard manages no button, whatever is left in TCL's table.
        protection == "off" -> Tile(TileKind.BUTTON, R.string.tile_button, Text(R.string.tile_button_not_managed), Tone.NEUTRAL)
        protection == null -> Tile(TileKind.BUTTON, R.string.tile_button, Text(R.string.tile_button_not_mapped), Tone.NEUTRAL)
        // What a real press did beats what the settings say.
        buttonWorked == false -> Tile(
            TileKind.BUTTON, R.string.tile_button, Text(R.string.tile_button_failed), Tone.BAD,
            support = Text(R.string.tile_button_fix), action = TileAction.FIX_BUTTON,
        )
        buttonMapped && keyConflict != null -> Tile(
            TileKind.BUTTON, R.string.tile_button, Text(R.string.tile_button_tizentube), Tone.ATTENTION,
            support = Text(R.string.tile_button_conflict, listOf(keyConflict)),
        )
        buttonMapped -> Tile(TileKind.BUTTON, R.string.tile_button, Text(R.string.tile_button_tizentube), Tone.NEUTRAL)
        // Protected but no button opens TizenTube yet: the tile becomes an invitation to choose one.
        else -> Tile(
            TileKind.BUTTON, R.string.tile_button, Text(R.string.tile_button_none), Tone.ATTENTION,
            support = Text(R.string.tile_button_choose), action = TileAction.CHOOSE_BUTTON,
        )
    }
    val check = Tile(TileKind.LAST_CHECK, R.string.tile_last_check, ago(lastCheck, now), Tone.NEUTRAL)
    return listOf(youtube, tizenTube, button, check)
}

/** "2 min ago" and friends for [time], or "never". */
fun ago(time: Long?, now: Long): Text {
    if (time == null) return Text(R.string.ago_never)
    val minutes = (now - time) / 60_000
    return when {
        minutes < 1 -> Text(R.string.ago_now)
        minutes < 60 -> Text(R.string.ago_minutes, listOf(minutes.toInt()))
        minutes < 24 * 60 -> Text(R.string.ago_hours, listOf((minutes / 60).toInt()))
        else -> Text(R.string.ago_days, listOf((minutes / (24 * 60)).toInt()))
    }
}
