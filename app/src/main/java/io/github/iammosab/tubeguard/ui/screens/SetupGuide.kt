package io.github.iammosab.tubeguard.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** The TV settings pages the walk-through can open directly. */
enum class SettingsPage(val action: String) {
    ABOUT("android.settings.DEVICE_INFO_SETTINGS"),
    DEVELOPER_OPTIONS("android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
}

fun SetupStep.page(): SettingsPage? = when (this) {
    SetupStep.DEVELOPER -> SettingsPage.ABOUT
    SetupStep.USB, SetupStep.PAIR -> SettingsPage.DEVELOPER_OPTIONS
    SetupStep.ALLOW, SetupStep.DONE -> null
}

/** What the TV says about itself; any app may read these two settings. */
data class DeviceState(val developerOptions: Boolean, val usbDebugging: Boolean)

/** Whether the TV already shows this step as done; null where the TV can't tell. */
fun SetupStep.done(state: DeviceState): Boolean? = when (this) {
    SetupStep.DEVELOPER -> state.developerOptions
    SetupStep.USB -> state.usbDebugging
    else -> null
}

fun readDeviceState(context: Context): DeviceState {
    val cr = context.contentResolver
    return DeviceState(
        developerOptions = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1,
        usbDebugging = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1,
    )
}

/**
 * An intent for this page, or null when this TV has no such page (some brands don't). Started from the activity
 * without NEW_TASK, the page opens on top of TubeGuard, so the remote's Back comes straight back to the step.
 */
fun SettingsPage.intent(context: Context): Intent? =
    Intent(action).takeIf { it.resolveActivity(context.packageManager) != null }
