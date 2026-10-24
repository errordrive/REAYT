package com.reayt.youtube.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val YouTubeDarkColorScheme = darkColorScheme(
    primary = YouTubeRed,
    secondary = YouTubeSurfaceVariant,
    background = YouTubeDarkBg,
    surface = YouTubeSurface,
    surfaceVariant = YouTubeSurfaceVariant,
    onPrimary = YouTubeTextPrimary,
    onSecondary = YouTubeTextPrimary,
    onBackground = YouTubeTextPrimary,
    onSurface = YouTubeTextPrimary,
    onSurfaceVariant = YouTubeTextSecondary
)

private val YouTubeLightColorScheme = lightColorScheme(
    primary = YouTubeRed,
    secondary = YouTubeLightSurfaceVariant,
    background = YouTubeLightBg,
    surface = YouTubeLightSurface,
    surfaceVariant = YouTubeLightSurfaceVariant,
    onPrimary = YouTubeTextPrimary,
    onSecondary = YouTubeLightTextPrimary,
    onBackground = YouTubeLightTextPrimary,
    onSurface = YouTubeLightTextPrimary,
    onSurfaceVariant = YouTubeLightTextSecondary
)

@Composable
fun YouTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) YouTubeDarkColorScheme else YouTubeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
