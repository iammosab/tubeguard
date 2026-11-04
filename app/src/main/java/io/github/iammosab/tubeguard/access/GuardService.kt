package io.github.iammosab.tubeguard.access

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import io.github.iammosab.tubeguard.App
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
