package com.example.tripshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun TripShotTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = TripShotPrimary,
        onPrimary = TripShotOnPrimary,

        secondary = TripShotSecondary,
        onSecondary = TripShotOnSecondary,

        background = TripShotBgColor,
        onBackground = TripShotTextPrimary,

        surface = TripShotSurfaceColor,
        onSurface = TripShotOnSurface,

        surfaceVariant = TripShotTabBgColor,
        onSurfaceVariant = TripShotTextSecondary,

        outline = TripShotHint,

        primaryContainer = TripShotNavIndicator,
        onPrimaryContainer = TripShotOnPrimaryContainer
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}