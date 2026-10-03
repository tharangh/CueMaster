package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CueMasterDarkScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF80F5FF),
    secondary = CueMarkerGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF005325),
    onSecondaryContainer = Color(0xFF6BFF9A),
    tertiary = TertiaryColor,
    onTertiary = Color.White,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioCardBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // CueMaster is a dedicated stage DAW, default dark studio theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CueMasterDarkScheme,
        typography = Typography,
        content = content
    )
}
