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
        learningSince = now()
    }

    /** While learning, records [keyCode] as the YouTube button and stops learning. */
    fun learn(keyCode: Int): Boolean {
        if (!learning) return false
        store.put(LEARNED, keyCode.toString())
        learningSince = null
        return true
    }

    companion object {
        const val TAKE_OVER = "takeOverButton"
        const val LEARNED = "learnedKeyCode"
        private const val LEARN_MS = 15_000L
        /** KEYCODE_BUTTON_3, which many remotes send for YouTube, and TCL's own YouTube code. */
        val DEFAULT_CODES = listOf(190, 4065)

        /** The other accessibility services listed in [enabledServices], which may also handle remote buttons. */
        fun otherKeyServices(enabledServices: String?): List<String> =
            AccessibilitySetting.entries(enabledServices) - OUR_SERVICE
    }
}
