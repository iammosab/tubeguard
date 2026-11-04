package io.github.iammosab.tubeguard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.iammosab.tubeguard.App
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.button.KeyPolicy
import io.github.iammosab.tubeguard.guard.YouTubeGuard
import io.github.iammosab.tubeguard.shell.AdbKeepAlive
import io.github.iammosab.tubeguard.update.Target
import io.github.iammosab.tubeguard.ui.screens.ActivityScreen
import io.github.iammosab.tubeguard.ui.screens.DeviceState
import io.github.iammosab.tubeguard.ui.screens.intent
import io.github.iammosab.tubeguard.ui.screens.readDeviceState
import io.github.iammosab.tubeguard.ui.screens.HomeScreen
import io.github.iammosab.tubeguard.ui.screens.SettingsActions
import io.github.iammosab.tubeguard.ui.screens.SettingsScreen
import io.github.iammosab.tubeguard.ui.screens.SetupScreen
import io.github.iammosab.tubeguard.ui.screens.SetupStep
import io.github.iammosab.tubeguard.ui.theme.TubeGuardTheme
import kotlinx.coroutines.delay

private sealed interface Screen {
    data object Home : Screen
    data class Setup(val from: SetupStep) : Screen
    data object Activity : Screen
    data object Settings : Screen
}

private enum class Ask { UNDO, UNINSTALL, REINSTALL, LEARN }

/** The one activity: home, the setup walk-through, activity and settings, all driven by the remote. */
class MainActivity : ComponentActivity() {
    private val app get() = App.of(this)
    private var status by mutableStateOf<Status?>(null)
    private var busy by mutableStateOf(false)
    private var screen by mutableStateOf<Screen>(Screen.Home)
    private var ask by mutableStateOf<Ask?>(null)
    /** Read again whenever the owner comes back from the TV's settings. */
    private var device by mutableStateOf(DeviceState(developerOptions = false, usbDebugging = false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tvName = runCatching { Settings.Global.getString(contentResolver, "device_name") }.getOrNull()
        setContent {
            TubeGuardTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Content(tvName)
                    Dialogs()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        device = readDeviceState(this)
        app.checkNow("app_open")
        app.updateAll("app_open")
        refresh()
    }

    private fun refresh() = app.background {
        val s = app.status()
        runOnUiThread { status = s }
    }

    /** Runs [block] on the app's background thread, shows it is busy, then refreshes. */
    private fun work(block: () -> Unit) {
        busy = true
        app.background {
            block()
            val s = app.status()
            runOnUiThread { status = s; busy = false }
        }
    }

    @Composable
    private fun Content(tvName: String?) {
        BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
        when (val current = screen) {
            Screen.Home -> HomeScreen(
                model = status?.home(System.currentTimeMillis()),
                tvName = tvName,
                busy = busy,
                onPrimary = ::primary,
                onActivity = { screen = Screen.Activity },
                onSettings = { screen = Screen.Settings },
            )
            is Screen.Setup -> SetupScreen(
                tvName = tvName,
                startAt = current.from,
                onConnect = { done ->
                    app.background {
                        val ok = app.setup().all { it.ok }
                        val s = app.status()
                        runOnUiThread { status = s; done(ok) }
                    }
                },
                onPair = { port, code, done ->
                    app.background {
                        val ok = app.adb.pair(port, code)
                        runOnUiThread { done(ok) }
                    }
                },
                onFinish = { screen = Screen.Home; refresh() },
                device = device,
                canOpen = { it.intent(this) != null },
                onOpen = { page -> page.intent(this)?.let { runCatching { startActivity(it) } } },
            )
            Screen.Activity -> ActivityScreen(tvName, status?.log.orEmpty()) { screen = Screen.Home }
            Screen.Settings -> SettingsScreen(tvName, status, settingsActions())
        }
    }

    private fun primary(action: PrimaryAction) {
        when (action) {
            PrimaryAction.CHECK_NOW, PrimaryAction.REMOVE_NOW, PrimaryAction.REPAIR -> work { app.checkNowBlocking("manual") }
            PrimaryAction.SET_UP, PrimaryAction.FIX -> screen = Screen.Setup(SetupStep.DEVELOPER)
            PrimaryAction.TURN_BACK_ON -> screen = Screen.Setup(SetupStep.ALLOW)
            PrimaryAction.PLAY_STORE -> runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${YouTubeGuard.YOUTUBE}")))
            }
        }
    }

    private fun settingsActions() = SettingsActions(
        keepAdb = { on -> app.store.put(AdbKeepAlive.KEY, on.toString()); refresh() },
        takeOver = { on -> app.store.put(KeyPolicy.TAKE_OVER, on.toString()); refresh() },
        learn = { app.keys.startLearning(); ask = Ask.LEARN },
        updateTizenTube = { work { app.updateTizenTube(app.versionOf(Target.TIZENTUBE.pkg), force = true) } },
        reinstallTizenTube = { ask = Ask.REINSTALL },
        uninstallTizenTube = { ask = Ask.UNINSTALL },
        checkAppUpdate = { app.updateAll("manual") },
        setUpAgain = { screen = Screen.Setup(SetupStep.DEVELOPER) },
        undo = { ask = Ask.UNDO },
    )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun Dialogs() {
        val dismiss = { ask = null }
        when (ask) {
            Ask.UNDO -> Confirm(R.string.undo, R.string.undo_confirm, R.string.undo, dismiss) {
                work { app.undo() }
                screen = Screen.Home
            }
            Ask.UNINSTALL -> Confirm(R.string.uninstall_tizentube, R.string.uninstall_confirm, R.string.uninstall, dismiss) {
                work { app.adb.run("pm uninstall ${Target.TIZENTUBE.pkg}") }
            }
            Ask.REINSTALL -> Confirm(R.string.reinstall, R.string.signature_note, R.string.reinstall_go, dismiss) {
                work { app.reinstallTizenTube() }
            }
            Ask.LEARN -> {
                // Closes itself once the key is caught, or after the 15 seconds learning lasts.
                LaunchedEffect(Unit) {
                    while (app.keys.learning) delay(300)
                    ask = null
                    refresh()
                }
                AlertDialog(
                    onDismissRequest = dismiss,
                    icon = { LoadingIndicator(Modifier.size(56.dp)) },
                    title = { Text(stringResource(R.string.learn)) },
                    text = { Text(stringResource(R.string.learn_prompt)) },
                    confirmButton = {},
                )
            }
            null -> {}
        }
    }

    @Composable
    private fun Confirm(title: Int, text: Int, yes: Int, dismiss: () -> Unit, onYes: () -> Unit) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(text)) },
            confirmButton = { TextButton({ dismiss(); onYes() }) { Text(stringResource(yes)) } },
            dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
