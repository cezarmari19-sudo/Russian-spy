package com.astran.russianspy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B0000),
    secondary = Color(0xFF37474F),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun RussianSpyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
