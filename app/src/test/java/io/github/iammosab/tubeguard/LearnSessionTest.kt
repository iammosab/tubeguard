package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.LearnSession
import io.github.iammosab.tubeguard.button.TclKeys
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnSessionTest {
    private val otherService = "com.example.launcher/com.example.launcher.ButtonService"
    private val tizen4065 = "{\"keycode\":4065,\"behavior\":[{\"intent\":\"component=io.gh.reisxd.tizentube.cobalt/x\"}]}"
    private val stock4066 = "{\"keycode\":4066,\"behavior\":[{\"intent\":\"stock\"}]}"

    private fun tv() = FakeTv(
        global = mutableMapOf(
            "CustomKeyList" to "[\"4065\",\"4066\"]",
            "CustomKey_4065" to tizen4065,
            "CustomKey_4066" to stock4066,
        ),
        secure = mutableMapOf("enabled_accessibility_services" to "$otherService:$OUR_SERVICE", "accessibility_enabled" to "1"),
    )

    private fun session(tv: FakeTv, store: MapKeyValue = MapKeyValue()) = LearnSession(tv, store, TclKeys(tv, store))

    @Test
    fun startPausesEveryOtherBinding() {
        val tv = tv()
        assertTrue(session(tv).start())
        // Only our service may see keys while learning…
        assertEquals(OUR_SERVICE, tv.secure["enabled_accessibility_services"])
        assertEquals("1", tv.secure["accessibility_enabled"])
        // …and TCL's table is empty, so no key launches anything.
        assertEquals(null, tv.global["CustomKeyList"])
        assertEquals(null, tv.global["CustomKey_4065"])
        assertEquals(null, tv.global["CustomKey_4066"])
    }

    @Test
    fun finishPutsEverythingBackExactly() {
        val tv = tv()
        val before = tv.global.toMap() to tv.secure.toMap()
        val s = session(tv)
        s.start()
        assertTrue(s.finish(learned = null))
        assertEquals(before, tv.global.toMap() to tv.secure.toMap())
    }

    @Test
    fun learnedKeyIsMappedOnTcl() {
        val tv = tv()
        val store = MapKeyValue()
        val s = session(tv, store)
        s.start()
        s.finish(learned = 300)
        assertEquals("TCL's table does the work; take-over stays as it was", null, store.get("takeOverButton"))
        val list = JSONArray(tv.global["CustomKeyList"])
        assertEquals(listOf("4065", "4066", "300"), (0 until list.length()).map { list.getString(it) })
        assertTrue(JSONObject(tv.global["CustomKey_300"]!!).toString().contains("io.gh.reisxd.tizentube.cobalt"))
        // Other keys exactly as before.
        assertEquals(stock4066, tv.global["CustomKey_4066"])
        assertEquals("$otherService:$OUR_SERVICE", tv.secure["enabled_accessibility_services"])
    }

    @Test
    fun interruptedSessionIsPutBackNextTime() {
        val tv = tv()
        val store = MapKeyValue()
        session(tv, store).start()
        // The process died while learning; a new one finds the saved bindings and restores them.
        assertTrue(session(tv, store).restoreIfInterrupted())
        assertEquals("[\"4065\",\"4066\"]", tv.global["CustomKeyList"])
        assertEquals(tizen4065, tv.global["CustomKey_4065"])
        assertEquals("$otherService:$OUR_SERVICE", tv.secure["enabled_accessibility_services"])
        assertFalse("nothing left to restore", session(tv, store).restoreIfInterrupted())
    }

    @Test
    fun unreadableTvChangesNothing() {
        val tv = tv().apply { failReads = true }
        val before = tv.global.toMap() to tv.secure.toMap()
        assertFalse(session(tv).start())
        assertEquals(before, tv.global.toMap() to tv.secure.toMap())
    }

    @Test
    fun otherBrandsOnlyPauseOtherServices() {
        val tv = FakeTv(secure = mutableMapOf("enabled_accessibility_services" to "$otherService:$OUR_SERVICE"))
        val store = MapKeyValue()
        val s = session(tv, store)
        assertTrue(s.start())
        assertEquals(OUR_SERVICE, tv.secure["enabled_accessibility_services"])
        assertTrue(tv.commands.none { it.contains("CustomKey") && !it.startsWith("settings get") })
        s.finish(learned = 300)
        assertEquals("$otherService:$OUR_SERVICE", tv.secure["enabled_accessibility_services"])
        assertEquals(null, tv.global["CustomKeyList"])
        // Without TCL's table, TubeGuard itself has to catch the key, so take-over goes on.
        assertEquals("true", store.get("takeOverButton"))
    }
}
