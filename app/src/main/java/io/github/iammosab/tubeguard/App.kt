package io.github.iammosab.tubeguard

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.iammosab.tubeguard.access.AccessibilitySetting
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.button.TclKeys
import io.github.iammosab.tubeguard.flow.Setup
import io.github.iammosab.tubeguard.flow.StepResult
import io.github.iammosab.tubeguard.flow.Undo
import io.github.iammosab.tubeguard.guard.GuardResult
import io.github.iammosab.tubeguard.guard.Watchdog
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.jobs.Scheduler
import io.github.iammosab.tubeguard.shell.AdbKeepAlive
import io.github.iammosab.tubeguard.shell.LocalAdb
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.PrefsKeyValue
import io.github.iammosab.tubeguard.ui.MainActivity
import io.github.iammosab.tubeguard.ui.Status
import io.github.iammosab.tubeguard.update.Target
import io.github.iammosab.tubeguard.update.UpdateResult
import io.github.iammosab.tubeguard.update.Updater
import io.github.iammosab.tubeguard.update.UrlHttp
import java.util.concurrent.Executors

/** The app's single set of collaborators, and one background thread every check and change runs on. */
class App : Application() {
    val adb by lazy { LocalAdb(this) }
    val store by lazy { PrefsKeyValue(this) }
    val log by lazy { EventLog(store) }
    val guard by lazy { YouTubeGuard(adb, log, store = store) }
    val keys by lazy { KeyPolicy(store) }
    val tcl by lazy { TclKeys(adb, store) }
    val updater by lazy { Updater(adb, UrlHttp(), log, cacheDir, store) }
    val watchdog by lazy {
        Watchdog(adb, log, jobsPending = { Scheduler.pending(this) }, schedule = { Scheduler.schedule(this) })
    }
    /** Checks and changes, one at a time. */
    private val worker = Executors.newSingleThreadExecutor()
    /** Downloads and installs, so a 150 MB update never holds up a YouTube check. */
    private val updates = Executors.newSingleThreadExecutor()
    private val abis get() = Build.SUPPORTED_ABIS.toList()

    override fun onCreate() {
        super.onCreate()
        if (store.get(Setup.PROTECTION) != null) adb.markAllowed()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_protection), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    fun background(block: () -> Unit) = worker.execute(guarded(block))

    private fun guarded(block: () -> Unit) = Runnable {
        runCatching(block).onFailure { Log.e(LocalAdb.TAG, "background task failed", it) }
    }

    fun checkNow(trigger: String) = background { checkNowBlocking(trigger) }

    /** One check, on the calling (background) thread. */
    fun checkNowBlocking(trigger: String) {
        val result = guard.enforce(trigger)
        val guarding = store.get(Setup.PROTECTION) == "on"
        if (guarding && result !is GuardResult.Failed) store.put("lastCheck", System.currentTimeMillis().toString())
        if (store.get(Setup.PROTECTION) == "on" && adb.connected) watchdog.ensure()
        Log.i(LocalAdb.TAG, "check ($trigger): $result")
        if (result is GuardResult.Failed && store.get(Setup.PROTECTION) == "on") {
            notify(getString(R.string.notify_not_protected, result.reason))
        }
    }

    /** Updates TizenTube (only if it is installed; installing it is setup's job) and then this app. */
    fun updateAll(trigger: String) = updates.execute(guarded {
        if (store.get(Setup.PROTECTION) != "off") versionOf(Target.TIZENTUBE.pkg)?.let { updateTizenTube(it, force = false) }
        Log.i(LocalAdb.TAG, "update self ($trigger): ${updater.update(Target.SELF, versionOf(packageName), abis)}")
    })

    fun updateTizenTube(installed: String?, force: Boolean, replace: Boolean = false): UpdateResult {
        val result = updater.update(Target.TIZENTUBE, installed, abis, force, replace)
        when (result) {
            UpdateResult.SignatureMismatch -> store.put(SIGNATURE_MISMATCH, "true")
            is UpdateResult.Installed -> store.put(SIGNATURE_MISMATCH, null)
            else -> {}
        }
        Log.i(LocalAdb.TAG, "update TizenTube: $result")
        return result
    }

    /**
     * Replaces TizenTube (signing the owner out of it) with the release build, only on the owner's say. The old
     * copy goes only once the new one is downloaded and checked.
     */
    fun reinstallTizenTube(): UpdateResult {
        log.add("tizentube_reinstall", "replacing the installed copy with the release build")
        return updateTizenTube(null, force = true, replace = true)
    }

    fun setup(): List<StepResult> {
        adb.connect(LocalAdb.SETUP_WAIT_MS, ask = true)
        val steps = Setup(
            shell = adb,
            guard = guard,
            keys = tcl,
            log = log,
            store = store,
            tizenTubeVersion = { versionOf(Target.TIZENTUBE.pkg) },
            installTizenTube = { updateTizenTube(null, force = true) },
            schedule = { Scheduler.schedule(this) },
        ).run(abis, tcl.isTcl())
        if (Build.VERSION.SDK_INT >= 33) adb.run("pm grant $packageName android.permission.POST_NOTIFICATIONS")
        return steps
    }

    fun undo(): List<StepResult> =
        Undo(adb, tcl, log, store, isTcl = tcl.isTcl(), cancelJobs = { Scheduler.cancel(this) }).run()

    fun status(): Status {
        val connected = adb.connect()
        val services = if (connected) adb.run("settings get secure enabled_accessibility_services").out else null
        val a11yOn = connected && adb.run("settings get secure accessibility_enabled").out.trim() == "1"
        val isTcl = connected && tcl.isTcl()
        return Status(
            adbConnected = connected,
            protection = store.get(Setup.PROTECTION),
            youtubePresent = if (connected) guard.isPresent(YouTubeGuard.YOUTUBE) else null,
            tizenTube = versionOf(Target.TIZENTUBE.pkg),
            tizenTubeLatest = store.get("latest.${Target.TIZENTUBE.name}"),
            signatureMismatch = store.get(SIGNATURE_MISMATCH) == "true",
            isTcl = isTcl,
            tclMapped = isTcl && tcl.isMapped(),
            takeOver = store.get(KeyPolicy.TAKE_OVER) == "true",
            learnedKey = store.get(KeyPolicy.LEARNED),
            otherKeyServices = KeyPolicy.otherKeyServices(services),
            keepAdbOn = store.get(AdbKeepAlive.KEY) != "false",
            lastCheck = store.get("lastCheck")?.toLongOrNull(),
            removals = store.get(YouTubeGuard.REMOVALS)?.toIntOrNull() ?: 0,
            appVersion = versionOf(packageName),
            appLatest = store.get("latest.${Target.SELF.name}"),
            log = log.recent(50),
            watchdogOk = !connected || (OUR_SERVICE in AccessibilitySetting.entries(services) && a11yOn && Scheduler.pending(this)),
        )
    }

    fun versionOf(pkg: String): String? =
        runCatching { packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()

    fun notify(text: String) {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { getSystemService(NotificationManager::class.java).notify(1, n) }
    }

    companion object {
        private const val CHANNEL = "protection"
        private const val SIGNATURE_MISMATCH = "tizentube.signatureMismatch"
        fun of(context: Context) = context.applicationContext as App
    }
}
