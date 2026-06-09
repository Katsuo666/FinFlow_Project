package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpringGreen,
    onPrimary = Color(0xFF003918),
    primaryContainer = SpringGreenDim,
    onPrimaryContainer = Color(0xFF00612E),
    secondary = AccentTeal,
    onSecondary = Color(0xFF003829),
    tertiary = AccentOrange,
    background = DarkBackground,
    onBackground = OnSurface,
    surface = DarkSurface,
    onSurface = OnSurface,
    surfaceVariant = DarkSurfaceDim,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Forçamos o modo escuro para o visual AMOLED/escuro
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
