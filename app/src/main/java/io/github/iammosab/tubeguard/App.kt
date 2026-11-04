package io.github.iammosab.tubeguard

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.LocalAdb
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.PrefsKeyValue
import io.github.iammosab.tubeguard.update.Target
import io.github.iammosab.tubeguard.update.Updater
import io.github.iammosab.tubeguard.update.UrlHttp
import java.util.concurrent.Executors

/** The app's single set of collaborators, and one background thread every check and change runs on. */
class App : Application() {
    val adb by lazy { LocalAdb(this) }
    val store by lazy { PrefsKeyValue(this) }
    val log by lazy { EventLog(store) }
    val guard by lazy { YouTubeGuard(adb, log) }
    val keys by lazy { KeyPolicy(store) }
    val updater by lazy { Updater(adb, UrlHttp(), log, cacheDir, store) }
    private val worker = Executors.newSingleThreadExecutor()

    fun background(block: () -> Unit) {
        worker.execute { runCatching(block).onFailure { Log.e(LocalAdb.TAG, "background task failed", it) } }
    }

    fun checkNow(trigger: String) = background {
        store.put("lastCheck", System.currentTimeMillis().toString())
        Log.i(LocalAdb.TAG, "check ($trigger): ${guard.enforce(trigger)}")
    }

    /** Updates TizenTube (only if it is installed; installing it is setup's job) and then this app. */
    fun updateAll(trigger: String) = background {
        val abis = Build.SUPPORTED_ABIS.toList()
        versionOf(Target.TIZENTUBE.pkg)?.let {
            Log.i(LocalAdb.TAG, "update TizenTube ($trigger): ${updater.update(Target.TIZENTUBE, it, abis)}")
        }
        Log.i(LocalAdb.TAG, "update self ($trigger): ${updater.update(Target.SELF, versionOf(packageName), abis)}")
    }

    fun versionOf(pkg: String): String? =
        runCatching { packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()

    companion object {
        fun of(context: Context) = context.applicationContext as App
    }
}
