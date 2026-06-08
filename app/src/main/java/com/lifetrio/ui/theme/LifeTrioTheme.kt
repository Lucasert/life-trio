package com.lifetrio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background = Color(0xFFF5F7FB)
    val Surface = Color.White
    val Text = Color(0xFF172033)
    val Muted = Color(0xFF667085)
    val Border = Color(0xFFE1E7EF)
    val Blue = Color(0xFF2563EB)
    val BlueSoft = Color(0xFFEAF2FF)
    val Green = Color(0xFF059669)
    val Yellow = Color(0xFFF59E0B)
    val Red = Color(0xFFE11D48)
    val DangerSoft = Color(0xFFFFEEF2)
}

@Composable
fun LifeTrioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Blue,
            secondary = AppColors.Green,
            background = AppColors.Background,
            surface = AppColors.Surface,
            error = AppColors.Red
        ),
        content = { Surface(color = AppColors.Background, content = content) }
    )
}
