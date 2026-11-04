package io.github.iammosab.tubeguard

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.LocalAdb
import io.github.iammosab.tubeguard.state.EventLog
import io.github.iammosab.tubeguard.state.PrefsKeyValue
import java.util.concurrent.Executors

/** The app's single set of collaborators, and one background thread every check and change runs on. */
class App : Application() {
    val adb by lazy { LocalAdb(this) }
    val store by lazy { PrefsKeyValue(this) }
    val log by lazy { EventLog(store) }
    val guard by lazy { YouTubeGuard(adb, log) }
    private val worker = Executors.newSingleThreadExecutor()

    fun background(block: () -> Unit) {
        worker.execute { runCatching(block).onFailure { Log.e(LocalAdb.TAG, "background task failed", it) } }
    }

    fun checkNow(trigger: String) = background {
        store.put("lastCheck", System.currentTimeMillis().toString())
        Log.i(LocalAdb.TAG, "check ($trigger): ${guard.enforce(trigger)}")
    }

    companion object {
        fun of(context: Context) = context.applicationContext as App
    }
}
