package io.github.iammosab.tubeguard.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.ui.components.AllowArt
import io.github.iammosab.tubeguard.ui.components.BuildNumberArt
import io.github.iammosab.tubeguard.ui.components.ButtonRow
import io.github.iammosab.tubeguard.ui.components.PairingArt
import io.github.iammosab.tubeguard.ui.components.FocusOnStart
import io.github.iammosab.tubeguard.ui.components.Pill
import io.github.iammosab.tubeguard.ui.components.PillStyle
import io.github.iammosab.tubeguard.ui.components.TopBar
import io.github.iammosab.tubeguard.ui.components.TvStatusArt
import io.github.iammosab.tubeguard.ui.components.UsbDebuggingArt
import io.github.iammosab.tubeguard.ui.theme.SafeH
import io.github.iammosab.tubeguard.ui.theme.SafeV

enum class SetupStep { DEVELOPER, USB, PAIR, ALLOW, DONE }

/** Where a step sits in the three numbered steps (pairing is a side path of step 2). */
private fun SetupStep.number() = when (this) {
    SetupStep.DEVELOPER -> 1
    SetupStep.USB, SetupStep.PAIR -> 2
    SetupStep.ALLOW -> 3
    SetupStep.DONE -> 3
}

/**
 * The illustrated walk-through: turn on Developer options, then USB debugging (or pair with Wireless debugging),
 * then let TubeGuard in. [onConnect] and [onPair] do the work and report back true when it went through.
 */
@Composable
fun SetupScreen(
    tvName: String?,
    startAt: SetupStep,
    onConnect: (done: (ok: Boolean) -> Unit) -> Unit,
    onPair: (port: Int, code: String, done: (ok: Boolean) -> Unit) -> Unit,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(startAt) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val go = { s: SetupStep -> failed = false; step = s }

    Column(Modifier.fillMaxSize().padding(horizontal = SafeH, vertical = SafeV)) {
        TopBar(tvName, title = stringResource(R.string.setup_title))
        Spacer(Modifier.height(20.dp))
        AnimatedContent(
            step, Modifier.weight(1f),
            transitionSpec = { (slideInHorizontally { it / 6 } + fadeIn()) togetherWith fadeOut() },
            label = "step",
        ) { s ->
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                ArtPanel(s, busy, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    if (s != SetupStep.DONE) Progress(s.number())
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(title(s)), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(body(s)), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s == SetupStep.ALLOW && busy) {
                        // A prompt left unanswered earlier stays pending and the TV won't show another one.
                        var slow by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { delay(25_000); slow = true }
                        if (slow) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.setup_no_prompt), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (failed) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(if (s == SetupStep.PAIR) R.string.setup_pair_failed else R.string.setup_connect_failed),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    when (s) {
                        SetupStep.DEVELOPER -> Buttons(
                            primary = R.string.setup_next to { go(SetupStep.USB) },
                        )
                        SetupStep.USB -> Buttons(
                            primary = R.string.setup_next to { go(SetupStep.ALLOW) },
                            secondary = R.string.setup_wireless_only to { go(SetupStep.PAIR) },
                            back = { go(SetupStep.DEVELOPER) },
                        )
                        SetupStep.PAIR -> PairForm(busy, onBack = { go(SetupStep.USB) }) { port, code ->
                            busy = true
                            onPair(port, code) { ok -> busy = false; if (ok) go(SetupStep.ALLOW) else failed = true }
                        }
                        SetupStep.ALLOW -> Buttons(
                            primary = (if (busy) R.string.setup_waiting else R.string.setup_connect) to {
                                if (!busy) {
                                    busy = true
                                    failed = false
                                    onConnect { ok -> busy = false; if (ok) go(SetupStep.DONE) else failed = true }
                                }
                            },
                            back = { if (!busy) go(SetupStep.USB) },
                        )
                        SetupStep.DONE -> Buttons(primary = R.string.setup_finish to onFinish)
                    }
                }
            }
        }
    }
}

private fun title(s: SetupStep) = when (s) {
    SetupStep.DEVELOPER -> R.string.setup_developer_title
    SetupStep.USB -> R.string.setup_usb_title
    SetupStep.PAIR -> R.string.setup_pair_title
    SetupStep.ALLOW -> R.string.setup_allow_title
    SetupStep.DONE -> R.string.setup_done_title
}

private fun body(s: SetupStep) = when (s) {
    SetupStep.DEVELOPER -> R.string.setup_developer_body
    SetupStep.USB -> R.string.setup_usb_body
    SetupStep.PAIR -> R.string.setup_pair_body
    SetupStep.ALLOW -> R.string.setup_allow_body
    SetupStep.DONE -> R.string.setup_done_body
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtPanel(step: SetupStep, busy: Boolean, modifier: Modifier) {
    val done = step == SetupStep.DONE
    val bg by animateColorAsState(
        if (done) androidx.compose.ui.graphics.Color(0xFF1E3A2A) else MaterialTheme.colorScheme.surfaceContainer,
        label = "art",
    )
    Box(modifier.clip(RoundedCornerShape(40.dp)).background(bg), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(320.dp).clip(MaterialShapes.Cookie12Sided.toShape())
                .background(if (done) androidx.compose.ui.graphics.Color(0xFF2C5A3F) else MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        val art = Modifier.fillMaxWidth(0.78f).fillMaxHeight(0.62f)
        when (step) {
            SetupStep.DEVELOPER -> BuildNumberArt(art)
            SetupStep.USB -> UsbDebuggingArt(art)
            SetupStep.PAIR -> PairingArt(art)
            SetupStep.ALLOW -> if (busy) LoadingIndicator(Modifier.size(120.dp)) else AllowArt(art)
            SetupStep.DONE -> TvStatusArt(good = true, modifier = art)
        }
    }
}

/** Three solid segments; the done ones in the accent colour. */
@Composable
private fun Progress(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..3).forEach { n ->
            Box(
                Modifier.width(if (n == current) 48.dp else 28.dp).height(8.dp).clip(CircleShape)
                    .background(if (n <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.setup_step_of, current, 3), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Buttons(primary: Pair<Int, () -> Unit>, secondary: Pair<Int, () -> Unit>? = null, back: (() -> Unit)? = null) {
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = ButtonRow) {
            Pill(stringResource(primary.first), primary.second, Modifier.focusRequester(focus), style = PillStyle.PRIMARY)
            if (back != null) Pill(stringResource(R.string.setup_back), back)
        }
        // The side path sits on its own line, so the main row never runs off the screen.
        if (secondary != null) Pill(stringResource(secondary.first), secondary.second)
    }
    FocusOnStart(focus)
}

@Composable
private fun PairForm(busy: Boolean, onBack: () -> Unit, onPair: (Int, String) -> Unit) {
    var port by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                port, { port = it.filter(Char::isDigit).take(5) }, Modifier.width(170.dp).focusRequester(focus),
                label = { Text(stringResource(R.string.pair_port)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                code, { code = it.filter(Char::isDigit).take(6) }, Modifier.width(200.dp),
                label = { Text(stringResource(R.string.pair_code)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Row(horizontalArrangement = ButtonRow) {
            Pill(
                stringResource(if (busy) R.string.setup_waiting else R.string.pair),
                { port.toIntOrNull()?.let { if (code.length == 6 && !busy) onPair(it, code) } },
                style = PillStyle.PRIMARY,
            )
            Pill(stringResource(R.string.setup_back), onBack)
        }
    }
    FocusOnStart(focus)
}
