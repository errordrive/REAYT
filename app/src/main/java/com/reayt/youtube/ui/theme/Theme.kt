package com.reayt.youtube.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val YouTubeDarkColorScheme = darkColorScheme(
    primary = YouTubeRed,
    secondary = YouTubeSurfaceVariant,
    background = YouTubeDarkBg,
    surface = YouTubeSurface,
    onPrimary = YouTubeTextPrimary,
    onSecondary = YouTubeTextPrimary,
    onBackground = YouTubeTextPrimary,
    onSurface = YouTubeTextPrimary
)

@Composable
fun YouTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YouTubeDarkColorScheme,
        content = content
    )
}
