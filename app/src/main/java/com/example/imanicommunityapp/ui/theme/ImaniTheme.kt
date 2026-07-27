package com.example.imanicommunityapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ImaniColor.BluePrimary,
    onPrimary = ImaniColor.White,
    primaryContainer = ImaniColor.BlueLight,
    onPrimaryContainer = ImaniColor.White,
    secondary = ImaniColor.BlueLight,
    onSecondary = ImaniColor.White,
    background = ImaniColor.White,
    onBackground = ImaniColor.Black,
    surface = ImaniColor.Surface,
    onSurface = ImaniColor.Black,
    onSurfaceVariant = ImaniColor.OnSurfaceMuted,
    outline = ImaniColor.LineGray,
    error = ImaniColor.Red,
)

private val DarkColors = darkColorScheme(
    primary = ImaniColor.BlueLight,
    onPrimary = ImaniColor.Black,
    primaryContainer = ImaniColor.BluePrimary,
    onPrimaryContainer = ImaniColor.White,
    secondary = ImaniColor.BlueLight,
    background = Color(0xFF0B1220),
    onBackground = ImaniColor.White,
    surface = Color(0xFF121A2B),
    onSurface = ImaniColor.White,
    onSurfaceVariant = ImaniColor.Gray,
    outline = Color(0xFF2A3548),
    error = ImaniColor.Red,
)

@Composable
fun ImaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
