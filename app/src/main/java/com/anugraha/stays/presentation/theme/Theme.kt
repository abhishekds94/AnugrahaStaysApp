package com.anugraha.stays.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = SurfaceWhite,

    secondary = SecondaryOrange,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryOrangeDark,
    onSecondaryContainer = SurfaceWhite,

    tertiary = InfoBlue,
    onTertiary = SurfaceWhite,

    background = DarkBackground,
    onBackground = SurfaceWhite,

    surface = DarkSurface,
    onSurface = SurfaceWhite,

    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFE0E0E0),

    error = ErrorRed,
    onError = SurfaceWhite,

    outline = DividerGray,
    outlineVariant = Color(0xFF757575)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = SurfaceWhite,

    secondary = SecondaryOrange,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryOrangeLight,
    onSecondaryContainer = TextPrimary,

    tertiary = InfoBlue,
    onTertiary = SurfaceWhite,

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceWhite,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = SurfaceWhite,

    outline = DividerGray,
    outlineVariant = DisabledGray
)

@Composable
fun AnugrahaStaysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}