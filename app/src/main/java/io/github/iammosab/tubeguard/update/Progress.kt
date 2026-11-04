package io.github.iammosab.tubeguard.update

/** Where a download-and-install is. */
sealed class Progress {
    data class Downloading(val fraction: Float) : Progress()
    data object Installing : Progress()
}

/**
 * TizenTube is the point of the app, so while protection is on it's put back if it goes missing; unless the
 * owner removed it themselves with "Uninstall TizenTube", which stands until they choose to install it.
 */
fun shouldInstallTizenTube(protection: String?, installed: String?, removedByOwner: Boolean) =
    protection == "on" && installed == null && !removedByOwner
