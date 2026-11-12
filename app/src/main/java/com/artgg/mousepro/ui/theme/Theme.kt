package com.artgg.mousepro.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ArtggElectricBlue,
    onPrimary = ArtggTextPrimary,
    primaryContainer = ArtggElectricBlueDim,
    secondary = ArtggNeonPurple,
    onSecondary = ArtggTextPrimary,
    secondaryContainer = ArtggNeonPurpleDim,
    background = ArtggBackground,
    onBackground = ArtggTextPrimary,
    surface = ArtggSurface,
    onSurface = ArtggTextPrimary,
    surfaceVariant = ArtggSurfaceVariant,
    onSurfaceVariant = ArtggTextSecondary,
    error = ArtggError,
    onError = ArtggTextPrimary,
    outline = ArtggDivider
)

@Composable
fun ARTGGMouseProTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ArtggBackground.toArgb()
            window.navigationBarColor = ArtggBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}