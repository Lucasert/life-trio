package com.lifetrio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Legacy flat color tokens repointed to the warm journal palette. Retained as a
 * transition shim so not-yet-migrated screens keep rendering; call sites are
 * migrated to [MaterialTheme.colorScheme] / [LocalExtendedColors] across later
 * phases, after which this object is removed.
 */
object AppColors {
    val Background = Color(0xFFFAF6F0)
    val Surface = Color(0xFFFFFFFF)
    val Text = Color(0xFF211A13)
    val Muted = Color(0xFF52443A)
    val Border = Color(0xFFE6DACB)
    val Blue = Color(0xFFFF8A2A)
    val BlueSoft = Color(0xFFFFDCBE)
    val Green = Color(0xFF2E7D52)
    val Yellow = Color(0xFFB45309)
    val Red = Color(0xFFBA1A1A)
    val DangerSoft = Color(0xFFFFDAD6)
}

@Composable
fun LifeTrioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = { Surface(color = colorScheme.background, content = content) }
        )
    }
}
