package com.ranaseek.app.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.data.image.ImageStore
import com.ranaseek.app.data.settings.AppSettings
import com.ranaseek.app.data.settings.BubbleStyle
import com.ranaseek.app.data.settings.ThemeMode
import com.ranaseek.app.ui.theme.RsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppearanceScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onApplyBackground: (String) -> Unit,
    onBlurChange: (Int) -> Unit,
    onDimChange: (Int) -> Unit,
    onClearBackground: () -> Unit,
    onBubbleStyle: (BubbleStyle) -> Unit,
    onAssistantBubble: (Boolean) -> Unit,
    onAcrylicTint: (Int) -> Unit,
    onAcrylicSaturation: (Int) -> Unit,
    onAcrylicBlur: (Int) -> Unit
) {
    val p = RsTheme
    val context = LocalContext.current
    var cropUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) cropUri = uri
    }

    val backgroundBitmap by produceState<android.graphics.Bitmap?>(null, settings.backgroundPath) {
        value = withContext(Dispatchers.IO) {
            ImageStore.loadBackground(settings.backgroundPath)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bgSettings)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "外观", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("主题模式")
            SettingsCard {
                RadioRow("跟随系统", settings.themeMode == ThemeMode.SYSTEM) {
                    onThemeMode(ThemeMode.SYSTEM)
                }
                RadioRow("浅色", settings.themeMode == ThemeMode.LIGHT) {
                    onThemeMode(ThemeMode.LIGHT)
                }
                RadioRow("深色", settings.themeMode == ThemeMode.DARK) {
                    onThemeMode(ThemeMode.DARK)
                }
            }

            SectionLabel("聊天背景")
            SettingsCard {
                // 预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .aspectRatio(2.2f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.surface)
                        .border(1.dp, p.divider, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = backgroundBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "聊天背景预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (Build.VERSION.SDK_INT >= 31 && settings.backgroundBlur > 0) {
                                        Modifier.blur(settings.backgroundBlur.dp)
                                    } else Modifier
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                                    )
                                )
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = null,
                                tint = p.textSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("还没有背景图", color = p.textSecondary, fontSize = 13.sp)
                        }
                    }
                }

                SettingsRow(
                    icon = Icons.Outlined.AddPhotoAlternate,
                    title = if (settings.hasBackground) "更换背景图片" else "选择背景图片",
                    value = "",
                    showArrow = false,
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                if (settings.hasBackground) {
                    // 虚化程度
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("虚化程度", color = p.textPrimary, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${settings.backgroundBlur}",
                                color = p.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Slider(
                            value = settings.backgroundBlur.toFloat(),
                            onValueChange = { onBlurChange(it.toInt()) },
                            valueRange = 0f..25f,
                            steps = 24
                        )
                    }

                    // 背景暗化（参考 DSH 窗口背景插件）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("背景暗化", color = p.textPrimary, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${settings.backgroundDim}", color = p.textSecondary, fontSize = 14.sp)
                        }
                        Slider(
                            value = settings.backgroundDim.toFloat(),
                            onValueChange = { onDimChange(it.toInt()) },
                            valueRange = 0f..80f,
                            steps = 79
                        )
                    }

                    SettingsRow(
                        icon = Icons.Outlined.Delete,
                        title = "移除背景图片",
                        value = "",
                        showArrow = false,
                        onClick = onClearBackground
                    )
                }
            }
            Text(
                "使用背景图时会自动切换到浅色主题，文字对比更清晰",
                color = p.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )

            SectionLabel("消息框风格")
            SettingsCard {
                StyleRow(
                    title = "纯色",
                    desc = "清爽的实色气泡",
                    selected = settings.bubbleStyle == BubbleStyle.SOLID,
                    onClick = { onBubbleStyle(BubbleStyle.SOLID) }
                ) {
                    BubblePreview(acrylic = false)
                }
                StyleRow(
                    title = "亚克力",
                    desc = "半透明 + 细描边，有玻璃质感",
                    selected = settings.bubbleStyle == BubbleStyle.ACRYLIC,
                    onClick = { onBubbleStyle(BubbleStyle.ACRYLIC) }
                ) {
                    BubblePreview(acrylic = true)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("显示助手气泡", color = p.textPrimary, fontSize = 16.sp)
                        Text(
                            "关闭后助手回复为纯文本",
                            color = p.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.assistantBubble,
                        onCheckedChange = onAssistantBubble,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedTrackColor = p.brand
                        )
                    )
                }

                if (settings.bubbleStyle == BubbleStyle.ACRYLIC) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("亚克力底色", color = p.textPrimary, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${settings.acrylicTint}", color = p.textSecondary, fontSize = 14.sp)
                        }
                        Slider(
                            value = settings.acrylicTint.toFloat(),
                            onValueChange = { onAcrylicTint(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 99
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("亚克力饱和度", color = p.textPrimary, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${settings.acrylicSaturation}", color = p.textSecondary, fontSize = 14.sp)
                        }
                        Slider(
                            value = settings.acrylicSaturation.toFloat(),
                            onValueChange = { onAcrylicSaturation(it.toInt()) },
                            valueRange = 100f..200f,
                            steps = 99
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("亚克力模糊", color = p.textPrimary, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Text("${settings.acrylicBlur} px", color = p.textSecondary, fontSize = 14.sp)
                        }
                        Text(
                            "亚克力表面背后壁纸的模糊半径",
                            color = p.textSecondary,
                            fontSize = 12.sp
                        )
                        Slider(
                            value = settings.acrylicBlur.toFloat(),
                            onValueChange = { onAcrylicBlur(it.toInt()) },
                            valueRange = 0f..30f,
                            steps = 29
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    val uri = cropUri
    if (uri != null) {
        CropBackgroundScreen(
            uri = uri,
            onCancel = { cropUri = null },
            onDone = { path ->
                onApplyBackground(path)
                cropUri = null
            }
        )
    }
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = p.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(1.5.dp, if (selected) p.brand else p.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(p.brand, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun StyleRow(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp, 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(p.bg)
                .border(
                    1.dp,
                    if (selected) p.brand else p.divider,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            preview()
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = p.textPrimary, fontSize = 16.sp)
            Text(desc, color = p.textSecondary, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(1.5.dp, if (selected) p.brand else p.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(Modifier.size(12.dp).background(p.brand, CircleShape))
            }
        }
    }
}

@Composable
private fun BubblePreview(acrylic: Boolean) {
    val p = RsTheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .height(20.dp)
            .clip(shape)
            .background(
                if (acrylic) p.textPrimary.copy(alpha = 0.16f) else p.surface
            )
            .then(
                if (acrylic) {
                    Modifier.border(1.dp, p.textPrimary.copy(alpha = 0.22f), shape)
                } else Modifier
            )
    )
}
