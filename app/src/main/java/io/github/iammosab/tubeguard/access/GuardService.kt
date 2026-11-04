package io.github.iammosab.tubeguard.access

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import io.github.iammosab.tubeguard.App
import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.shell.LocalAdb
import io.github.iammosab.tubeguard.guard.YouTubeGuard

/**
 * The long-lived part of the app. Android keeps accessibility services running where TV makers kill ordinary
 * background apps, so this is where YouTube coming back is noticed the moment it happens.
 */
class GuardService : AccessibilityService() {
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            if (pkg != YouTubeGuard.YOUTUBE) return
            val trigger = when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> "package_added"
                Intent.ACTION_PACKAGE_REPLACED -> "package_replaced"
                else -> "package_changed"
            }
            App.of(context).checkNow(trigger)
        }
    }

    override fun onServiceConnected() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packages, filter)
        App.of(this).checkNow("service_start")
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(packages) }
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val policy = App.of(this).keys
        Log.d(LocalAdb.TAG, "key ${event.keyCode} action ${event.action} handle=${policy.shouldHandle(event.keyCode)}")
        if (policy.learning) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                policy.learn(event.keyCode)
                App.of(this).log.add("button_learned", "key code ${event.keyCode}")
            }
            return true
        }
        if (!policy.shouldHandle(event.keyCode)) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            runCatching {
                startActivity(
                    Intent(Intent.ACTION_MAIN).setComponent(ComponentName(TclKeys.PKG, "dev.cobalt.app.MainActivity"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                )
            }
        }
        // Both the press and the release are consumed, so nothing else (e.g. Google TV's launcher) acts on the key.
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
