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
import io.github.iammosab.tubeguard.access.GuardService
import io.github.iammosab.tubeguard.access.OUR_SERVICE
import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.button.LearnSession
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
import io.github.iammosab.tubeguard.update.Progress
import io.github.iammosab.tubeguard.update.Target
import io.github.iammosab.tubeguard.update.shouldInstallTizenTube
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
    val learnSession by lazy { LearnSession(adb, store, tcl) }
    val updater by lazy { Updater(adb, UrlHttp(), log, cacheDir, store) }
    val watchdog by lazy {
        Watchdog(
            adb, log,
            jobsPending = { Scheduler.pending(this) },
            schedule = { Scheduler.schedule(this) },
            serviceRunning = { GuardService.running },
        )
    }
    /** Checks and changes, one at a time. */
    private val worker = Executors.newSingleThreadExecutor()
    /** Downloads and installs, so a 150 MB update never holds up a YouTube check. */
    private val updates = Executors.newSingleThreadExecutor()
    private val abis get() = Build.SUPPORTED_ABIS.toList()

    /** When the TizenTube button was last pressed (any brand, take-over or not). */
    @Volatile
    var lastTizenTubeKeyAt: Long? = null
        private set

    /** Called by the accessibility service for every press of the TizenTube button. */
    fun onTizenTubeKey() {
        val at = System.currentTimeMillis()
        lastTizenTubeKeyAt = at
        // A moment later: did TizenTube really come up? That, not the settings, is what the home screen reports.
        android.os.Handler(mainLooper).postDelayed({ background { verifyButton() } }, 1_500)
        // If no other app reacts this time, an old warning no longer holds.
        android.os.Handler(mainLooper).postDelayed({
            if ((store.get(KEY_CONFLICT_AT)?.toLongOrNull() ?: 0) < at) {
                store.put(KEY_CONFLICT, null)
                store.put(KEY_CONFLICT_AT, null)
            }
        }, 2_500)
    }

    private fun verifyButton() {
        if (store.get(Setup.PROTECTION) != "on" || !adb.connected) return
        val top = adb.run("dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity'")
        if (!top.ok) return
        val worked = top.out.contains("${Target.TIZENTUBE.pkg}/")
        if (!worked && store.get(BUTTON_WORKED) != "false") log.add("button_failed", "the TizenTube button didn't open TizenTube")
        store.put(BUTTON_WORKED, worked.toString())
    }

    /** The owner asked to fix the button: rewrite TCL's table (making TCL reload it), or take the key over. */
    fun fixButton() = background {
        if (tcl.isTcl()) tcl.apply() else store.put(KeyPolicy.TAKE_OVER, "true")
        store.put(BUTTON_WORKED, null)
        log.add("button_fixed", "mapping written again")
    }

    /** Another app opened TubeGuard right after the TizenTube button: remembered for the home screen. */
    fun onKeyConflict(appName: String) {
        if (store.get(KEY_CONFLICT) != appName) log.add("key_conflict", "$appName also reacts to the TizenTube button")
        store.put(KEY_CONFLICT, appName)
        store.put(KEY_CONFLICT_AT, System.currentTimeMillis().toString())
    }

    /** A TizenTube download or install running right now, for the screen to show. */
    @Volatile
    var tizenTubeWork: Progress? = null
        private set

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

    /**
     * Pauses every other remote binding, then starts listening for the owner's YouTube button. [ready] is told
     * (on the background thread) whether listening started; it only starts once the pause is in place.
     */
    fun startLearning(ready: (Boolean) -> Unit) = background {
        val ok = learnSession.start()
        if (ok) keys.startLearning()
        ready(ok)
    }

    /** Ends learning (caught a key, timed out or cancelled) and puts every paused binding back. */
    fun finishLearning() {
        keys.stopLearning()
        background { learnSession.finish(keys.lastLearned) }
    }

    /** One check, on the calling (background) thread. */
    fun checkNowBlocking(trigger: String) {
        // A learning session cut short (crash, force-stop, power off) leaves the other bindings paused.
        if (!keys.learning && learnSession.restoreIfInterrupted()) log.add("learn_restored", "remote buttons put back")
        val result = guard.enforce(trigger)
        val guarding = store.get(Setup.PROTECTION) == "on"
        if (guarding && result !is GuardResult.Failed) store.put("lastCheck", System.currentTimeMillis().toString())
        if (store.get(Setup.PROTECTION) == "on" && adb.connected) watchdog.ensure()
        Log.i(LocalAdb.TAG, "check ($trigger): $result")
        if (result is GuardResult.Failed && store.get(Setup.PROTECTION) == "on") {
            notify(getString(R.string.notify_not_protected, result.reason))
        }
    }

    /** Installs TizenTube if it went missing (not if the owner removed it), updates it, then updates this app. */
    fun updateAll(trigger: String) = updates.execute(guarded {
        val installed = versionOf(Target.TIZENTUBE.pkg)
        if (shouldInstallTizenTube(store.get(Setup.PROTECTION), installed, store.get(REMOVED_BY_OWNER) == "true")) {
            log.add("tizentube_missing", "installing it again ($trigger)")
            updateTizenTube(null, force = true)
        } else if (installed != null && store.get(Setup.PROTECTION) != "off") {
            updateTizenTube(installed, force = false)
        }
        Log.i(LocalAdb.TAG, "update self ($trigger): ${updater.update(Target.SELF, versionOf(packageName), abis)}")
    })

    fun updateTizenTube(installed: String?, force: Boolean, replace: Boolean = false): UpdateResult {
        val result = try {
            updater.update(Target.TIZENTUBE, installed, abis, force, replace) { tizenTubeWork = it }
        } finally {
            tizenTubeWork = null
        }
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

    /** The owner asked to install TizenTube: undoes "removed by owner" and installs it in the background. */
    fun installTizenTube() {
        store.put(REMOVED_BY_OWNER, null)
        updates.execute(guarded { updateTizenTube(versionOf(Target.TIZENTUBE.pkg), force = true) })
    }

    /** The owner's own uninstall: remembered, so TubeGuard doesn't put TizenTube straight back. */
    fun uninstallTizenTube() {
        store.put(REMOVED_BY_OWNER, "true")
        adb.run("pm uninstall ${Target.TIZENTUBE.pkg}")
        log.add("tizentube_removed", "uninstalled from Settings")
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
            installTizenTube = { store.put(REMOVED_BY_OWNER, null); updateTizenTube(null, force = true) },
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
            tizenTubeWork = tizenTubeWork,
            keyConflict = store.get(KEY_CONFLICT),
            buttonWorked = store.get(BUTTON_WORKED)?.toBooleanStrictOrNull(),
            watchdogOk = !connected ||
                (OUR_SERVICE in AccessibilitySetting.entries(services) && a11yOn && GuardService.running && Scheduler.pending(this)),
        )
    }

    fun versionOf(pkg: String): String? =
        runCatching { packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()

    fun notify(text: String) {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
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
        private const val REMOVED_BY_OWNER = "tizentube.removedByOwner"
        private const val KEY_CONFLICT = "button.conflict"
        private const val BUTTON_WORKED = "button.worked"
        private const val KEY_CONFLICT_AT = "button.conflictAt"
        fun of(context: Context) = context.applicationContext as App
    }
}
