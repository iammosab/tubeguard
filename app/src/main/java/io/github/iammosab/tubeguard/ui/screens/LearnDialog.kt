package io.github.iammosab.tubeguard.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.ui.components.FocusOnStart
import io.github.iammosab.tubeguard.ui.components.Pill
import io.github.iammosab.tubeguard.ui.components.PillStyle
import io.github.iammosab.tubeguard.ui.components.RemoteArt
import io.github.iammosab.tubeguard.ui.components.RemoteMood
import io.github.iammosab.tubeguard.ui.theme.toneColors
import io.github.iammosab.tubeguard.ui.Tone
import kotlinx.coroutines.delay

enum class LearnPhase { PREPARING, LISTENING, LEARNED, FAILED }

/**
 * Learning the YouTube button, told in pictures: the other bindings being paused, the remote's button pulsing
 * while TubeGuard listens (with the time left), then the caught key and what it now opens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LearnDialog(
    phase: LearnPhase,
    learnedKey: Int?,
    paused: List<String>,
    windowMs: Long,
    onClose: () -> Unit,
    onRetry: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val t by rememberInfiniteTransition(label = "remote").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1_600, easing = LinearEasing)), label = "t",
        )
        Row(
            Modifier.width(820.dp).height(400.dp).clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            val good = phase == LearnPhase.LEARNED
            val bad = phase == LearnPhase.FAILED
            Box(
                Modifier.weight(0.8f).fillMaxHeight().clip(RoundedCornerShape(32.dp))
                    .background(
                        when {
                            good -> toneColors(Tone.GOOD).container
                            bad -> toneColors(Tone.BAD).container
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(260.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(
                        when {
                            good -> toneColors(Tone.GOOD).shape
                            bad -> toneColors(Tone.BAD).shape
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                )
                RemoteArt(
                    when (phase) {
                        LearnPhase.PREPARING -> RemoteMood.PAUSING
                        LearnPhase.LISTENING -> RemoteMood.LISTENING
                        LearnPhase.LEARNED -> RemoteMood.LEARNED
                        LearnPhase.FAILED -> RemoteMood.FAILED
                    },
                    t, Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.8f),
                )
            }
            Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Steps(phase)
                Spacer(Modifier.height(18.dp))
                AnimatedContent(phase, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "text") { p ->
                    Column {
                        Text(stringResource(titleOf(p)), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when (p) {
                                LearnPhase.LEARNED -> stringResource(R.string.learn_done_body)
                                else -> stringResource(bodyOf(p))
                            },
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                when (phase) {
                    LearnPhase.PREPARING -> Chips(paused, dim = true)
                    LearnPhase.LISTENING -> Countdown(windowMs)
                    LearnPhase.LEARNED -> Chips(listOf(stringResource(R.string.learn_key_chip, learnedKey ?: 0)), dim = false, good = true)
                    LearnPhase.FAILED -> {}
                }
                Spacer(Modifier.height(24.dp))
                val focus = remember(phase) { FocusRequester() }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (phase) {
                        LearnPhase.LEARNED -> Pill(stringResource(R.string.done), onClose, Modifier.focusRequester(focus), style = PillStyle.PRIMARY)
                        LearnPhase.FAILED -> {
                            Pill(stringResource(R.string.learn_retry), onRetry, Modifier.focusRequester(focus), style = PillStyle.PRIMARY)
                            Pill(stringResource(R.string.cancel), onClose)
                        }
                        else -> Pill(stringResource(R.string.cancel), onClose, Modifier.focusRequester(focus))
                    }
                }
                FocusOnStart(focus)
            }
        }
    }
}

private fun titleOf(p: LearnPhase) = when (p) {
    LearnPhase.PREPARING -> R.string.learn_title_preparing
    LearnPhase.LISTENING -> R.string.learn_title_listening
    LearnPhase.LEARNED -> R.string.learn_title_done
    LearnPhase.FAILED -> R.string.learn_title_failed
}

private fun bodyOf(p: LearnPhase) = when (p) {
    LearnPhase.PREPARING -> R.string.learn_preparing
    LearnPhase.LISTENING -> R.string.learn_prompt
    LearnPhase.LEARNED -> R.string.learn_done_body
    LearnPhase.FAILED -> R.string.learn_failed
}

/** Pause › Press › Restore, the current one in the accent colour and the finished ones ticked. */
@Composable
private fun Steps(phase: LearnPhase) {
    val current = when (phase) {
        LearnPhase.PREPARING, LearnPhase.FAILED -> 0
        LearnPhase.LISTENING -> 1
        LearnPhase.LEARNED -> 2
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(R.string.learn_step_pause, R.string.learn_step_press, R.string.learn_step_restore).forEachIndexed { i, label ->
            val done = i < current || phase == LearnPhase.LEARNED
            val now = i == current && phase != LearnPhase.LEARNED
            Text(
                (if (done) "✓ " else "") + stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = if (now) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clip(CircleShape)
                    .background(
                        when {
                            now -> MaterialTheme.colorScheme.primary
                            done -> toneColors(Tone.GOOD).container
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Chips(items: List<String>, dim: Boolean, good: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach {
            Text(
                (if (dim) "⏸  " else "") + it, style = MaterialTheme.typography.labelLarge,
                color = if (good) Color(0xFFE6F4EA) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape)
                    .background(if (good) toneColors(Tone.GOOD).container else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** The time left to press the button, as an expressive wavy bar running down. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Countdown(windowMs: Long) {
    var left by remember { mutableLongStateOf(windowMs) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (left > 0) {
            delay(100)
            left = (windowMs - (System.currentTimeMillis() - start)).coerceAtLeast(0)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearWavyProgressIndicator(progress = { left / windowMs.toFloat() }, modifier = Modifier.fillMaxWidth(0.9f))
        Text(
            stringResource(R.string.learn_seconds_left, ((left + 999) / 1000).toInt()),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
