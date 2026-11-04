package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.shell.Shell
import io.github.iammosab.tubeguard.shell.ShellResult
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TclKeysTest {
    private val office4065 = javaClass.getResource("/office-tv-customkey-4065.json")!!.readText().trim()
    private val office190 = office4065.replace("\"keycode\":4065", "\"keycode\":190")

    /** A fake TV whose global settings live in [settings]; `settings put/delete global` really change them. */
    private class Tv(vararg initial: Pair<String, String>) : Shell {
        val settings = mutableMapOf(*initial)
        val commands = mutableListOf<String>()
        var manufacturer = "TCL"
        private val get = Regex("^settings get global (\\S+)$")
        private val put = Regex("^settings put global (\\S+) '(.*)'$")
        private val delete = Regex("^settings delete global (\\S+)$")

        override fun run(cmd: String): ShellResult {
            commands += cmd
            get.find(cmd)?.let { return ShellResult(0, settings[it.groupValues[1]] ?: "null") }
            put.find(cmd)?.let { settings[it.groupValues[1]] = it.groupValues[2]; return ShellResult(0, "") }
            delete.find(cmd)?.let { settings.remove(it.groupValues[1]); return ShellResult(0, "") }
            if (cmd.startsWith("getprop ro.product.manufacturer")) return ShellResult(0, manufacturer)
            return ShellResult(0, "")
        }

        override fun install(apk: File) = ShellResult(1, "unused")
    }

    private fun keys(tv: Tv, store: MapKeyValue = MapKeyValue()) = TclKeys(tv, store)

    @Test
    fun entryMatchesSpec() {
        val tv = Tv()
        assertEquals(JSONObject(office4065).toString(), JSONObject(keys(tv).entry(4065)).toString())
    }

    @Test
    fun applyOnFreshTclWritesBothKeysAndList() {
        val tv = Tv()
        assertTrue(keys(tv).apply())
        assertEquals(listOf("4065", "190"), list(tv.settings["CustomKeyList"]))
        assertEquals(JSONObject(office190).toString(), JSONObject(tv.settings["CustomKey_190"]!!).toString())
    }

    @Test
    fun mergeKeepsOtherKeys() {
        val tv = Tv("CustomKeyList" to "[\"4066\"]", "CustomKey_4066" to "{\"keycode\":4066}")
        assertTrue(keys(tv).apply())
        assertEquals(listOf("4066", "4065", "190"), list(tv.settings["CustomKeyList"]))
        assertEquals("{\"keycode\":4066}", tv.settings["CustomKey_4066"])
        assertTrue(tv.commands.none { it.contains("CustomKey_4066 ") })
    }

    @Test
    fun garbageListIsNotOverwritten() {
        val tv = Tv("CustomKeyList" to "oops")
        assertFalse(keys(tv).apply())
        assertTrue(tv.commands.none { it.startsWith("settings put") })
        assertEquals("oops", tv.settings["CustomKeyList"])
    }

    @Test
    fun recordsOriginalsOnce() {
        val stock = "{\"keycode\":190,\"behavior\":[{\"keyaction\":1,\"type\":1,\"intent\":\"stock\"}]}"
        val tv = Tv("CustomKeyList" to "[\"190\"]", "CustomKey_190" to stock)
        val store = MapKeyValue()
        keys(tv, store).apply()
        assertEquals(stock, store.get("orig.CustomKey_190"))
        assertEquals(TclKeys.ABSENT, store.get("orig.CustomKey_4065"))
        keys(tv, store).apply()
        assertEquals(stock, store.get("orig.CustomKey_190"))
    }

    @Test
    fun handMadeSetupIsRecordedAsOurs() {
        val tv = Tv("CustomKeyList" to "[\"4065\",\"190\"]", "CustomKey_4065" to office4065, "CustomKey_190" to office190)
        val store = MapKeyValue()
        keys(tv, store).apply()
        assertEquals(TclKeys.OURS, store.get("orig.CustomKey_4065"))
        assertEquals(TclKeys.OURS, store.get("orig.CustomKey_190"))
    }

    @Test
    fun isMappedDetectsHandMadeSetup() {
        val tv = Tv("CustomKeyList" to "[\"4065\",\"190\"]", "CustomKey_4065" to office4065, "CustomKey_190" to office190)
        assertTrue(keys(tv).isMapped())
        assertFalse(keys(Tv()).isMapped())
    }

    @Test
    fun undoDeletesKeysBeforeList() {
        val tv = Tv("CustomKeyList" to "[\"4065\",\"190\"]", "CustomKey_4065" to office4065, "CustomKey_190" to office190)
        val store = MapKeyValue()
        val k = keys(tv, store)
        k.apply()
        tv.commands.clear()
        assertTrue(k.undo())
        val writes = tv.commands.filter { !it.startsWith("settings get") }
        assertEquals(
            listOf(
                "settings delete global CustomKey_4065",
                "settings delete global CustomKey_190",
                "settings delete global CustomKeyList",
            ),
            writes,
        )
        assertTrue(tv.settings.isEmpty())
    }

    @Test
    fun undoRestoresStockValueAndKeepsOtherKeys() {
        val stock = "{\"keycode\":190,\"behavior\":[{\"keyaction\":1,\"type\":1,\"intent\":\"stock\"}]}"
        val tv = Tv("CustomKeyList" to "[\"4066\",\"190\"]", "CustomKey_190" to stock, "CustomKey_4066" to "{}")
        val store = MapKeyValue()
        val k = keys(tv, store)
        k.apply()
        assertTrue(k.undo())
        assertEquals(stock, tv.settings["CustomKey_190"])
        assertEquals(null, tv.settings["CustomKey_4065"])
        assertEquals(listOf("4066", "190"), list(tv.settings["CustomKeyList"]))
    }

    @Test
    fun isTclFromManufacturerOrList() {
        assertTrue(keys(Tv()).isTcl())
        val other = Tv().apply { manufacturer = "Xiaomi" }
        assertFalse(keys(other).isTcl())
    }

    private fun list(json: String?): List<String> {
        val a = JSONArray(json ?: "[]")
        return (0 until a.length()).map { a.getString(it) }
    }

    /** A TV where `settings get` for [failing] keys fails (ADB dropped mid-read). */
    private class FlakyTv(private val tv: Tv, private val failing: Set<String>) : Shell {
        override fun run(cmd: String): ShellResult {
            if (failing.any { cmd == "settings get global $it" }) { tv.commands += cmd; return ShellResult(-100, "ADB connection lost") }
            return tv.run(cmd)
        }
        override fun install(apk: File) = tv.install(apk)
    }

    @Test
    fun failedListReadDoesNotApply() {
        val tv = Tv("CustomKeyList" to "[\"4066\"]")
        assertFalse(TclKeys(FlakyTv(tv, setOf("CustomKeyList")), MapKeyValue()).apply())
        assertTrue(tv.commands.none { it.startsWith("settings put") })
        assertEquals("[\"4066\"]", tv.settings["CustomKeyList"])
    }

    @Test
    fun failedListReadDoesNotUndo() {
        val tv = Tv("CustomKeyList" to "[\"4066\",\"4065\"]")
        assertFalse(TclKeys(FlakyTv(tv, setOf("CustomKeyList")), MapKeyValue()).undo())
        assertTrue(tv.commands.none { it.startsWith("settings put") || it.startsWith("settings delete") })
    }

    @Test
    fun failedKeyReadIsNotRecordedAsAbsent() {
        val stock = "{\"keycode\":190}"
        val tv = Tv("CustomKeyList" to "[\"190\"]", "CustomKey_190" to stock)
        val store = MapKeyValue()
        assertFalse(TclKeys(FlakyTv(tv, setOf("CustomKey_190")), store).apply())
        assertEquals(null, store.get("orig.CustomKey_190"))
        assertEquals(stock, tv.settings["CustomKey_190"])
    }

    @Test
    fun unknownBrandCountsAsTclForUndo() {
        // If even the reads fail, Undo still tries the TCL table (harmless elsewhere) rather than skipping it.
        val tv = Tv().apply { manufacturer = "Xiaomi" }
        assertTrue(TclKeys(FlakyTv(tv, setOf("CustomKeyList")), MapKeyValue()).isTcl())
    }

    @Test
    fun applyMakesTclReloadItsTable() {
        // TCL keeps the table in memory and rereads it only when CustomKeyList changes: the list is set to empty
        // and back, after every entry is written, so the new entries are always picked up.
        val tv = Tv("CustomKeyList" to "[\"4065\",\"190\"]")
        keys(tv).apply()
        val writes = tv.commands.filter { it.startsWith("settings put global") }
        assertEquals("settings put global CustomKeyList '[]'", writes[writes.size - 2])
        assertTrue(writes.last().startsWith("settings put global CustomKeyList '[\"4065\",\"190\"]'"))
        assertTrue(writes.dropLast(2).all { it.startsWith("settings put global CustomKey_") })
    }
}
