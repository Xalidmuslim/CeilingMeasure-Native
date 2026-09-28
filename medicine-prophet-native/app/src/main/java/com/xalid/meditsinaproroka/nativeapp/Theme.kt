package com.xalid.meditsinaproroka.nativeapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF91B9A1),
    onPrimary = Color(0xFF102019),
    primaryContainer = Color(0xFF253A30),
    onPrimaryContainer = Color(0xFFE4EFE8),
    background = Color(0xFF141D18),
    onBackground = Color(0xFFF1EDE4),
    surface = Color(0xFF1B2620),
    onSurface = Color(0xFFF1EDE4),
    surfaceVariant = Color(0xFF243129),
    onSurfaceVariant = Color(0xFFB7B9B4),
    outline = Color(0xFF3B4A42),
    secondary = Color(0xFFD1B77B),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF315C45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDECE3),
    onPrimaryContainer = Color(0xFF173625),
    background = Color(0xFFF6F1E8),
    onBackground = Color(0xFF222823),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF222823),
    surfaceVariant = Color(0xFFEDE7DD),
    onSurfaceVariant = Color(0xFF626862),
    outline = Color(0xFFC9C2B7),
    secondary = Color(0xFF8B6E34),
)

@Composable
fun MedicinaTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content,
    )
}
