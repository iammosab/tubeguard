package io.github.iammosab.tubeguard.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import io.github.iammosab.tubeguard.App
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.AdbKeepAlive
import io.github.iammosab.tubeguard.update.Target
import java.util.Date

/** The one screen: what is protected and how, the log, and the few things the owner can change. */
class MainActivity : Activity() {
    private val app get() = App.of(this)
    private var rendering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        click(R.id.connect) { work { app.setup().also { s -> setupResult(s.joinToString { "${it.name} ${if (it.ok) "✓" else "✗"}" }) } } }
        click(R.id.pair) {
            val port = text(R.id.pairPort).toIntOrNull() ?: return@click
            val code = text(R.id.pairCode)
            work { setupResult("pair: " + if (app.adb.pair(port, code)) "✓" else "✗") }
        }
        click(R.id.updateNow) { work { app.updateTizenTube(app.versionOf(Target.TIZENTUBE.pkg), force = true) } }
        click(R.id.reinstall) { work { app.reinstallTizenTube() } }
        click(R.id.learn) {
            app.keys.startLearning()
            findViewById<TextView>(R.id.button).setText(R.string.learn_prompt)
            findViewById<View>(R.id.learn).postDelayed({ refresh() }, 16_000)
        }
        click(R.id.undo) {
            confirm(R.string.undo_confirm) {
                work { app.undo().also { s -> setupResult(s.joinToString { "${it.name} ${if (it.ok) "✓" else "✗ ${it.detail}"}" }) } }
            }
        }
        click(R.id.playStore) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${YouTubeGuard.YOUTUBE}")))
            }
        }
        click(R.id.uninstall) {
            confirm(R.string.uninstall_confirm) { work { app.adb.run("pm uninstall ${Target.TIZENTUBE.pkg}") } }
        }
        click(R.id.appUpdate) { app.updateAll("manual"); refreshSoon() }
        switch(R.id.takeOver) { on -> app.store.put(KeyPolicy.TAKE_OVER, on.toString()) }
        switch(R.id.keepAdb) { on -> app.store.put(AdbKeepAlive.KEY, on.toString()) }
    }

    override fun onResume() {
        super.onResume()
        app.checkNow("app_open")
        app.updateAll("app_open")
        refresh()
    }

    private fun refresh() = app.background {
        val status = app.status()
        runOnUiThread { if (!isDestroyed) render(status) }
    }

    private fun refreshSoon() = findViewById<View>(R.id.headline).postDelayed({ refresh() }, 5_000)

    /** Runs [block] on the app's background thread with the screen showing it is busy, then refreshes. */
    private fun work(block: () -> Unit) {
        findViewById<TextView>(R.id.headlineDetail).setText(R.string.working)
        app.background {
            block()
            val status = app.status()
            runOnUiThread { if (!isDestroyed) render(status) }
        }
    }

    private fun render(s: Status) {
        rendering = true
        val headline = findViewById<TextView>(R.id.headline)
        val (text, color) = when (s.headline()) {
            Headline.PROTECTED -> R.string.headline_protected to R.color.good
            Headline.NOT_SET_UP -> R.string.headline_not_set_up to R.color.bad
            Headline.NOT_PROTECTED_NO_ADB -> R.string.headline_no_adb to R.color.bad
            Headline.YOUTUBE_PRESENT -> R.string.headline_youtube to R.color.bad
            Headline.OFF -> R.string.headline_off to R.color.off
            Headline.WATCHDOG_OFF -> R.string.headline_watchdog to R.color.bad
            Headline.CHECK_FAILED -> R.string.headline_check_failed to R.color.bad
            Headline.UNDO_INCOMPLETE -> R.string.headline_undo_incomplete to R.color.bad
        }
        headline.setText(text)
        headline.setBackgroundColor(getColor(color))
        val last = s.lastCheck?.let { DateFormat.format("d MMM HH:mm", Date(it)).toString() } ?: getString(R.string.never)
        set(R.id.headlineDetail, getString(R.string.headline_detail, last, s.removals))

        val needsSetup = !s.adbConnected || s.protection != "on"
        findViewById<View>(R.id.setupPanel).visibility = if (needsSetup) View.VISIBLE else View.GONE
        findViewById<View>(R.id.playStore).visibility =
            if (s.headline() == Headline.UNDO_INCOMPLETE) View.VISIBLE else View.GONE

        set(R.id.youtube, getString(when (s.youtubePresent) {
            true -> if (s.protection == "on") R.string.youtube_present_protected else R.string.youtube_present
            false -> R.string.youtube_removed
            null -> R.string.unknown
        }))
        set(R.id.tizentube, getString(
            R.string.tizentube_status, s.tizenTube ?: getString(R.string.not_installed), s.tizenTubeLatest ?: "?",
        ))
        findViewById<View>(R.id.signatureNote).visibility = if (s.signatureMismatch) View.VISIBLE else View.GONE
        findViewById<View>(R.id.reinstall).visibility = if (s.signatureMismatch) View.VISIBLE else View.GONE

        val button = buildList {
            if (s.isTcl) add(getString(if (s.tclMapped) R.string.button_tcl else R.string.button_tcl_not))
            add(if (s.takeOver) getString(R.string.button_a11y_on, s.learnedKey ?: getString(R.string.default_keys))
                else getString(R.string.button_a11y_off))
            if (s.otherKeyServices.isNotEmpty()) {
                add(getString(R.string.button_others, s.otherKeyServices.joinToString { it.substringBefore('/') }))
            }
        }
        set(R.id.button, button.joinToString("\n"))
        findViewById<Switch>(R.id.takeOver).isChecked = s.takeOver
        findViewById<Switch>(R.id.keepAdb).isChecked = s.keepAdbOn

        set(R.id.log, if (s.log.isEmpty()) getString(R.string.log_empty) else s.log.joinToString("\n") {
            "${DateFormat.format("dd MMM HH:mm", Date(it.time))}  ${it.kind}  ${it.detail}"
        })
        set(R.id.about, getString(R.string.about, s.appVersion ?: "?", s.appLatest ?: "?"))
        rendering = false
    }

    private fun setupResult(text: String) = runOnUiThread { set(R.id.setupResult, getString(R.string.setup_done, text)) }

    private fun set(id: Int, text: String) {
        findViewById<TextView>(id).text = text
    }

    private fun text(id: Int) = findViewById<EditText>(id).text.toString().trim()

    private fun click(id: Int, action: () -> Unit) = findViewById<Button>(id).setOnClickListener { action() }

    private fun switch(id: Int, changed: (Boolean) -> Unit) =
        findViewById<Switch>(id).setOnCheckedChangeListener { _, on ->
            if (!rendering) {
                changed(on)
                refresh()
            }
        }

    private fun confirm(message: Int, yes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ -> yes() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
