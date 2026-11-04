package io.github.iammosab.tubeguard.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.iammosab.tubeguard.App

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Scheduler.schedule(context)
        App.of(context).checkNow("boot")
    }
}
