// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.common.NightCityPalette
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.settings.SettingsActivity

/** Currently selected Night City palette (falls back to Night City for HeliBoard / user themes). */
@Composable
fun currentNightCityPalette(): NightCityPalette {
    val ctx = LocalContext.current
    val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0) Log.v("irrelevant", "trigger recomposition on preference change")
    val name = ctx.prefs().getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)
    return NightCityPalette.forId(name) ?: NightCityPalette.NIGHT_CITY_PALETTE
}

/**
 * Night City Keyboard settings theme. The settings app is always dark (the brand is a dark UI);
 * [dark] is kept for API compatibility with the existing screens and previews.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun Theme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = currentNightCityPalette()
    val c = { i: Int -> Color(i) }
    val colorScheme = darkColorScheme(
        primary = c(p.accent),
        onPrimary = c(p.onAccent),
        primaryContainer = c(p.keySecondary),
        onPrimaryContainer = c(p.accentBright),
        secondary = c(p.teal),
        onSecondary = c(p.onAccent),
        secondaryContainer = c(p.keySecondary),
        onSecondaryContainer = c(p.teal),
        tertiary = c(p.teal),
        onTertiary = c(p.onAccent),
        background = c(p.base),
        onBackground = Color(0xFFE9EEF2),
        surface = c(p.base),
        onSurface = Color(0xFFE9EEF2),
        surfaceVariant = c(p.key),
        onSurfaceVariant = c(p.hintText),
        surfaceTint = c(p.accent),
        surfaceContainerLowest = c(p.base),
        surfaceContainerLow = c(p.panel),
        surfaceContainer = c(p.panel),
        surfaceContainerHigh = c(p.key),
        surfaceContainerHighest = c(p.keySecondary),
        outline = c(p.border),
        outlineVariant = c(p.border),
        error = Color(0xFFFF5C6C),
        onError = c(p.onAccent),
    )
    val material3 = Typography()
    val mono = FontFamily.Monospace
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = material3.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            titleMedium = material3.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
            titleSmall = material3.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = mono, letterSpacing = 1.5.sp),
            labelSmall = material3.labelSmall.copy(fontFamily = mono, letterSpacing = 1.2.sp),
        ),
        shapes = Shapes(
            extraSmall = CutCornerShape(topStart = 3.dp, bottomEnd = 3.dp),
            small = CutCornerShape(topStart = 5.dp, bottomEnd = 5.dp),
            medium = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
            large = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
            extraLarge = CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp),
        ),
        content = content
    )
}

const val previewDark = true
