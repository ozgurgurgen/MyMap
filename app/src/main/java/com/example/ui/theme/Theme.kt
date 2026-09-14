package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RadarColorScheme = darkColorScheme(
    primary = TrafficAmber,
    onPrimary = AsphaltBlack,
    primaryContainer = TrafficAmberDark,
    onPrimaryContainer = TextPrimary,
    secondary = TrafficAmberBright,
    onSecondary = AsphaltBlack,
    secondaryContainer = AsphaltElevated,
    onSecondaryContainer = TrafficAmberLight,
    tertiary = SpeedAlertOrange,
    onTertiary = AsphaltBlack,
    background = AsphaltBlack,
    onBackground = TextPrimary,
    surface = AsphaltDark,
    onSurface = TextPrimary,
    surfaceVariant = AsphaltCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = AsphaltCard,
    surfaceContainerHigh = AsphaltElevated,
    outline = AsphaltBorder,
    error = SpeedAlertRed,
    onError = TextPrimary
)

@Composable
fun RadarUyariTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RadarColorScheme,
        typography = Typography,
        content = content
    )
}

