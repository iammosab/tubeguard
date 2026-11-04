package io.github.iammosab.tubeguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.iammosab.tubeguard.R
import io.github.iammosab.tubeguard.ui.Status
import io.github.iammosab.tubeguard.update.Progress
import io.github.iammosab.tubeguard.ui.components.FocusOnStart
import io.github.iammosab.tubeguard.ui.components.SectionLabel
import io.github.iammosab.tubeguard.ui.components.SettingRow
import io.github.iammosab.tubeguard.ui.components.TopBar
import io.github.iammosab.tubeguard.ui.theme.SafeH
import io.github.iammosab.tubeguard.ui.theme.SafeV

/** Everything the owner can change, in one scrolling list of plain rows. */
class SettingsActions(
    val keepAdb: (Boolean) -> Unit,
    val takeOver: (Boolean) -> Unit,
    val learn: () -> Unit,
    val updateTizenTube: () -> Unit,
    val reinstallTizenTube: () -> Unit,
    val uninstallTizenTube: () -> Unit,
    val checkAppUpdate: () -> Unit,
    val setUpAgain: () -> Unit,
    val undo: () -> Unit,
)

@Composable
fun SettingsScreen(tvName: String?, status: Status?, actions: SettingsActions) {
    val first = remember { FocusRequester() }
    val s = status
    Column(Modifier.fillMaxSize().padding(horizontal = SafeH, vertical = SafeV)) {
        TopBar(tvName, title = stringResource(R.string.nav_settings))
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        ) {
            item { SectionLabel(stringResource(R.string.section_protection)) }
            item {
                Column(Modifier.focusRequester(first)) {
                    SettingRow(
                        stringResource(R.string.keep_adb), stringResource(R.string.keep_adb_detail),
                        checked = s?.keepAdbOn ?: true,
                    ) { actions.keepAdb(!(s?.keepAdbOn ?: true)) }
                }
            }

            item { SectionLabel(stringResource(R.string.section_button)) }
            item {
                val detail = when {
                    s == null -> null
                    s.isTcl && s.tclMapped -> stringResource(R.string.take_over_tcl)
                    s.otherKeyServices.isNotEmpty() ->
                        stringResource(R.string.take_over_others, s.otherKeyServices.joinToString { it.substringBefore('/') })
                    else -> stringResource(R.string.take_over_detail)
                }
                SettingRow(stringResource(R.string.take_over), detail, checked = s?.takeOver ?: false) {
                    actions.takeOver(!(s?.takeOver ?: false))
                }
            }
            item {
                SettingRow(
                    stringResource(R.string.learn),
                    s?.learnedKey?.let { stringResource(R.string.learn_detail_code, it) } ?: stringResource(R.string.learn_detail),
                    onClick = actions.learn,
                )
            }

            item { SectionLabel(stringResource(R.string.tile_tizentube)) }
            item {
                val work = s?.tizenTubeWork
                SettingRow(
                    stringResource(if (s != null && s.tizenTube == null) R.string.install_tizentube else R.string.update_now),
                    when {
                        work is Progress.Downloading -> stringResource(R.string.tile_tizentube_downloading, (work.fraction * 100).toInt())
                        work == Progress.Installing -> stringResource(R.string.tile_tizentube_installing)
                        s != null && s.tizenTube == null -> stringResource(R.string.install_tizentube_detail)
                        else -> stringResource(R.string.version_line, s?.tizenTube ?: "—", s?.tizenTubeLatest?.removePrefix("v") ?: "—")
                    },
                    onClick = { if (work == null) actions.updateTizenTube() },
                )
            }
            if (s?.signatureMismatch == true) {
                item { SettingRow(stringResource(R.string.reinstall), stringResource(R.string.signature_note), danger = true, onClick = actions.reinstallTizenTube) }
            }
            item { SettingRow(stringResource(R.string.uninstall_tizentube), stringResource(R.string.uninstall_detail), onClick = actions.uninstallTizenTube) }

            item { SectionLabel(stringResource(R.string.app_name)) }
            item {
                SettingRow(
                    stringResource(R.string.check_app_update),
                    stringResource(R.string.version_line, s?.appVersion ?: "—", s?.appLatest?.removePrefix("v") ?: "—"),
                    onClick = actions.checkAppUpdate,
                )
            }
            item { SettingRow(stringResource(R.string.set_up_again), stringResource(R.string.set_up_again_detail), onClick = actions.setUpAgain) }
            item { SettingRow(stringResource(R.string.undo), stringResource(R.string.undo_detail), danger = true, onClick = actions.undo) }
        }
    }
    FocusOnStart(first)
}
