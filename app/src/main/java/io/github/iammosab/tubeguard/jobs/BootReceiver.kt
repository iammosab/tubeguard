package io.github.iammosab.tubeguard.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.iammosab.tubeguard.App
import io.github.iammosab.tubeguard.flow.Setup
import io.github.iammosab.tubeguard.shell.AdbKeepAlive

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Scheduler.schedule(context)
        val app = App.of(context)
        app.background {
            AdbKeepAlive.onBoot(
                context, app.adb,
                enabled = app.store.get(AdbKeepAlive.KEY) != "false",
                protecting = app.store.get(Setup.PROTECTION) == "on",
            )
        }
        app.checkNow("boot")
        App.of(context).updateAll("boot")
    }
}
