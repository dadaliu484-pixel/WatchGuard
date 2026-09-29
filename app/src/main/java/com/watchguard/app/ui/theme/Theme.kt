package com.watchguard.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GuardGreen,
    onPrimary = DarkBackground,
    primaryContainer = GuardGreenDark,
    onPrimaryContainer = LightTextPrimary,
    secondary = GuardCyan,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = LightTextPrimary,
    surface = DarkSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = GuardRed,
    onError = LightTextPrimary
)

@Composable
fun WatchGuardTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
