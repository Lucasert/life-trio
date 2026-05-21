package com.lifetrio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val Text = Color(0xFF1E293B)
    val Muted = Color(0xFF64748B)
    val Border = Color(0xFFE2E8F0)
    val Blue = Color(0xFF3B82F6)
    val BlueSoft = Color(0xFFEFF6FF)
    val Green = Color(0xFF16A34A)
    val Yellow = Color(0xFFF59E0B)
    val Red = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFFE4E6)
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
