package io.github.iammosab.tubeguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.iammosab.tubeguard.ui.theme.StatusGreen
import io.github.iammosab.tubeguard.ui.theme.StatusRed

// Flat, simple illustrations drawn on a 120 x 90 grid and scaled to fit, so they stay sharp on any TV.

private val Ink = Color(0xFFE6E1E3)
private val Panel = Color(0xFF2B2A2C)
private val Row = Color(0xFF3A383B)
private val Accent = Color(0xFFFFB4A9)
private val White = Color.White

private fun DrawScope.grid(block: DrawScope.(u: Float) -> Unit) {
    val u = minOf(size.width / 120f, size.height / 90f)
    val dx = (size.width - 120 * u) / 2
    val dy = (size.height - 90 * u) / 2
    drawContext.transform.translate(dx, dy)
    block(u)
    drawContext.transform.translate(-dx, -dy)
}

private fun DrawScope.box(u: Float, x: Float, y: Float, w: Float, h: Float, r: Float, c: Color) =
    drawRoundRect(c, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(r * u))

private fun DrawScope.tv(u: Float) {
    box(u, 6f, 8f, 108f, 64f, 12f, Ink)
    box(u, 44f, 76f, 32f, 6f, 3f, Ink)
}

private fun DrawScope.shield(u: Float, cx: Float, cy: Float, s: Float, color: Color) {
    val p = Path().apply {
        moveTo((cx) * u, (cy - 20 * s) * u)
        lineTo((cx + 17 * s) * u, (cy - 13 * s) * u)
        lineTo((cx + 17 * s) * u, (cy - 2 * s) * u)
        cubicTo((cx + 17 * s) * u, (cy + 9 * s) * u, (cx + 9.5f * s) * u, (cy + 16 * s) * u, cx * u, (cy + 19 * s) * u)
        cubicTo((cx - 9.5f * s) * u, (cy + 16 * s) * u, (cx - 17 * s) * u, (cy + 9 * s) * u, (cx - 17 * s) * u, (cy - 2 * s) * u)
        lineTo((cx - 17 * s) * u, (cy - 13 * s) * u)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.tick(u: Float, points: List<Pair<Float, Float>>, color: Color, width: Float) {
    val p = Path().apply {
        moveTo(points[0].first * u, points[0].second * u)
        points.drop(1).forEach { lineTo(it.first * u, it.second * u) }
    }
    drawPath(p, color, style = Stroke(width * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** The TV with a green shield (protected), a warm one (not set up or off) or a red warning (needs you). */
@Composable
fun TvStatusArt(good: Boolean, modifier: Modifier = Modifier, inviting: Boolean = false) = Canvas(modifier) {
    grid { u ->
        tv(u)
        if (good) {
            shield(u, 60f, 40f, 1f, if (inviting) Accent else StatusGreen)
            tick(u, listOf(52.5f to 39.5f, 58f to 45f, 68f to 34f), White, 4f)
        } else {
            drawCircle(StatusRed, 18 * u, Offset(60 * u, 40 * u))
            box(u, 57f, 29f, 6f, 14f, 3f, White)
            drawCircle(White, 3.2f * u, Offset(60 * u, 49 * u))
        }
    }
}

// The step pictures are animated: [t] runs from 0 to 1 and repeats, and each picture acts out its step.

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f.coerceIn(0f, 1f)
private fun phase(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)
private val Ember = Color(0xFF561E17)
private val Off = Color(0xFF5F5D60)

private fun DrawScope.ring(u: Float, x: Float, y: Float, w: Float, h: Float, alpha: Float = 1f) =
    drawRoundRect(
        White.copy(alpha = alpha), Offset((x - 2.5f) * u, (y - 2.5f) * u), Size((w + 5) * u, (h + 5) * u),
        CornerRadius((h / 2 + 2.5f) * u), style = Stroke(1.8f * u),
    )

/** Step 1: About → Build, pressed seven times: the remote's OK button presses and the count climbs to ×7. */
@Composable
fun BuildNumberArt(t: Float, modifier: Modifier = Modifier) = Box(modifier) {
    val presses = (t / 0.7f * 7).toInt().coerceIn(0, 7)
    val pressing = t < 0.7f && (t / 0.7f * 7) % 1f < 0.35f
    Canvas(Modifier.fillMaxSize()) { buildNumber(presses, pressing) }
    // The count sits on the badge drawn at (84, 4) on the 120 x 90 grid.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val u = minOf(maxWidth / 120f, maxHeight / 90f)
        val dx = (maxWidth - u * 120f) / 2
        val dy = (maxHeight - u * 90f) / 2
        Box(
            Modifier.offset(dx + u * 84f, dy + u * 4f).size(u * 22f, u * 12f),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (presses >= 7) "✓" else "×$presses", color = White, fontWeight = FontWeight.Bold, fontSize = (u.value * 8f).sp)
        }
    }
}

private fun DrawScope.buildNumber(presses: Int, pressing: Boolean) {
    grid { u ->
        box(u, 8f, 6f, 72f, 78f, 10f, Panel)
        listOf(14f, 28f, 42f).forEach { y -> box(u, 14f, y, 60f, 10f, 5f, Row) }
        // "Build", with the remote's focus on it; it flashes with every press.
        box(u, 14f, 58f, 60f, 14f, 7f, if (pressing) White else Accent)
        box(u, 20f, 63f, 26f, 4f, 2f, Ember)
        ring(u, 14f, 58f, 60f, 14f)
        // The remote: its OK button sinks with every press.
        box(u, 88f, 18f, 24f, 62f, 12f, Ink)
        drawCircle(if (pressing) Ember else Accent, (if (pressing) 6f else 7f) * u, Offset(100 * u, 36 * u))
        drawCircle(Panel, 2.2f * u, Offset(100 * u, 56 * u))
        drawCircle(Panel, 2.2f * u, Offset(100 * u, 66 * u))
        box(u, 84f, 4f, 22f, 12f, 6f, if (presses >= 7) StatusGreen else Row)
    }
}

/** Step 2: focus comes down to USB debugging and the switch slides on. */
@Composable
fun UsbDebuggingArt(t: Float, modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        box(u, 10f, 6f, 100f, 78f, 10f, Panel)
        box(u, 18f, 16f, 50f, 6f, 3f, Row)
        listOf(32f, 60f).forEach { y -> box(u, 18f, y, 84f, 14f, 7f, Row) }
        box(u, 18f, 46f, 84f, 14f, 7f, Accent)
        box(u, 24f, 51f, 34f, 4f, 2f, Ember)
        // Focus moves down from the row above, then OK flips the switch.
        val ringY = lerp(32f, 46f, phase(t, 0.1f, 0.3f))
        ring(u, 18f, ringY, 84f, 14f)
        val on = phase(t, 0.45f, 0.6f)
        box(u, 80f, 49f, 16f, 8f, 4f, androidx.compose.ui.graphics.lerp(Off, StatusGreen, on))
        drawCircle(White, 3.2f * u, Offset(lerp(84f, 92f, on) * u, 53 * u))
    }
}

/** Step 3: "Always allow" gets ticked, focus moves to Allow and it is pressed. */
@Composable
fun AllowArt(t: Float, modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        box(u, 8f, 10f, 104f, 70f, 12f, Panel)
        box(u, 18f, 20f, 60f, 6f, 3f, Ink)
        box(u, 18f, 31f, 80f, 4f, 2f, Row)
        val ticked = t > 0.25f
        box(u, 18f, 42f, 10f, 10f, 2.5f, if (ticked) StatusGreen else Off)
        if (ticked) tick(u, listOf(20.5f to 47f, 22.5f to 49f, 26f to 44.5f), White, 1.8f)
        box(u, 32f, 45f, 44f, 4f, 2f, Ink)
        val pressed = t > 0.7f
        box(u, 18f, 60f, 34f, 12f, 6f, if (pressed) StatusGreen else White)
        box(u, 26f, 64.5f, 18f, 3f, 1.5f, if (pressed) White else Panel)
        box(u, 58f, 60f, 34f, 12f, 6f, Row)
        // The remote's focus: first on the checkbox, then down onto Allow.
        val move = phase(t, 0.4f, 0.55f)
        ring(u, lerp(15f, 18f, move), lerp(39f, 60f, move), lerp(64f, 34f, move), lerp(16f, 12f, move))
    }
}

/** Wireless debugging: the six-digit code going in, box by box. */
@Composable
fun PairingArt(t: Float, modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        box(u, 10f, 8f, 100f, 74f, 12f, Panel)
        box(u, 20f, 18f, 48f, 6f, 3f, Ink)
        val filled = (t / 0.8f * 6).toInt().coerceIn(0, 6)
        (0 until 6).forEach { i ->
            val x = 18f + i * 14.5f
            box(u, x, 36f, 12f, 20f, 5f, if (i < filled) Accent else Row)
            if (i == filled.coerceAtMost(5) && filled < 6) ring(u, x, 36f, 12f, 20f)
        }
        box(u, 20f, 66f, 60f, 4f, 2f, if (filled == 6) StatusGreen else Row)
    }
}

