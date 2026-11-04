package io.github.iammosab.tubeguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class PillStyle { PRIMARY, TONAL }

/** The app's one button: a solid pill, focus-ringed. */
@Composable
fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: PillStyle = PillStyle.TONAL, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (style) {
        PillStyle.PRIMARY -> colors.primary to colors.onPrimary
        PillStyle.TONAL -> colors.surfaceContainerHigh to colors.onSurface
    }
    Box(
        modifier
            .tvFocus(CircleShape)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            // Stays clickable (and so focusable) while disabled, so focus can land on it before it becomes usable.
            .clickable(interaction, indication = null) { if (enabled) onClick() }
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) fg else fg.copy(alpha = 0.5f))
    }
}

/** A row in a settings list: title, optional detail, and either a switch or nothing (then it acts as a button). */
@Composable
fun SettingRow(title: String, detail: String? = null, checked: Boolean? = null, danger: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .tvFocus(shape, grow = 1.02f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.titleMedium,
                color = if (danger) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Spacer(Modifier.height(2.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (checked != null) {
            Spacer(Modifier.width(16.dp))
            // The row takes the click and the focus; the switch only shows the state.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/** A small solid square holding an icon, tinted by status. */
@Composable
fun IconBadge(background: Color, content: @Composable () -> Unit) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(background), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}

val ButtonRow = Arrangement.spacedBy(14.dp)
