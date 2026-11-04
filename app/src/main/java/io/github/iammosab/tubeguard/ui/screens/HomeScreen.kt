package io.github.iammosab.tubeguard.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.ui.HomeModel
import io.github.iammosab.tubeguard.ui.PrimaryAction
import io.github.iammosab.tubeguard.ui.Tile
import io.github.iammosab.tubeguard.ui.Tone
import io.github.iammosab.tubeguard.ui.components.ButtonRow
import io.github.iammosab.tubeguard.ui.components.IconBadge
import io.github.iammosab.tubeguard.ui.components.FocusOnStart
import io.github.iammosab.tubeguard.ui.components.Pill
import io.github.iammosab.tubeguard.ui.components.PillStyle
import io.github.iammosab.tubeguard.ui.components.TileIcon
import io.github.iammosab.tubeguard.ui.components.TopBar
import io.github.iammosab.tubeguard.ui.components.TvStatusArt
import io.github.iammosab.tubeguard.ui.components.resolve
import io.github.iammosab.tubeguard.ui.theme.SafeH
import io.github.iammosab.tubeguard.ui.theme.SafeV
import io.github.iammosab.tubeguard.ui.theme.toneColors

@Composable
fun HomeScreen(
    model: HomeModel?,
    tvName: String?,
    busy: Boolean,
    onPrimary: (PrimaryAction) -> Unit,
    onActivity: () -> Unit,
    onSettings: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().padding(horizontal = SafeH, vertical = SafeV)) {
        TopBar(tvName)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Hero(model, Modifier.weight(1.05f).fillMaxHeight())
            Tiles(model?.tiles.orEmpty(), Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = ButtonRow, verticalAlignment = Alignment.CenterVertically) {
            val action = model?.primary ?: PrimaryAction.CHECK_NOW
            Pill(
                if (busy) stringResource(R.string.working) else stringResource(primaryLabel(action)),
                onClick = { onPrimary(action) },
                style = PillStyle.PRIMARY,
                enabled = !busy && model != null,
                modifier = Modifier.focusRequester(first),
            )
            Pill(stringResource(R.string.nav_activity), onActivity)
            Pill(stringResource(R.string.nav_settings), onSettings)
        }
    }
    FocusOnStart(first)
}

private fun primaryLabel(action: PrimaryAction) = when (action) {
    PrimaryAction.CHECK_NOW -> R.string.action_check_now
    PrimaryAction.SET_UP -> R.string.action_set_up
    PrimaryAction.FIX -> R.string.action_fix
    PrimaryAction.REMOVE_NOW -> R.string.action_remove_now
    PrimaryAction.REPAIR -> R.string.action_repair
    PrimaryAction.PLAY_STORE -> R.string.action_play_store
    PrimaryAction.TURN_BACK_ON -> R.string.action_turn_back_on
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Hero(model: HomeModel?, modifier: Modifier) {
    val tone = toneColors(model?.tone ?: Tone.NEUTRAL)
    val container by animateColorAsState(tone.container, MaterialTheme.motionScheme.slowEffectsSpec(), label = "hero")
    val shapeColor by animateColorAsState(tone.shape, MaterialTheme.motionScheme.slowEffectsSpec(), label = "shape")
    // The expressive cookie shape turns very slowly behind the picture; calm, but alive.
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )
    val shape = RoundedCornerShape(40.dp)
    Box(modifier.clip(shape).background(container)) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-60).dp).size(300.dp)
                .graphicsLayer { rotationZ = spin }
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(shapeColor),
        )
        Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.SpaceBetween) {
            if (model == null) {
                LoadingIndicator(Modifier.size(72.dp))
            } else {
                TvStatusArt(
                    good = model.tone != Tone.BAD, inviting = model.tone == Tone.NEUTRAL,
                    modifier = Modifier.width(150.dp).height(112.dp),
                )
            }
            Column {
                Text(
                    model?.title?.resolve() ?: stringResource(R.string.loading),
                    style = MaterialTheme.typography.displayMedium, color = tone.onContainer,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    model?.body?.resolve() ?: "", style = MaterialTheme.typography.bodyLarge,
                    color = tone.onContainer.copy(alpha = 0.85f), modifier = Modifier.fillMaxWidth(0.9f),
                )
            }
        }
    }
}

@Composable
private fun Tiles(tiles: List<Tile>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { StatusTile(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun StatusTile(tile: Tile, modifier: Modifier) {
    val tone = toneColors(tile.tone)
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconBadge(if (tile.tone == Tone.NEUTRAL) MaterialTheme.colorScheme.surfaceContainerHigh else tone.container) {
            TileIcon(tile.kind, tile.tone, Modifier.size(22.dp))
        }
        Column {
            Text(stringResource(tile.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(tile.value.resolve(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            tile.support?.let {
                Text(it.resolve(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
