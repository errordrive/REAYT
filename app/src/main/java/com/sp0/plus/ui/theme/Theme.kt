package com.sp0.plus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyLightGrey,
    background = SpotifyBlack,
    surface = SpotifyDarkGrey,
    onPrimary = SpotifyBlack,
    onSecondary = SpotifyTextWhite,
    onBackground = SpotifyTextWhite,
    onSurface = SpotifyTextWhite
)

@Composable
fun Sp0PlusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