enum class RemoteMood { PAUSING, LISTENING, LEARNED, FAILED }

/**
 * The remote, for learning its YouTube button. Pausing: a pause badge. Listening: the button pulses with
 * rings going out from it. Learned: the button turns green with a tick. Failed: a red warning badge.
 */
@Composable
fun RemoteArt(mood: RemoteMood, t: Float, modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        // Body, D-pad ring and OK, and two rows of small keys; the big key below them is the YouTube button.
        box(u, 44f, 4f, 32f, 82f, 16f, Ink)
        drawCircle(Row, 10f * u, Offset(60 * u, 22 * u))
        drawCircle(Ink, 5f * u, Offset(60 * u, 22 * u))
        listOf(52f, 68f).forEach { x -> drawCircle(Off, 2.6f * u, Offset(x * u, 40 * u)) }
        val key = Offset(60 * u, 58 * u)
        when (mood) {
            RemoteMood.LISTENING -> {
                // Three rings leaving the button, one after the other.
                (0 until 3).forEach { i ->
                    val p = (t + i / 3f) % 1f
                    drawCircle(Accent.copy(alpha = (1f - p) * 0.8f), (9f + p * 30f) * u, key, style = Stroke(2f * u))
                }
                drawCircle(Accent, (8.5f + 1.2f * kotlin.math.sin(t * 6.283f * 2)) * u, key)
                drawPath(
                    Path().apply { moveTo(57 * u, 54 * u); lineTo(64.5f * u, 58 * u); lineTo(57 * u, 62 * u); close() }, Ember,
                )
            }
            RemoteMood.LEARNED -> {
                drawCircle(StatusGreen, 10f * u, key)
                tick(u, listOf(55.5f to 58f, 58.5f to 61f, 64.5f to 54.5f), White, 2.6f)
            }
            RemoteMood.PAUSING, RemoteMood.FAILED -> drawCircle(Off, 8.5f * u, key)
        }
        listOf(72f, 79f).forEach { y -> box(u, 52f, y, 16f, 3f, 1.5f, Off) }
        // Badge on the remote's shoulder.
        when (mood) {
            RemoteMood.PAUSING -> {
                drawCircle(Panel, 9f * u, Offset(82 * u, 10 * u))
                box(u, 78.5f, 5.5f, 2.6f, 9f, 1.3f, Accent)
                box(u, 83f, 5.5f, 2.6f, 9f, 1.3f, Accent)
            }
            RemoteMood.FAILED -> {
                drawCircle(StatusRed, 9f * u, Offset(82 * u, 10 * u))
                box(u, 80.8f, 5f, 2.4f, 6.5f, 1.2f, White)
                drawCircle(White, 1.4f * u, Offset(82 * u, 14.4f * u))
            }
            else -> {}
        }
    }
}
