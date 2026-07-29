package com.yishenghuang.skry.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SkryDarkColorScheme = darkColorScheme(
    primary = SkryColors.Primary,
    onPrimary = SkryColors.OnBackground,
    secondary = SkryColors.Accent,
    onSecondary = SkryColors.Background,
    tertiary = SkryColors.PrimaryVariant,
    background = SkryColors.Background,
    onBackground = SkryColors.OnBackground,
    surface = SkryColors.Surface,
    onSurface = SkryColors.OnBackground,
    surfaceVariant = SkryColors.SurfaceLifted,
    onSurfaceVariant = SkryColors.OnSurfaceMuted,
    error = SkryColors.Risk,
    onError = SkryColors.OnBackground,
    outline = SkryColors.Hairline
)

/**
 * Flagship dark-only theme. Dynamic color is intentionally disabled
 * so brand tokens stay consistent across devices.
 */
@Composable
fun SkryTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = SkryDarkColorScheme,
        typography = Typography,
        content = content
    )
}
