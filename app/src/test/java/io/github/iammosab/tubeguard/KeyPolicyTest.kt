package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.KeyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyPolicyTest {
    private var clock = 0L
    private fun policy(store: MapKeyValue = MapKeyValue()) = KeyPolicy(store) { clock }

    @Test
    fun offByDefault() {
        assertFalse(policy().shouldHandle(190))
        assertFalse(policy().shouldHandle(4065))
    }

    @Test
    fun handlesDefaultsWhenOn() {
        val p = policy(MapKeyValue("takeOverButton" to "true"))
        assertTrue(p.shouldHandle(190))
        assertTrue(p.shouldHandle(4065))
        assertFalse(p.shouldHandle(19)) // DPAD_UP
    }

    @Test
    fun learnedCodeReplacesDefaults() {
        val p = policy(MapKeyValue("takeOverButton" to "true", "learnedKeyCode" to "300"))
        assertTrue(p.shouldHandle(300))
        assertFalse(p.shouldHandle(190))
    }

    @Test
    fun learningCapturesOneKeyThenStops() {
        val store = MapKeyValue()
        val p = policy(store)
        assertFalse(p.learn(300))
        p.startLearning()
        assertTrue(p.learning)
        assertTrue(p.learn(300))
        assertEquals("300", store.get("learnedKeyCode"))
        assertFalse(p.learning)
        assertFalse(p.learn(301))
        assertEquals("300", store.get("learnedKeyCode"))
    }

    @Test
    fun learningTimesOutAfter15Seconds() {
        val p = policy()
        p.startLearning()
        clock = 15_001
        assertFalse(p.learn(300))
        assertFalse(p.learning)
    }

    @Test
    fun othersExcludesOurs() = assertEquals(
        listOf("com.example.remote/.KeyService", "com.example.launcher/com.example.launcher.ButtonService"),
        KeyPolicy.otherKeyServices("com.example.remote/.KeyService:$OUR_SERVICE:com.example.launcher/com.example.launcher.ButtonService"),
    )
}
