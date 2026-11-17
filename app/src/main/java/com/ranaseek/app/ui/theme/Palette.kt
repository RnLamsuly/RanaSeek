package com.ranaseek.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义化配色。深色为默认（对齐 DeepSeek App 截图实测），浅色为新增模式。
 */
data class RsPalette(
    val bg: Color,
    val bgSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val bgSettings: Color,
    val brand: Color,
    val brandSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textPlaceholder: Color,
    val divider: Color,
    val outline: Color,
    val iconMuted: Color,
    val danger: Color,
    val isDark: Boolean
)

val DarkPalette = RsPalette(
    bg = Color(0xFF0F0F0F),
    bgSecondary = Color(0xFF101010),
    surface = Color(0xFF242424),
    surfaceElevated = Color(0xFF292929),
    bgSettings = Color(0xFF1F1F1F),
    brand = Color(0xFF507BF2),
    brandSoft = Color(0xFF283142),
    textPrimary = Color(0xFFF8F8F8),
    textSecondary = Color(0xFF919193),
    textPlaceholder = Color(0xFF6E6E6E),
    divider = Color(0xFF2C2C2C),
    outline = Color(0xFF3D3D3D),
    iconMuted = Color(0xFFB4B4B4),
    danger = Color(0xFFE5484D),
    isDark = true
)

val LightPalette = RsPalette(
    bg = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFF7F7F8),
    surface = Color(0xFFF2F2F4),
    surfaceElevated = Color(0xFFECECEF),
    bgSettings = Color(0xFFF7F7F8),
    brand = Color(0xFF426EFE),
    brandSoft = Color(0xFFE4EAFE),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6B6B),
    textPlaceholder = Color(0xFF9A9A9A),
    divider = Color(0xFFE5E5E5),
    outline = Color(0xFFD2D2D6),
    iconMuted = Color(0xFF6B6B6B),
    danger = Color(0xFFD93036),
    isDark = false
)

val LocalRsPalette = staticCompositionLocalOf { DarkPalette }

/** 当前主题配色（Composable 内读取）。 */
val RsTheme: RsPalette
    @Composable
    get() = LocalRsPalette.current
