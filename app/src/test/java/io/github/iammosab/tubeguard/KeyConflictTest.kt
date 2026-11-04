package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.button.launchedByAnotherButtonApp
import io.github.iammosab.tubeguard.button.shouldPullTizenTubeBack
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

class PullBackTest {
    private val tizen = "io.gh.reisxd.tizentube.cobalt"

    @Test
    fun anotherAppRightAfterTheKeyIsPulledBack() {
        assertTrue(shouldPullTizenTubeBack(now = 1_200, lastKeyAt = 1_000, shown = "io.github.iammosab.tubeguard", pulls = 0))
        assertTrue(shouldPullTizenTubeBack(now = 1_200, lastKeyAt = 1_000, shown = "com.netflix.ninja", pulls = 1))
    }

    @Test
    fun leavesTizenTubeTheSystemAndLaterChoicesAlone() {
        val f = ::shouldPullTizenTubeBack
        assertFalse(f(1_200, 1_000, tizen, 0))
        assertFalse(f(1_200, 1_000, "com.android.systemui", 0))
        assertFalse("after the window it's the owner's own move", f(3_000, 1_000, "com.example.launcher", 0))
        assertFalse("never more than twice", f(1_200, 1_000, "com.example.launcher", 2))
        assertFalse("no key, no pull", f(1_200, null, "com.example.launcher", 0))
    }
}
