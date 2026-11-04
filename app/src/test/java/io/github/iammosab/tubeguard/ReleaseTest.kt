package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.update.isNewer
import io.github.iammosab.tubeguard.update.parseRelease
import io.github.iammosab.tubeguard.update.pickTizenTube
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseTest {
    private val release = parseRelease(javaClass.getResource("/tizentube-v2.0.3.json")!!.readText())

    @Test
    fun parsesTagAndAssets() {
        assertEquals("v2.0.3", release.tag)
        val arm = release.assets.first { it.name == "cobalt-arm.apk" }
        assertEquals(80570825L, arm.size)
        assertEquals("72a3527f520db3a1b414916ad96481244e451c41e272ae33344ae5e79abb37dd", arm.sha256)
        assertEquals("https://github.com/reisxd/TizenTubeCobalt/releases/download/v2.0.3/cobalt-arm.apk", arm.url)
    }

    @Test
    fun picksArmOn32BitOnly() =
        assertEquals("cobalt-arm.apk", pickTizenTube(release, listOf("armeabi-v7a", "armeabi"))!!.name)

    @Test
    fun picksArm64WhenSupported() =
        assertEquals("cobalt-arm64.apk", pickTizenTube(release, listOf("arm64-v8a", "armeabi-v7a"))!!.name)

    @Test
    fun isNewerCompares() {
        assertTrue(isNewer("v2.0.4", "2.0.3"))
        assertFalse(isNewer("v2.0.3", "2.0.3"))
        assertTrue(isNewer("v2.0.10", "2.0.9"))
        assertFalse(isNewer("v2.0.2", "2.0.3"))
        assertTrue(isNewer("v2.0.3", null))
    }

    @Test
    fun missingDigestIsNull() {
        val r = parseRelease("""{"tag_name":"v1","assets":[{"name":"a.apk","size":1,"browser_download_url":"u"}]}""")
        assertEquals(null, r.assets.single().sha256)
    }
}
