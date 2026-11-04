package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.update.shouldInstallTizenTube
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoInstallTest {
    @Test
    fun missingTizenTubeIsInstalledWhileProtecting() =
        assertTrue(shouldInstallTizenTube(protection = "on", installed = null, removedByOwner = false))

    @Test
    fun notWhenTheOwnerRemovedIt() =
        assertFalse(shouldInstallTizenTube(protection = "on", installed = null, removedByOwner = true))

    @Test
    fun notBeforeSetupOrAfterUndo() {
        assertFalse(shouldInstallTizenTube(protection = null, installed = null, removedByOwner = false))
        assertFalse(shouldInstallTizenTube(protection = "off", installed = null, removedByOwner = false))
    }

    @Test
    fun notWhenItIsThere() = assertFalse(shouldInstallTizenTube(protection = "on", installed = "2.0.3", removedByOwner = false))
}
