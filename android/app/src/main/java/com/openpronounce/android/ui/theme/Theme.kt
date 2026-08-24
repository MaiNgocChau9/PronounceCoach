package com.openpronounce.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FallbackLight = lightColorScheme()
private val FallbackDark = darkColorScheme()

// Fixed palettes for when Material You dynamic color is turned off in Settings.
val PaletteGreen = Color(0xFF1FA05A)
val PaletteBlue = Color(0xFF2F6FED)
val PalettePurple = Color(0xFF7C5CF0)

private fun tinted(seed: Color, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = if (dark) seed.copy(alpha = 0.35f) else seed.copy(alpha = 0.18f),
        onPrimaryContainer = if (dark) Color(0xFFE8FFEF) else Color(0xFF08351D),
        secondary = seed.copy(alpha = 0.85f),
        tertiary = seed.copy(red = seed.red * 0.6f + 0.25f, green = seed.green * 0.5f + 0.35f, blue = seed.blue * 0.4f + 0.45f)
    )
}

/**
 * Material You: wallpaper-based dynamic color on Android 12+, generous rounded shapes,
 * and a fallback palette elsewhere. [darkOverride] (null = follow system) and
 * [colorSource] ("system" | "green" | "blue" | "purple") come from Settings.
 */
@Composable
fun OpenPronounceTheme(
    darkOverride: Boolean? = null,
    colorSource: String = "system",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = darkOverride ?: isSystemInDarkTheme()
    val useDynamic = colorSource == "system"

    val colorScheme = when {
        useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> when (colorSource) {
            "green" -> tinted(PaletteGreen, darkTheme)
            "blue" -> tinted(PaletteBlue, darkTheme)
            "purple" -> tinted(PalettePurple, darkTheme)
            else -> if (darkTheme) FallbackDark else FallbackLight
        }
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}
