package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.flow.Setup
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.ShellResult
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.update.UpdateResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupTest {
    private val store = MapKeyValue()
    private val log = EventLog(store)
    private var scheduled = 0
    private val installs = mutableListOf<List<String>>()
    private val ours = javaClass.getResource("/office-tv-customkey-4065.json")!!.readText().trim()

    private fun setup(shell: FakeShell, tizenTube: String?) = Setup(
        shell = shell,
        guard = YouTubeGuard(shell, log, store = store),
        keys = TclKeys(shell, store),
        log = log,
        store = store,
        tizenTubeVersion = { tizenTube },
        installTizenTube = { abis -> installs += abis; UpdateResult.Installed("v2.0.3") },
        schedule = { scheduled++ },
    )

    @Test
    fun freshTvRunsAllSteps() {
        val shell = FakeShell(
            Regex("^pm list packages") to ShellResult(0, "package:com.google.android.youtube.tv  installer=null"),
            Regex("^pm uninstall") to ShellResult(0, "Success"),
            Regex("^settings get") to ShellResult(0, "null"),
        )
        val steps = setup(shell, tizenTube = null).run(listOf("armeabi-v7a"), isTcl = true)
        assertEquals(
            listOf("permission", "accessibility", "youtube", "tizentube", "button", "schedule"),
            steps.map { it.name },
        )
        assertTrue(steps.joinToString { it.toString() }, steps.all { it.ok })
        assertTrue("pm grant io.github.iammosab.tubeguard android.permission.WRITE_SECURE_SETTINGS" in shell.commands)
        // TCL drops boot and update broadcasts for apps without its auto-start permission.
        assertTrue("appops set io.github.iammosab.tubeguard AUTO_START allow" in shell.commands)
        assertTrue("pm uninstall -k --user 0 com.google.android.youtube.tv" in shell.commands)
        assertEquals(listOf(listOf("armeabi-v7a")), installs)
        assertTrue(shell.commands.any { it.startsWith("settings put global CustomKeyList") })
        assertTrue(shell.commands.any { it.startsWith("settings put secure enabled_accessibility_services") && OUR_SERVICE in it })
        assertEquals(1, scheduled)
        assertEquals("on", store.get("protection"))
    }

    @Test
    fun handSetUpTvIsAdopted() {
        val office190 = ours.replace("\"keycode\":4065", "\"keycode\":190")
        val shell = FakeShell(
            Regex("^pm list packages") to ShellResult(0, ""),
            Regex("^settings get global CustomKeyList") to ShellResult(0, "[\"4065\",\"190\"]"),
            Regex("^settings get global CustomKey_4065") to ShellResult(0, ours),
            Regex("^settings get global CustomKey_190") to ShellResult(0, office190),
            Regex("^settings get secure") to ShellResult(0, "com.example.launcher/com.example.launcher.ButtonService"),
        )
        val steps = setup(shell, tizenTube = "2.0.3").run(listOf("armeabi-v7a"), isTcl = true)
        assertEquals(listOf("permission", "accessibility", "adopted", "schedule"), steps.map { it.name })
        assertTrue(shell.commands.none { it.startsWith("pm uninstall") || it.startsWith("install") })
        assertTrue(shell.commands.none { it.startsWith("settings put global") })
        assertTrue(installs.isEmpty())
        assertTrue("appops set io.github.iammosab.tubeguard AUTO_START allow" in shell.commands)
        assertEquals("adopted", log.recent().first().kind)
        // The other app's service is kept next to ours.
        assertTrue(shell.commands.contains("settings put secure enabled_accessibility_services 'com.example.launcher/com.example.launcher.ButtonService:$OUR_SERVICE'"))
    }

    @Test
    fun nonTclSkipsButtonTable() {
        val shell = FakeShell(Regex("^pm list packages") to ShellResult(0, ""), Regex("^settings get") to ShellResult(0, "null"))
        val steps = setup(shell, tizenTube = null).run(listOf("arm64-v8a"), isTcl = false)
        assertTrue(steps.none { it.name == "button" })
        assertTrue(shell.commands.none { it.contains("CustomKey") })
    }

    @Test
    fun noAdbFailsFirstStepAndStops() {
        val shell = FakeShell(Regex(".") to ShellResult(-100, "ADB not connected"))
        val steps = setup(shell, tizenTube = null).run(listOf("arm64-v8a"), isTcl = false)
        assertEquals(1, steps.size)
        assertEquals(false, steps.single().ok)
        assertEquals(null, store.get("protection"))
    }

    @Test
    fun cantTellIfYoutubeIsThereIsNotAdoption() {
        val shell = FakeShell(
            Regex("^pm list packages") to ShellResult(1, "Can't find service: package"),
            Regex("^settings get") to ShellResult(0, "null"),
        )
        val steps = setup(shell, tizenTube = "2.0.3").run(listOf("armeabi-v7a"), isTcl = false)
        assertTrue(steps.none { it.name == "adopted" })
        assertEquals(false, steps.first { it.name == "youtube" }.ok)
    }
}
