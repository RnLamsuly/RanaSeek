package com.ranaseek.app.ui.theme

import androidx.compose.ui.graphics.Color

/** 颜色 token，取值来自 DeepSeek 移动端截图实测 + 官方资源表 color/primary。 */
object RsColors {
    val Bg = Color(0xFF0F0F0F)
    val BgSecondary = Color(0xFF101010)
    val Surface = Color(0xFF242424)
    val SurfaceElevated = Color(0xFF292929)
    val BgSettings = Color(0xFF1F1F1F)

    /** 浅色主题下的官方主色 */
    val BrandLight = Color(0xFF426EFE)

    /** 深色主题下的官方主色（App 默认深色） */
    val Brand = Color(0xFF507BF2)

    /** 「深度思考」胶囊底色 */
    val BrandSoft = Color(0xFF283142)

    val TextPrimary = Color(0xFFF8F8F8)
    val TextSecondary = Color(0xFF919193)
    val TextPlaceholder = Color(0xFF6E6E6E)
    val Divider = Color(0xFF2C2C2C)
    val Outline = Color(0xFF3D3D3D)
    val IconMuted = Color(0xFFB4B4B4)
    val Danger = Color(0xFFE5484D)
}
