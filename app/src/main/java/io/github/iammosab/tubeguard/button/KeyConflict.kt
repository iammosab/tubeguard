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
