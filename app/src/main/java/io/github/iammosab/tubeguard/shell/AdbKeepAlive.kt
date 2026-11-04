package io.github.iammosab.tubeguard.shell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE

/**
 * Some TVs switch network debugging off at every restart, which would leave the app powerless. With
 * WRITE_SECURE_SETTINGS (granted once over ADB during setup) the app switches it back on itself, and puts its
 * accessibility service back if something (a force-stop) left it unbound.
 */
object AdbKeepAlive {
    const val KEY = "keepAdbOn"

    fun grantSelf(shell: Shell): Boolean =
        shell.run("pm grant io.github.iammosab.tubeguard android.permission.WRITE_SECURE_SETTINGS").ok

    /**
     * TCL's own auto-start switch (app op AUTO_START): while it's "ignore", the TV drops boot and app-update
     * broadcasts for the app, so nothing would restart the watch. Other brands don't have the op; that's fine.
     */
    fun allowAutoStart(shell: Shell): Boolean = shell.run("appops set io.github.iammosab.tubeguard AUTO_START allow").ok

    fun canWriteSettings(context: Context) =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Runs on the app's background thread at boot. */
    fun onBoot(context: Context, adb: LocalAdb, enabled: Boolean, protecting: Boolean) {
        if (!canWriteSettings(context)) return
        val cr = context.contentResolver
        val services = Settings.Secure.getString(cr, "enabled_accessibility_services")
        if (enabled && !adb.connect()) {
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            if (Build.VERSION.SDK_INT >= 30) Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
            for (attempt in 1..3) {
                Thread.sleep(10_000)
                if (adb.connect()) break
            }
            Log.i(LocalAdb.TAG, "keep ADB on: connected=${adb.connected}")
        }
        if (!protecting) return
        // Android binds a service when the setting changes, so ours is taken out and put back; and put back at
        // all if a force-stop dropped it.
        Settings.Secure.putString(cr, "enabled_accessibility_services", AccessibilitySetting.withoutService(services, OUR_SERVICE))
        Settings.Secure.putString(cr, "enabled_accessibility_services", AccessibilitySetting.withService(services, OUR_SERVICE))
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
    }
}
