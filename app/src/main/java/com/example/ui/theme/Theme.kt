package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun InfinityClockTheme(
    themeName: String,
    amoledDark: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = ThemePreset.getColorScheme(themeName, amoledDark)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
