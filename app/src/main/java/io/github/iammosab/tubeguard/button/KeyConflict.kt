package io.github.iammosab.tubeguard.button

/**
 * Another button app (e.g. a launcher with its own button settings) can react to the same remote key as TubeGuard: its own YouTube-button
 * setting opened TubeGuard 30 ms after TubeGuard opened TizenTube, covering it. A launcher-style start of
 * TubeGuard from another app within [WINDOW_MS] of the TizenTube key is that, not the owner opening TubeGuard.
 */
fun launchedByAnotherButtonApp(now: Long, lastKeyAt: Long?, referrer: String?, launcherIntent: Boolean): Boolean =
    launcherIntent && lastKeyAt != null && now - lastKeyAt in 0..WINDOW_MS &&
        referrer != null && referrer != OWN_PACKAGE

private const val WINDOW_MS = 2_000L
private const val OWN_PACKAGE = "io.github.iammosab.tubeguard"

/**
 * Right after the TizenTube button, another app's screen coming up is another binding on the same key (Free My
 * TV opened TubeGuard 13 ms after TizenTube): TubeGuard brings TizenTube back on top. Only within [PULL_MS] of
 * the press, at most [MAX_PULLS] times, and never over TizenTube itself or the system's own windows.
 */
fun shouldPullTizenTubeBack(now: Long, lastKeyAt: Long?, shown: String, pulls: Int): Boolean =
    lastKeyAt != null && now - lastKeyAt in 0..PULL_MS && pulls < MAX_PULLS && shown !in LEAVE_ALONE

private const val PULL_MS = 1_500L
private const val MAX_PULLS = 2
private val LEAVE_ALONE = setOf("io.gh.reisxd.tizentube.cobalt", "com.android.systemui", "android")
