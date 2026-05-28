package com.gibdd.eyewitness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1565C0)
private val BlueDark = Color(0xFF0D47A1)
private val Accent = Color(0xFFD32F2F)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = BlueDark,
    error = Accent,
)

private val DarkColors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = BlueDark,
    error = Accent,
)

@Composable
fun GibddEyewitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
