package io.github.iammosab.tubeguard.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
    device: DeviceState,
    canOpen: (SettingsPage) -> Boolean,
    onOpen: (SettingsPage) -> Unit,
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
                ArtPanel(s, busy, Modifier.weight(0.85f).fillMaxHeight())
                Column(Modifier.weight(1.15f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    if (s != SetupStep.DONE) Progress(s.number())
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(title(s)), style = MaterialTheme.typography.headlineMedium)
                    path(s)?.let {
                        Spacer(Modifier.height(10.dp))
                        PathLine(stringResource(it))
                    }
                    s.done(device)?.takeIf { it }?.let {
                        Spacer(Modifier.height(12.dp))
                        DoneChip(stringResource(if (s == SetupStep.DEVELOPER) R.string.setup_status_dev_on else R.string.setup_status_usb_on))
                    }
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
                    // Until the TV shows the step as done, the main button opens the right settings page.
                    val page = s.page()?.takeIf { canOpen(it) && s.done(device) != true }
                    val open = page?.let { openLabel(it) to { onOpen(it) } }
                    when (s) {
                        SetupStep.DEVELOPER -> Buttons(
                            primary = open ?: (R.string.setup_next to { go(SetupStep.USB) }),
                            also = if (open != null) R.string.setup_next to { go(SetupStep.USB) } else null,
                        )
                        SetupStep.USB -> Buttons(
                            primary = open ?: (R.string.setup_next to { go(SetupStep.ALLOW) }),
                            also = if (open != null) R.string.setup_next to { go(SetupStep.ALLOW) } else null,
                            secondary = R.string.setup_wireless_only to { go(SetupStep.PAIR) },
                            back = { go(SetupStep.DEVELOPER) },
                        )
                        SetupStep.PAIR -> PairForm(
                            busy, onBack = { go(SetupStep.USB) },
                            open = SettingsPage.DEVELOPER_OPTIONS.takeIf(canOpen)?.let { { onOpen(it) } },
                        ) { port, code ->
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

private fun path(s: SetupStep) = when (s) {
    SetupStep.DEVELOPER -> R.string.setup_path_developer
    SetupStep.USB -> R.string.setup_path_usb
    SetupStep.PAIR -> R.string.setup_path_pair
    else -> null
}

private fun openLabel(page: SettingsPage) = when (page) {
    SettingsPage.ABOUT -> R.string.setup_open_about
    SettingsPage.DEVELOPER_OPTIONS -> R.string.setup_open_developer
}

/** "Settings › System › About › Build" as small solid chips, so the way there can be seen at a glance. */
@Composable
private fun PathLine(path: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        path.split(" › ").forEachIndexed { i, part ->
            if (i > 0) Text("›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                part, style = MaterialTheme.typography.labelMedium,
                color = if (i == path.split(" › ").lastIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clip(CircleShape)
                    .background(if (i == path.split(" › ").lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DoneChip(text: String) {
    Text(
        "✓  $text", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color(0xFFE6F4EA),
        modifier = Modifier.clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFF1E3A2A))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
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
        // Each picture acts its step out on a loop: a few seconds, then a short pause on the finished state.
        val t by rememberInfiniteTransition(label = "art").animateFloat(
            0f, 1f, infiniteRepeatable(tween(4_800, easing = LinearEasing)), label = "t",
        )
        when (step) {
            SetupStep.DEVELOPER -> BuildNumberArt(t, art)
            SetupStep.USB -> UsbDebuggingArt(t, art)
            SetupStep.PAIR -> PairingArt(t, art)
            SetupStep.ALLOW -> if (busy) LoadingIndicator(Modifier.size(120.dp)) else AllowArt(t, art)
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
private fun Buttons(
    primary: Pair<Int, () -> Unit>,
    also: Pair<Int, () -> Unit>? = null,
    secondary: Pair<Int, () -> Unit>? = null,
    back: (() -> Unit)? = null,
) {
    // Keyed on the main button, so focus moves to "Done, next" once the TV shows the step as done.
    val focus = remember(primary.first) { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = ButtonRow) {
            Pill(stringResource(primary.first), primary.second, Modifier.focusRequester(focus), style = PillStyle.PRIMARY)
            if (also != null) Pill(stringResource(also.first), also.second)
            if (back != null && also == null) Pill(stringResource(R.string.setup_back), back)
        }
        // The side path (and Back, when the main row is full) sit on their own line, so nothing runs off the screen.
        if (secondary != null || (back != null && also != null)) {
            Row(horizontalArrangement = ButtonRow) {
                if (secondary != null) Pill(stringResource(secondary.first), secondary.second)
                if (back != null && also != null) Pill(stringResource(R.string.setup_back), back)
            }
        }
    }
    FocusOnStart(focus)
}

@Composable
private fun PairForm(busy: Boolean, onBack: () -> Unit, open: (() -> Unit)?, onPair: (Int, String) -> Unit) {
    var port by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    // Focus starts on "Open Developer options" (the code is found there first); without that, on the port field.
    val openFocus = remember { FocusRequester() }
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
            if (open != null) Pill(stringResource(R.string.setup_open_developer), open, Modifier.focusRequester(openFocus))
            Pill(stringResource(R.string.setup_back), onBack)
        }
    }
    FocusOnStart(if (open != null) openFocus else focus)
}
