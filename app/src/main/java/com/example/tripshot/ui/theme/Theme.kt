package com.example.tripshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TripShotColorScheme = darkColorScheme(
    primary = TripShotPrimary,
    onPrimary = Color.White,
    secondary = TripShotTabBgColor,
    onSecondary = TripShotTextPrimary,
    tertiary = TripShotSurfaceColor,
    onTertiary = TripShotTextPrimary,
    background = TripShotBgColor,
    onBackground = TripShotTextPrimary,
    surface = TripShotSurfaceColor,
    onSurface = TripShotTextPrimary,
    surfaceVariant = TripShotTabBgColor,
    onSurfaceVariant = TripShotTextSecondary,
    outline = TripShotDividerColor,
    outlineVariant = TripShotHint
)

@Composable
fun TripShotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TripShotColorScheme,
        typography = Typography,
        content = content
    )
}