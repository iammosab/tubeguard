package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.button.launchedByAnotherButtonApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyConflictTest {
    @Test
    fun launchRightAfterTheKeyFromAnotherAppIsTheButtonConflict() {
        assertTrue(launchedByAnotherButtonApp(now = 10_030, lastKeyAt = 10_000, referrer = "com.example.launcher", launcherIntent = true))
    }

    @Test
    fun openingTubeGuardYourselfIsNotAConflict() {
        // Long after the key, or no key at all.
        assertFalse(launchedByAnotherButtonApp(now = 20_000, lastKeyAt = 10_000, referrer = "com.example.launcher", launcherIntent = true))
        assertFalse(launchedByAnotherButtonApp(now = 10_030, lastKeyAt = null, referrer = "com.example.launcher", launcherIntent = true))
        // TubeGuard opening its own screen (settings, setup) right after a key.
        assertFalse(launchedByAnotherButtonApp(now = 10_030, lastKeyAt = 10_000, referrer = "io.github.iammosab.tubeguard", launcherIntent = true))
        // Not the launcher intent another app sends.
        assertFalse(launchedByAnotherButtonApp(now = 10_030, lastKeyAt = 10_000, referrer = "com.example.launcher", launcherIntent = false))
    }

    @Test
    fun theTizenTubeKeyIsKnownEvenWithTakeOverOff() {
        val p = KeyPolicy(MapKeyValue())
        assertTrue(p.isTizenTubeKey(190))
        assertTrue(p.isTizenTubeKey(4065))
        assertFalse(p.isTizenTubeKey(20))
        assertTrue(KeyPolicy(MapKeyValue("learnedKeyCode" to "300")).isTizenTubeKey(300))
    }
}
