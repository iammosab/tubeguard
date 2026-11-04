package io.github.iammosab.tubeguard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.iammosab.tubeguard.ui.theme.FocusRing

/**
 * What the remote is on: a white ring and a slight grow, the way the TV's own screens show it. Every focusable
 * thing on these screens wears it, so focus is never lost from sight.
 */
@Composable
fun Modifier.tvFocus(shape: Shape, grow: Float = 1.05f): Modifier {
    // Watches the focus state, not focus events: an event sent before the screen started listening (focus put
    // there as it opens) would be missed and leave focus invisible.
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) grow else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "focus")
    return this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(3.dp, FocusRing, shape) else Modifier)
}

/**
 * Puts the remote's focus on [requester] when a screen opens. It waits for the screen to be laid out (a screen
 * that slides in isn't attached on its first frame), and tries a few frames before giving up.
 */
@Composable
fun FocusOnStart(requester: FocusRequester) {
    LaunchedEffect(requester) {
        repeat(5) {
            withFrameNanos { }
            if (runCatching { requester.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
}
