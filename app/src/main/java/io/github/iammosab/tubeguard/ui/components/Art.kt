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

/** Step 1: About → Build, pressed seven times. */
@Composable
fun BuildNumberArt(modifier: Modifier = Modifier) = Box(modifier) {
    Canvas(Modifier.fillMaxSize()) { buildNumber() }
    // "×7" sits on the badge drawn at (84, 4) on the 120 x 90 grid.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val u = minOf(maxWidth / 120f, maxHeight / 90f)
        val dx = (maxWidth - u * 120f) / 2
        val dy = (maxHeight - u * 90f) / 2
        Box(
            Modifier.offset(dx + u * 84f, dy + u * 4f).size(u * 22f, u * 12f),
            contentAlignment = Alignment.Center,
        ) {
            Text("×7", color = White, fontWeight = FontWeight.Bold, fontSize = (u.value * 8f).sp)
        }
    }
}

private fun DrawScope.buildNumber() {
    grid { u ->
        box(u, 8f, 6f, 72f, 78f, 10f, Panel)
        listOf(14f, 28f, 42f).forEach { y -> box(u, 14f, y, 60f, 10f, 5f, Row) }
        box(u, 14f, 58f, 60f, 14f, 7f, Accent)
        box(u, 20f, 63f, 26f, 4f, 2f, Color(0xFF561E17))
        // The remote, with "x7" on its centre button.
        box(u, 88f, 18f, 24f, 62f, 12f, Ink)
        drawCircle(Accent, 7 * u, Offset(100 * u, 36 * u))
        drawCircle(Panel, 2.2f * u, Offset(100 * u, 56 * u))
        drawCircle(Panel, 2.2f * u, Offset(100 * u, 66 * u))
        box(u, 84f, 4f, 22f, 12f, 6f, StatusGreen)
    }
}

/** Step 2: Developer options → USB debugging, switched on. */
@Composable
fun UsbDebuggingArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        box(u, 10f, 6f, 100f, 78f, 10f, Panel)
        box(u, 18f, 16f, 50f, 6f, 3f, Row)
        listOf(32f, 60f).forEach { y -> box(u, 18f, y, 84f, 14f, 7f, Row) }
        box(u, 18f, 46f, 84f, 14f, 7f, Accent)
        box(u, 24f, 51f, 34f, 4f, 2f, Color(0xFF561E17))
        // The switch, on.
        box(u, 80f, 49f, 16f, 8f, 4f, StatusGreen)
        drawCircle(White, 3.2f * u, Offset(92 * u, 53 * u))
    }
}

/** Step 3: the "Allow USB debugging?" prompt with "Always allow" ticked and Allow chosen. */
@Composable
fun AllowArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        box(u, 8f, 10f, 104f, 70f, 12f, Panel)
        box(u, 18f, 20f, 60f, 6f, 3f, Ink)
        box(u, 18f, 31f, 80f, 4f, 2f, Row)
        // Always allow, ticked.
        box(u, 18f, 42f, 10f, 10f, 2.5f, StatusGreen)
        tick(u, listOf(20.5f to 47f, 22.5f to 49f, 26f to 44.5f), White, 1.8f)
        box(u, 32f, 45f, 44f, 4f, 2f, Ink)
        box(u, 18f, 60f, 34f, 12f, 6f, White)
        box(u, 26f, 64.5f, 18f, 3f, 1.5f, Panel)
        box(u, 58f, 60f, 34f, 12f, 6f, Row)
        // The remote's focus on Allow.
        drawRoundRect(
            Accent, Offset(15.5f * u, 57.5f * u), Size(39 * u, 17 * u), CornerRadius(8.5f * u),
            style = Stroke(2f * u),
        )
    }
}

/** Wireless debugging: a pairing code on screen. */
@Composable
fun PairingArt(modifier: Modifier = Modifier) = Canvas(modifier) {
    grid { u ->
        box(u, 10f, 8f, 100f, 74f, 12f, Panel)
        box(u, 20f, 18f, 48f, 6f, 3f, Ink)
        listOf(20f, 41f, 62f, 83f).forEachIndexed { i, x ->
            box(u, x, 36f, 17f, 22f, 6f, if (i == 0) Accent else Row)
        }
        box(u, 20f, 66f, 60f, 4f, 2f, Row)
    }
}
