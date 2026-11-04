package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.shell.ShellResult
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.update.Http
import io.github.iammosab.tubeguard.update.HttpResult
import io.github.iammosab.tubeguard.update.Target
import io.github.iammosab.tubeguard.update.UpdateResult
import io.github.iammosab.tubeguard.update.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class UpdaterTest {
    private val apk = "apk-bytes".toByteArray()
    private val good = sha256(apk)
    private val api = "https://api.github.com/repos/reisxd/TizenTubeCobalt/releases/latest"
    private val armUrl = "https://example/cobalt-arm.apk"
    private val abis = listOf("armeabi-v7a")
    private val log = EventLog(MapKeyValue())
    private val cache: File = Files.createTempDirectory("tg").toFile()

    private fun release(tag: String = "v2.0.4", digest: String? = good) = """
        {"tag_name":"$tag","assets":[
          {"name":"cobalt-arm.apk","size":${apk.size},"browser_download_url":"$armUrl"${digest?.let { ",\"digest\":\"sha256:$it\"" } ?: ""}},
          {"name":"cobalt-arm64.apk","size":1,"browser_download_url":"https://example/cobalt-arm64.apk"}]}
    """.trimIndent()

    private class FakeHttp(val pages: Map<String, HttpResult>, val files: Map<String, ByteArray>) : Http {
        val downloads = mutableListOf<String>()
        override fun get(url: String) = pages[url] ?: HttpResult(404)
        override fun download(url: String, to: File): HttpResult {
            downloads += url
            val bytes = files[url] ?: return HttpResult(404)
            to.writeBytes(bytes)
            return HttpResult(200)
        }
    }

    private fun http(json: String = release(), code: Int = 200) =
        FakeHttp(mapOf(api to HttpResult(code, json)), mapOf(armUrl to apk))

    private fun updater(shell: FakeShell, http: Http = http()) = Updater(shell, http, log, cache)

    private val notResumed = Regex("^dumpsys activity") to ShellResult(0, "  topResumedActivity=ActivityRecord{1 u0 com.example.launcher/.Home t9}")

    @Test
    fun upToDateDoesNothing() {
        val shell = FakeShell(notResumed)
        val h = http(release(tag = "v2.0.3"))
        assertEquals(UpdateResult.UpToDate, updater(shell, h).update(Target.TIZENTUBE, "2.0.3", abis))
        assertTrue(h.downloads.isEmpty())
        assertTrue(shell.commands.none { it.startsWith("install") })
    }

    @Test
    fun defersWhileTizenTubeResumed() {
        val shell = FakeShell(
            Regex("^dumpsys activity") to ShellResult(0, "  topResumedActivity=ActivityRecord{2 u0 io.gh.reisxd.tizentube.cobalt/dev.cobalt.app.MainActivity t12}"),
        )
        assertTrue(updater(shell).update(Target.TIZENTUBE, "2.0.3", abis) is UpdateResult.Deferred)
        assertTrue(shell.commands.none { it.startsWith("install") })
    }

    @Test
    fun forceInstallsEvenWhileResumed() {
        val shell = FakeShell(
            Regex("^dumpsys activity") to ShellResult(0, "topResumedActivity=ActivityRecord{2 u0 io.gh.reisxd.tizentube.cobalt/x}"),
            Regex("^install") to ShellResult(0, "Success"),
        )
        assertEquals(UpdateResult.Installed("v2.0.4"), updater(shell).update(Target.TIZENTUBE, "2.0.3", abis, force = true))
    }

    @Test
    fun installsAndLogs() {
        val shell = FakeShell(notResumed, Regex("^install") to ShellResult(0, "Success"))
        assertEquals(UpdateResult.Installed("v2.0.4"), updater(shell).update(Target.TIZENTUBE, "2.0.3", abis))
        assertTrue("install cobalt-arm.apk" in shell.commands)
        assertEquals("updated", log.recent().first().kind)
        assertTrue("download is cleaned up", cache.listFiles()!!.isEmpty())
    }

    @Test
    fun trimsCachesOnStorageFailureThenRetriesOnce() {
        var installs = 0
        val shell = object : io.github.iammosab.tubeguard.shell.Shell {
            val commands = mutableListOf<String>()
            override fun run(cmd: String): ShellResult { commands += cmd; return notResumed.second }
            override fun install(apk: File): ShellResult {
                commands += "install ${apk.name}"
                return if (++installs == 1) ShellResult(1, "Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]") else ShellResult(0, "Success")
            }
        }
        val result = Updater(shell, http(), log, cache).update(Target.TIZENTUBE, "2.0.3", abis)
        assertEquals(UpdateResult.Installed("v2.0.4"), result)
        assertEquals(
            listOf("install cobalt-arm.apk", "pm trim-caches 999G", "install cobalt-arm.apk"),
            shell.commands.filter { !it.startsWith("dumpsys") },
        )
    }

    @Test
    fun signatureMismatchStops() {
        val shell = FakeShell(notResumed, Regex("^install") to ShellResult(1, "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match]"))
        assertEquals(UpdateResult.SignatureMismatch, updater(shell).update(Target.TIZENTUBE, "2.0.3", abis))
        assertTrue(shell.commands.none { it.startsWith("pm uninstall") })
    }

    @Test
    fun sha256MismatchRejected() {
        val shell = FakeShell(notResumed)
        val result = updater(shell, http(release(digest = "0".repeat(64)))).update(Target.TIZENTUBE, "2.0.3", abis)
        assertTrue(result is UpdateResult.Failed)
        assertTrue(shell.commands.none { it.startsWith("install") })
    }

    @Test
    fun http403LeavesInstalledAlone() {
        val shell = FakeShell()
        val result = updater(shell, http(json = "{\"message\":\"API rate limit exceeded\"}", code = 403))
            .update(Target.TIZENTUBE, "2.0.3", abis)
        assertTrue(result is UpdateResult.Failed)
        assertEquals(1, log.recent().size)
        assertTrue(shell.commands.isEmpty())
    }

    @Test
    fun selfUsesItsOwnRepoAndAsset() {
        val selfApi = "https://api.github.com/repos/iammosab/tubeguard/releases/latest"
        val selfJson = """{"tag_name":"v1.0.1","assets":[{"name":"tubeguard.apk","size":${apk.size},"browser_download_url":"https://example/tg.apk","digest":"sha256:$good"}]}"""
        val h = FakeHttp(mapOf(selfApi to HttpResult(200, selfJson)), mapOf("https://example/tg.apk" to apk))
        val shell = FakeShell(Regex("^install") to ShellResult(0, "Success"))
        assertEquals(UpdateResult.Installed("v1.0.1"), Updater(shell, h, log, cache).update(Target.SELF, "1.0.0", abis))
        assertTrue("install tubeguard.apk" in shell.commands)
    }

    @Test
    fun recordsLatestTagEvenWhenUpToDate() {
        val store = MapKeyValue()
        Updater(FakeShell(notResumed), http(release(tag = "v2.0.3")), log, cache, store).update(Target.TIZENTUBE, "2.0.3", abis)
        assertEquals("v2.0.3", store.get("latest.TIZENTUBE"))
    }

    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
}
