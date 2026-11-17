package com.ranaseek.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ranaseek.app.data.settings.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = DarkPalette.brand,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DarkPalette.brandSoft,
    onPrimaryContainer = DarkPalette.brand,
    secondary = DarkPalette.brand,
    background = DarkPalette.bg,
    onBackground = DarkPalette.textPrimary,
    surface = DarkPalette.surface,
    onSurface = DarkPalette.textPrimary,
    surfaceVariant = DarkPalette.surfaceElevated,
    onSurfaceVariant = DarkPalette.textSecondary,
    outline = DarkPalette.outline,
    outlineVariant = DarkPalette.divider,
    error = DarkPalette.danger
)

private val LightScheme = lightColorScheme(
    primary = LightPalette.brand,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = LightPalette.brandSoft,
    onPrimaryContainer = LightPalette.brand,
    secondary = LightPalette.brand,
    background = LightPalette.bg,
    onBackground = LightPalette.textPrimary,
    surface = LightPalette.surface,
    onSurface = LightPalette.textPrimary,
    surfaceVariant = LightPalette.surfaceElevated,
    onSurfaceVariant = LightPalette.textSecondary,
    outline = LightPalette.outline,
    outlineVariant = LightPalette.divider,
    error = LightPalette.danger
)

/** 根据设置切换浅色/深色/跟随系统，并把语义配色下发到 CompositionLocal。 */
@Composable
fun RanaSeekTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalRsPalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = RsTypography,
            content = content
        )
    }
}
