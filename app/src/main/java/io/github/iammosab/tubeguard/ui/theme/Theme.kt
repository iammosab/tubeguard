package io.github.iammosab.tubeguard.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.iammosab.tubeguard.ui.Tone

/** Dark, like the TV's own screens: solid blocks, one warm accent, green and red only for status. */
private val Colors = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF561E17),
    primaryContainer = Color(0xFF73332B),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondaryContainer = Color(0xFF3A2A28),
    onSecondaryContainer = Color(0xFFFFDAD5),
    background = Color(0xFF131314),
    onBackground = Color(0xFFE6E1E3),
    surface = Color(0xFF131314),
    onSurface = Color(0xFFE6E1E3),
    surfaceContainerLow = Color(0xFF1A191B),
    surfaceContainer = Color(0xFF201F21),
    surfaceContainerHigh = Color(0xFF2B2A2C),
    surfaceContainerHighest = Color(0xFF363437),
    onSurfaceVariant = Color(0xFFC9C5C7),
    outline = Color(0xFF938F91),
    outlineVariant = Color(0xFF48464A),
    error = Color(0xFFFFB4AB),
)

/** The status colours: a container for the hero, a deeper shade for its shape, an accent for icons. */
data class ToneColors(val container: Color, val shape: Color, val accent: Color, val onContainer: Color)

fun toneColors(tone: Tone) = when (tone) {
    Tone.GOOD -> ToneColors(Color(0xFF1E3A2A), Color(0xFF2C5A3F), Color(0xFF8FD6A5), Color(0xFFE6F4EA))
    Tone.BAD -> ToneColors(Color(0xFF4A1714), Color(0xFF6B2420), Color(0xFFFFB4A9), Color(0xFFFFEDEA))
    Tone.NEUTRAL -> ToneColors(Color(0xFF2B2A2C), Color(0xFF3A383B), Color(0xFFC9C5C7), Color(0xFFE6E1E3))
    Tone.ATTENTION -> ToneColors(Color(0xFF3A2A28), Color(0xFF73332B), Color(0xFFFFB4A9), Color(0xFFFFDAD5))
}

val StatusGreen = Color(0xFF2F8F57)
val StatusRed = Color(0xFFC4372C)
val FocusRing = Color.White

private val Type = Typography(
    displayMedium = TextStyle(fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

/** Margins that keep everything inside the part of the picture every TV shows. */
val SafeH = 48.dp
val SafeV = 28.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TubeGuardTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = Colors, typography = Type, motionScheme = MotionScheme.expressive(), content = content)
}
