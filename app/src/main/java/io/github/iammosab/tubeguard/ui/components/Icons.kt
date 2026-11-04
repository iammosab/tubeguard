package io.github.iammosab.tubeguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.ui.TileKind
import io.github.iammosab.tubeguard.ui.Tone
import io.github.iammosab.tubeguard.ui.theme.toneColors

// Small line icons on a 24 x 24 grid.

private fun DrawScope.line(color: Color, w: Float, vararg pts: Pair<Float, Float>) {
    val u = size.minDimension / 24f
    val p = Path().apply {
        moveTo(pts[0].first * u, pts[0].second * u)
        pts.drop(1).forEach { lineTo(it.first * u, it.second * u) }
    }
    drawPath(p, color, style = Stroke(w * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
fun TileIcon(kind: TileKind, tone: Tone, modifier: Modifier = Modifier) {
    val accent = toneColors(tone).accent
    val ink = MaterialTheme.colorScheme.onSurface
    val warm = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val u = size.minDimension / 24f
        when (kind) {
            TileKind.YOUTUBE -> when (tone) {
                Tone.GOOD -> line(accent, 3f, 5f to 12.5f, 10f to 17.5f, 19f to 7.5f)
                Tone.BAD -> {
                    line(accent, 3f, 12f to 6f, 12f to 13.5f)
                    drawCircle(accent, 1.8f * u, Offset(12 * u, 18 * u))
                }
                Tone.NEUTRAL -> line(accent, 3f, 6f to 12f, 18f to 12f)
            }
            TileKind.TIZENTUBE -> drawPath(
                Path().apply { moveTo(8 * u, 5 * u); lineTo(19 * u, 12 * u); lineTo(8 * u, 19 * u); close() }, warm,
            )
            TileKind.BUTTON -> {
                drawRoundRect(ink, Offset(7 * u, 2.5f * u), Size(10 * u, 19 * u), CornerRadius(5 * u), style = Stroke(2 * u))
                drawCircle(warm, 2.3f * u, Offset(12 * u, 8.5f * u))
            }
            TileKind.LAST_CHECK -> {
                drawCircle(ink, 8.5f * u, Offset(12 * u, 12 * u), style = Stroke(2 * u))
                line(ink, 2f, 12f to 7.5f, 12f to 12f, 15f to 14f)
            }
        }
    }
}

@Composable
fun TopBar(tvName: String?, title: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            title ?: stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        if (tvName != null) {
            Text(
                tvName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
