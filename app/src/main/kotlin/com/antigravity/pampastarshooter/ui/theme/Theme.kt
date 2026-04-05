package com.antigravity.pampastarshooter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF63E7FF),
    secondary = Color(0xFFFFB85A),
    tertiary = Color(0xFFA9B5FF),
    background = Color(0xFF070B11),
    surface = Color(0xFF0C121A),
    surfaceVariant = Color(0xFF121C28),
    onPrimary = Color.Black,
    onBackground = Color(0xFFF4FBFF),
    onSurface = Color(0xFFEAF3FF),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF005B6A),
    secondary = Color(0xFF7A4A00),
    tertiary = Color(0xFF3943A6),
)

@Composable
fun PampaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
