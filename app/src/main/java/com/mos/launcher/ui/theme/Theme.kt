package com.mos.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = GlacierDark,
    onPrimary = Ink950,
    primaryContainer = GlacierDarkContainer,
    onPrimaryContainer = Mist100,
    secondary = Mist300,
    onSecondary = Ink950,
    background = Ink950,
    onBackground = Mist100,
    surface = Ink900,
    onSurface = Mist100,
    surfaceVariant = Ink850,
    onSurfaceVariant = Mist300,
    surfaceContainerLow = Ink900,
    surfaceContainer = Ink850,
    surfaceContainerHigh = Ink800,
    outline = Ink600,
    outlineVariant = Ink800,
    error = ErrorDark,
)

private val LightColors = lightColorScheme(
    primary = GlacierLight,
    onPrimary = Paper0,
    primaryContainer = GlacierLightContainer,
    onPrimaryContainer = Slate900,
    secondary = Slate500,
    onSecondary = Paper0,
    background = Paper50,
    onBackground = Slate900,
    surface = Paper0,
    onSurface = Slate900,
    surfaceVariant = Paper100,
    onSurfaceVariant = Slate500,
    surfaceContainerLow = Paper0,
    surfaceContainer = Paper100,
    surfaceContainerHigh = Paper200,
    outline = Slate500,
    outlineVariant = Paper200,
    error = ErrorLight,
)

private val MosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Spacing scale used across mOS for consistent rhythm. */
@Immutable
data class MosSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val screenEdge: Dp = 24.dp,
)

val LocalMosSpacing = staticCompositionLocalOf { MosSpacing() }

/**
 * mOS theme. Dark/Light follows the system for now; Milestone "Customization"
 * will drive [darkTheme] and the accent from DataStore preferences.
 */
@Composable
fun MosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MosTypography,
        shapes = MosShapes,
        content = content,
    )
}

/**
 * Sets status/navigation bar icon colour. Home sits on the wallpaper (light
 * icons); full-screen surfaces in light theme need dark icons.
 */
@Composable
fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }
}
