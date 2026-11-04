package io.github.iammosab.tubeguard.shell

import android.content.Context
import android.util.Log
import io.github.muntashirakon.adb.AdbStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** [Shell] over the TV's own adbd: 127.0.0.1:5555 first, then whatever port mDNS advertises (Wireless debugging). */
class LocalAdb(private val context: Context) : Shell {
    private val adb get() = AdbKeys.manager(context)

    val connected: Boolean get() = runCatching { adb.isConnected }.getOrDefault(false)

    /** Connects if not already connected; false (never an exception) when the TV's ADB can't be reached. */
    @Synchronized
    fun connect(): Boolean {
        if (connected) return true
        val ok = attempt("tcp 5555") { adb.connect("127.0.0.1", 5555) } ||
            attempt("mdns") { adb.autoConnect(context.applicationContext, 5_000) }
        Log.i(TAG, "connect: $ok")
        return ok
    }

    /** Pairs with Wireless debugging's "Pair device with pairing code" ([port] and the six-digit [code]). */
    fun pair(port: Int, code: String): Boolean = attempt("pair") { adb.pair("127.0.0.1", port, code) }

    @Synchronized
    override fun run(cmd: String): ShellResult {
        for (tryNo in 0..1) {
            if (!connect()) return ShellResult(NO_ADB, "ADB not connected")
            try {
                val raw = readAll("shell:($cmd) 2>&1; echo $EXIT\$?")
                val at = raw.lastIndexOf(EXIT)
                if (at < 0) return ShellResult(1, raw.trim())
                val exit = raw.substring(at + EXIT.length).trim().toIntOrNull() ?: 1
                return ShellResult(exit, raw.substring(0, at).trimEnd())
            } catch (e: Exception) {
                Log.w(TAG, "run failed (try $tryNo): $cmd", e)
                runCatching { adb.disconnect() }
            }
        }
        return ShellResult(NO_ADB, "ADB connection lost")
    }

    @Synchronized
    override fun install(apk: File): ShellResult {
        if (!connect()) return ShellResult(NO_ADB, "ADB not connected")
        return try {
            adb.openStream("exec:cmd package install -S ${apk.length()}").use { stream ->
                stream.openOutputStream().let { out -> apk.inputStream().use { it.copyTo(out) }; out.flush() }
                val text = readUntil(stream, INSTALL_TIMEOUT_MS) { INSTALL_DONE.containsMatchIn(it) }.trim()
                ShellResult(if (text.contains("Success")) 0 else 1, text)
            }
        } catch (e: Exception) {
            Log.w(TAG, "install failed: ${apk.name}", e)
            runCatching { adb.disconnect() }
            ShellResult(NO_ADB, "ADB connection lost: ${e.message}")
        }
    }

    private fun readAll(service: String): String = adb.openStream(service).use { stream ->
        readUntil(stream, RUN_TIMEOUT_MS) { EXIT_LINE.containsMatchIn(it) }
    }.replace("\r", "")

    /**
     * Reads until [done] holds for what has arrived, the stream ends, or [timeoutMs] passes. libadb-android can
     * miss a close that comes in while a read is waiting and then wait forever, so callers say what the end of the
     * output looks like instead of relying on the close; and it reports an ordinary end as "Stream closed.".
     */
    private fun readUntil(stream: AdbStream, timeoutMs: Long, done: (String) -> Boolean): String {
        val reader = Thread.currentThread()
        val alarm = timer.schedule({ reader.interrupt() }, timeoutMs, TimeUnit.MILLISECONDS)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        try {
            val input = stream.openInputStream()
            while (!done(out.toString(Charsets.UTF_8.name()))) {
                val n = try {
                    input.read(buf, 0, buf.size)
                } catch (e: IOException) {
                    if (e.message == "Stream closed.") -1 else throw e
                }
                if (n < 0) break
                out.write(buf, 0, n)
            }
        } finally {
            alarm.cancel(false)
            Thread.interrupted()
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun attempt(what: String, block: () -> Boolean): Boolean = try {
        block()
    } catch (e: Exception) {
        if (e is InterruptedException) Thread.currentThread().interrupt()
        Log.i(TAG, "$what: ${e.javaClass.simpleName}: ${e.message}")
        false
    }

    companion object {
        const val TAG = "TubeGuard"
        /** Exit code for "never reached the TV's ADB", so callers can tell it from a failed command. */
        const val NO_ADB = -100
        private const val EXIT = "__EXIT:"
        private val EXIT_LINE = Regex("__EXIT:-?\\d+\\s")
        private val INSTALL_DONE = Regex("(Success|Failure[^\\n]*)\\s")
        private const val RUN_TIMEOUT_MS = 60_000L
        private const val INSTALL_TIMEOUT_MS = 300_000L
        private val timer = Executors.newSingleThreadScheduledExecutor()
    }
}
