package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BeigeSimpleColorScheme = lightColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEEFB),
    onPrimaryContainer = Color(0xFF1A365D),
    secondary = NeonEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4EDDA),
    onSecondaryContainer = Color(0xFF155724),
    tertiary = WarmAmber,
    onTertiary = Color.White,
    background = DeepNavyBg,
    onBackground = TextPrimaryLight,
    surface = ElevatedSlateSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardSlateSurface,
    onSurfaceVariant = TextSecondaryMuted,
    error = CoralError,
    outline = BorderSubtle
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BeigeSimpleColorScheme,
        typography = Typography,
        content = content
    )
}
