package io.github.iammosab.tubeguard.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.iammosab.tubeguard.App
import io.github.iammosab.tubeguard.flow.Setup
import io.github.iammosab.tubeguard.shell.AdbKeepAlive

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // After a restart, and after TubeGuard itself was updated (Android doesn't always start the service again).
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val trigger = if (intent.action == Intent.ACTION_BOOT_COMPLETED) "boot" else "updated"
        Scheduler.schedule(context)
        val app = App.of(context)
        app.background {
            AdbKeepAlive.onBoot(
                context, app.adb,
                enabled = app.store.get(AdbKeepAlive.KEY) != "false",
                protecting = app.store.get(Setup.PROTECTION) == "on",
            )
        }
        app.checkNow(trigger)
        app.updateAll(trigger)
    }
}
