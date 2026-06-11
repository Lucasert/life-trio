package com.lifetrio.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Warm journal palette — light
private val LightPrimary = Color(0xFFFF8A2A)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFDCBE)
private val LightOnPrimaryContainer = Color(0xFF5C2E00)
private val LightSecondary = Color(0xFF8B6B55)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFF2E1D3)
private val LightOnSecondaryContainer = Color(0xFF3A2417)
private val LightTertiary = Color(0xFF4C8C6A)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFD2EBDC)
private val LightOnTertiaryContainer = Color(0xFF0E351F)
private val LightBackground = Color(0xFFFAF6F0)
private val LightOnBackground = Color(0xFF211A13)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF211A13)
private val LightSurfaceVariant = Color(0xFFF2E8DD)
private val LightOnSurfaceVariant = Color(0xFF52443A)
private val LightSurfaceContainerHigh = Color(0xFFEFE5D8)
private val LightOutline = Color(0xFFC9BBAA)
private val LightOutlineVariant = Color(0xFFE6DACB)
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)

// Warm journal palette — dark (warm brown-black, not flat gray)
private val DarkPrimary = Color(0xFFFFB877)
private val DarkOnPrimary = Color(0xFF4A2800)
private val DarkPrimaryContainer = Color(0xFF693C00)
private val DarkOnPrimaryContainer = Color(0xFFFFDCBE)
private val DarkSecondary = Color(0xFFD7BBA6)
private val DarkOnSecondary = Color(0xFF3A2417)
private val DarkSecondaryContainer = Color(0xFF52392B)
private val DarkOnSecondaryContainer = Color(0xFFF2E1D3)
private val DarkTertiary = Color(0xFFB5CFBE)
private val DarkOnTertiary = Color(0xFF1F3829)
private val DarkTertiaryContainer = Color(0xFF35503F)
private val DarkOnTertiaryContainer = Color(0xFFD2EBDC)
private val DarkBackground = Color(0xFF1A140E)
private val DarkOnBackground = Color(0xFFEDE1D4)
private val DarkSurface = Color(0xFF211A13)
private val DarkOnSurface = Color(0xFFEDE1D4)
private val DarkSurfaceVariant = Color(0xFF50443A)
private val DarkOnSurfaceVariant = Color(0xFFD4C3B4)
private val DarkSurfaceContainerHigh = Color(0xFF2E251C)
private val DarkOutline = Color(0xFF9D8E80)
private val DarkOutlineVariant = Color(0xFF50443A)
private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)

val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

/**
 * Semantic colors not covered by [androidx.compose.material3.ColorScheme]:
 * income/expense accents, budget warning surfaces, and the chart palette.
 */
@Immutable
data class ExtendedColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val chartPalette: List<Color>
)

val LightExtendedColors = ExtendedColors(
    income = Color(0xFF2E7D52),
    expense = LightError,
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFBEAD1),
    onWarningContainer = Color(0xFF4A3414),
    chartPalette = listOf(
        Color(0xFFFF8A2A), // orange
        Color(0xFF4C8C6A), // green
        Color(0xFFE0A53B), // gold
        Color(0xFFC4633F), // terracotta
        Color(0xFF8B5E9E), // plum
        Color(0xFF3E8F96)  // teal
    )
)

val DarkExtendedColors = ExtendedColors(
    income = Color(0xFF8ED0A8),
    expense = DarkError,
    warning = Color(0xFFF4B860),
    warningContainer = Color(0xFF4A3414),
    onWarningContainer = Color(0xFFFBEAD1),
    chartPalette = listOf(
        Color(0xFFFFB877), // orange
        Color(0xFF8ED0A8), // green
        Color(0xFFEAC57A), // gold
        Color(0xFFE0967A), // terracotta
        Color(0xFFC4A0D4), // plum
        Color(0xFF79C2C9)  // teal
    )
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
