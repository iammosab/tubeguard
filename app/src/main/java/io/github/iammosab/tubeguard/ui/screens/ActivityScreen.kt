package io.github.iammosab.tubeguard.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.state.Event
import io.github.iammosab.tubeguard.ui.Tone
import io.github.iammosab.tubeguard.ui.components.FocusOnStart
import io.github.iammosab.tubeguard.ui.components.Pill
import io.github.iammosab.tubeguard.ui.components.TopBar
import io.github.iammosab.tubeguard.ui.components.tvFocus
import io.github.iammosab.tubeguard.ui.describe
import io.github.iammosab.tubeguard.ui.theme.SafeH
import io.github.iammosab.tubeguard.ui.theme.SafeV
import io.github.iammosab.tubeguard.ui.theme.toneColors
import java.util.Date

@Composable
fun ActivityScreen(tvName: String?, events: List<Event>, onBack: () -> Unit) {
    val back = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().padding(horizontal = SafeH, vertical = SafeV)) {
        TopBar(tvName, title = stringResource(R.string.nav_activity))
        Spacer(Modifier.height(20.dp))
        if (events.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.activity_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                items(events) { EventRow(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Pill(stringResource(R.string.setup_back), onBack, Modifier.focusRequester(back))
    }
    FocusOnStart(back)
}

@Composable
private fun EventRow(event: Event) {
    val view = event.describe()
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)
    // Focusable so the remote can scroll through the list.
    Row(
        Modifier.fillMaxWidth().tvFocus(shape, grow = 1.01f).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interaction, indication = null) {}
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(if (view.tone == Tone.NEUTRAL) MaterialTheme.colorScheme.outline else toneColors(view.tone).accent))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(view.title), style = MaterialTheme.typography.titleMedium)
            val detail = view.source?.let { stringResource(R.string.event_brought_back_by, it) } ?: view.detail
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Spacer(Modifier.width(18.dp))
        Text(
            DateFormat.format("d MMM, HH:mm", Date(event.time)).toString(),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
