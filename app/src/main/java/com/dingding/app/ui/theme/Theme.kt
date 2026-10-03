package com.dingding.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DingDingDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = TextSecondary,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun DingDingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DingDingDarkColorScheme,
        typography = Typography,
        content = content
    )
}
