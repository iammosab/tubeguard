package io.github.iammosab.tubeguard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.iammosab.tubeguard.ui.Text

@Composable
fun Text.resolve(): String = stringResource(res, *args.toTypedArray())
