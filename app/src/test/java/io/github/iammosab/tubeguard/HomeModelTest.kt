package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.ui.PrimaryAction
import io.github.iammosab.tubeguard.ui.Status
import io.github.iammosab.tubeguard.ui.Text
import io.github.iammosab.tubeguard.ui.TileAction
import io.github.iammosab.tubeguard.ui.Tone
import io.github.iammosab.tubeguard.ui.ago
import io.github.iammosab.tubeguard.ui.home
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeModelTest {
    private val now = 1_000_000_000L
    private val good = Status(
        adbConnected = true, protection = "on", youtubePresent = false,
        tizenTube = "2.0.3", tizenTubeLatest = "v2.0.3", isTcl = true, tclMapped = true,
        lastCheck = now - 2 * 60_000, removals = 3,
    )

    @Test
    fun protectedIsGoodWithCheckNow() {
        val m = good.home(now)
        assertEquals(Tone.GOOD, m.tone)
        assertEquals(Text(R.string.hero_protected), m.title)
        assertEquals(PrimaryAction.CHECK_NOW, m.primary)
    }

    @Test
    fun tilesTellTheFourThings() {
        val t = good.home(now).tiles
        assertEquals(Text(R.string.tile_youtube_removed), t[0].value)
        assertEquals(Tone.GOOD, t[0].tone)
        assertEquals(Text(R.string.plain, listOf("2.0.3")), t[1].value)
        assertEquals(Text(R.string.tile_tizentube_current), t[1].support)
        assertEquals(Text(R.string.tile_button_tizentube), t[2].value)
        assertEquals(Text(R.string.ago_minutes, listOf(2)), t[3].value)
    }

    @Test
    fun newerTizenTubeIsShown() =
        assertEquals(
            Text(R.string.tile_tizentube_update, listOf("2.0.4")),
            good.copy(tizenTubeLatest = "v2.0.4").home(now).tiles[1].support,
        )

    @Test
    fun noAdbAsksToFix() {
        val m = good.copy(adbConnected = false, youtubePresent = null).home(now)
        assertEquals(Tone.BAD, m.tone)
        assertEquals(PrimaryAction.FIX, m.primary)
        assertEquals(Text(R.string.tile_unknown), m.tiles[0].value)
        assertEquals(Tone.BAD, m.tiles[0].tone)
    }

    @Test
    fun notSetUpIsAnInvitationNotAnAlarm() {
        val m = good.copy(protection = null, adbConnected = false, youtubePresent = null, lastCheck = null).home(now)
        assertEquals(PrimaryAction.SET_UP, m.primary)
        assertEquals(Tone.NEUTRAL, m.tone)
        assertEquals(Text(R.string.tile_not_checked), m.tiles[0].value)
        assertEquals(Tone.NEUTRAL, m.tiles[0].tone)
        assertEquals(Text(R.string.ago_never), m.tiles[3].value)
    }

    @Test
    fun youtubeBackOffersRemoval() {
        val m = good.copy(youtubePresent = true).home(now)
        assertEquals(PrimaryAction.REMOVE_NOW, m.primary)
        assertEquals(Text(R.string.tile_youtube_installed), m.tiles[0].value)
    }

    @Test
    fun watchdogOffOffersRepair() = assertEquals(PrimaryAction.REPAIR, good.copy(watchdogOk = false).home(now).primary)

    @Test
    fun undoneIsNeutralAndOffersTurningBackOn() {
        val m = good.copy(protection = "off", youtubePresent = true).home(now)
        assertEquals(Tone.NEUTRAL, m.tone)
        assertEquals(PrimaryAction.TURN_BACK_ON, m.primary)
    }

    @Test
    fun undoIncompleteOffersPlayStore() =
        assertEquals(PrimaryAction.PLAY_STORE, good.copy(protection = "off", youtubePresent = false).home(now).primary)

    @Test
    fun unmappedButtonInvitesChoosingOne() {
        val tile = good.copy(isTcl = false, tclMapped = false, takeOver = false).home(now).tiles[2]
        assertEquals(Text(R.string.tile_button_none), tile.value)
        assertEquals(Text(R.string.tile_button_choose), tile.support)
        assertEquals(TileAction.CHOOSE_BUTTON, tile.action)
        assertEquals(Tone.ATTENTION, tile.tone)
    }

    @Test
    fun mappedButtonIsJustStatus() {
        val tile = good.home(now).tiles[2]
        assertEquals(Text(R.string.tile_button_tizentube), tile.value)
        assertEquals(null, tile.action)
    }

    @Test
    fun noButtonInvitationBeforeSetup() =
        assertEquals(null, good.copy(protection = null, isTcl = false, tclMapped = false).home(now).tiles[2].action)

    @Test
    fun agoRanges() {
        assertEquals(Text(R.string.ago_now), ago(now - 20_000, now))
        assertEquals(Text(R.string.ago_minutes, listOf(5)), ago(now - 5 * 60_000, now))
        assertEquals(Text(R.string.ago_hours, listOf(3)), ago(now - 3 * 3_600_000, now))
        assertEquals(Text(R.string.ago_days, listOf(2)), ago(now - 2 * 86_400_000L, now))
        assertEquals(Text(R.string.ago_never), ago(null, now))
    }

    @Test
    fun protectedOnlyMentionsTheButtonWhenItIsMapped() {
        assertEquals(Text(R.string.hero_protected_body), good.home(now).body)
        assertEquals(
            Text(R.string.hero_protected_body_no_button),
            good.copy(isTcl = false, tclMapped = false, takeOver = false).home(now).body,
        )
    }
}
