package io.github.iammosab.tubeguard.button

import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.state.KeyValue

/**
 * Which remote keys the accessibility service takes over for TizenTube on TVs without TCL's table. Off until the
 * owner turns it on, since another app (other launchers or button apps) may already handle the same button.
 */
class KeyPolicy(private val store: KeyValue, private val now: () -> Long = System::currentTimeMillis) {
    private var learningSince: Long? = null

    /** The key caught by the current (or last) learning, or null if none was. */
    @Volatile
    var lastLearned: Int? = null
        private set

    val learning: Boolean
        get() {
            val since = learningSince ?: return false
            if (now() - since > LEARN_MS) learningSince = null
            return learningSince != null
        }

    fun shouldHandle(keyCode: Int): Boolean {
        if (store.get(TAKE_OVER) != "true") return false
        val learned = store.get(LEARNED)?.toIntOrNull()
        return if (learned != null) keyCode == learned else keyCode in DEFAULT_CODES
    }

    fun startLearning() {
        lastLearned = null
        learningSince = now()
    }

    fun stopLearning() {
        learningSince = null
    }

    /**
     * While learning, records [keyCode] as the YouTube button and stops learning. The remote's navigation keys
     * are never learned (false, and they keep working), so the owner can still move around and Back cancels.
     */
    fun learn(keyCode: Int): Boolean {
        if (!learning || isNavigation(keyCode)) return false
        store.put(LEARNED, keyCode.toString())
        lastLearned = keyCode
        learningSince = null
        return true
    }

    companion object {
        const val TAKE_OVER = "takeOverButton"
        const val LEARNED = "learnedKeyCode"
        private const val LEARN_MS = 15_000L
        /** KEYCODE_BUTTON_3, which many remotes send for YouTube, and TCL's own YouTube code. */
        val DEFAULT_CODES = listOf(190, 4065)
        /** DPAD up, down, left, right, centre; Back; Home; Enter. */
        private val NAVIGATION = setOf(19, 20, 21, 22, 23, 4, 3, 66)

        fun isNavigation(keyCode: Int) = keyCode in NAVIGATION

        /** The other accessibility services listed in [enabledServices], which may also handle remote buttons. */
        fun otherKeyServices(enabledServices: String?): List<String> =
            AccessibilitySetting.entries(enabledServices) - OUR_SERVICE
    }
}
