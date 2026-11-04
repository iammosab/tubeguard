package io.github.iammosab.tubeguard

import io.github.iammosab.tubeguard.access.AccessibilitySetting

/** Tests don't wait for the TV; touched from the fakes every test class uses. */
internal val noPauses = run { AccessibilitySetting.rebindPauseMs = 0; Unit }
