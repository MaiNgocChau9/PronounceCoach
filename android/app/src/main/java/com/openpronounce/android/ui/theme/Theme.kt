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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val PaletteGreen = Color(0xFF006D3A)
val PaletteBlue = Color(0xFF1E5EC8)
val PalettePurple = Color(0xFF6750A4)

/** Wallpaper-derived primary color, independent of current palette selection.
 *  Used by the DynamicSwatch so it always shows the device's actual wallpaper color. */
@Composable
fun wallpaperPrimaryColor(): Color {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    return remember(darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            scheme.primary
        } else {
            PaletteGreen
        }
    }
}

private val LightGreenScheme = lightColorScheme(
    primary = Color(0xFF006D3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF99F7B3),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF4F6353),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0D1F13),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFE9F8),
    onTertiaryContainer = Color(0xFF001F27),
    background = Color(0xFFF6FAF3),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF6FAF3),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF414942),
    surfaceContainer = Color(0xFFEBEFE8),
    surfaceContainerLow = Color(0xFFF1F5EE),
    surfaceContainerHigh = Color(0xFFE5E9E2)
)

private val DarkGreenScheme = darkColorScheme(
    primary = Color(0xFF7DDA99),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00532B),
    onPrimaryContainer = Color(0xFF99F7B3),
    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3C),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033641),
    tertiaryContainer = Color(0xFF214C58),
    onTertiaryContainer = Color(0xFFBFE9F8),
    background = Color(0xFF101511),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF101511),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9BF),
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerLow = Color(0xFF181D19),
    surfaceContainerHigh = Color(0xFF272B27)
)

private val LightBlueScheme = lightColorScheme(
    primary = Color(0xFF1E5EC8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001945),
    secondary = Color(0xFF575E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF715573),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCD7FB),
    onTertiaryContainer = Color(0xFF29132D),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceContainer = Color(0xFFEDEEF6),
    surfaceContainerLow = Color(0xFFF3F3FB),
    surfaceContainerHigh = Color(0xFFE7E8F0)
)

private val DarkBlueScheme = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002D6F),
    primaryContainer = Color(0xFF00439D),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFDFBBDE),
    onTertiary = Color(0xFF402743),
    tertiaryContainer = Color(0xFF583E5A),
    onTertiaryContainer = Color(0xFFFCD7FB),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainerHigh = Color(0xFF282A2F)
)

private val LightPurpleScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainerHigh = Color(0xFFECE6F0)
)

private val DarkPurpleScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerHigh = Color(0xFF2B2930)
)

/**
 * Material You: wallpaper-based dynamic color on Android 12+, generous rounded shapes,
 * and standard Material 3 tonal palettes. [darkOverride] (null = follow system) and
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

    val colorScheme = remember(darkTheme, colorSource) {
        when {
            useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> when (colorSource) {
                "green" -> if (darkTheme) DarkGreenScheme else LightGreenScheme
                "blue" -> if (darkTheme) DarkBlueScheme else LightBlueScheme
                "purple" -> if (darkTheme) DarkPurpleScheme else LightPurpleScheme
                else -> if (darkTheme) DarkGreenScheme else LightGreenScheme
            }
        }
    }
    val shapes = remember {
        Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}
