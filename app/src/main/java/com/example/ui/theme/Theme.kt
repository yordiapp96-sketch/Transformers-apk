package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberDarkColorScheme = darkColorScheme(
    primary = EnergonCyan,
    onPrimary = CyberVoid,
    primaryContainer = AutobotCobalt,
    onPrimaryContainer = TextPrimary,
    secondary = AutobotCrimson,
    onSecondary = TextPrimary,
    tertiary = MatrixGold,
    onTertiary = CyberVoid,
    background = CyberVoid,
    onBackground = TextPrimary,
    surface = CyberNavy,
    onSurface = TextPrimary,
    surfaceVariant = CyberPanel,
    onSurfaceVariant = TextSecondary,
    error = AlertRed,
    onError = TextPrimary,
    outline = CyberPanelBorder,
)

@Composable
fun CyberShiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = CyberTypography,
        content = content,
    )
}
