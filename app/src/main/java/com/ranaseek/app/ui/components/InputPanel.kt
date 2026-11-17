package com.ranaseek.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.ui.theme.RsTheme
import androidx.compose.ui.geometry.Offset

/** 「深度思考」图标：两个交叠的菱形（自绘，近似官方）。 */
@Composable
fun DeepThinkIcon(
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    val c = tint ?: RsTheme.brand
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val stroke = s * 0.10f
        val half = s * 0.24f
        fun diamond(cx: Float, cy: Float) {
            val path = Path().apply {
                moveTo(cx, cy - half)
                lineTo(cx + half * 0.78f, cy)
                lineTo(cx, cy + half)
                lineTo(cx - half * 0.78f, cy)
                close()
            }
            drawPath(path, c, style = Stroke(width = stroke))
        }
        diamond(s * 0.36f, s * 0.5f)
        diamond(s * 0.64f, s * 0.5f)
    }
}

/** 底部输入区上方的圆角胶囊开关（深度思考 / 智能搜索）。 */
@Composable
fun ModeChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val p = RsTheme
    val bg = if (enabled) p.brandSoft else p.surfaceElevated
    val fg = if (enabled) p.brand else p.textSecondary
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(text = text, color = fg, fontSize = 13.sp)
    }
}

/** 圆形描边图标按钮（加号 / 麦克风 / 发送）。 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val p = RsTheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) p.surface else p.surface.copy(alpha = 0.55f), CircleShape)
            .border(1.2.dp, if (enabled) p.outline else p.outline.copy(alpha = 0.5f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** 无边框实心圆按钮（发送 / 停止）。 */
@Composable
fun SolidCircleButton(
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) color else color.copy(alpha = 0.5f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** 输入面板：圆角卡片 + 输入框 + 模式开关 + 右侧按钮。 */
@Composable
fun InputPanel(
    text: String,
    onTextChange: (String) -> Unit,
    deepThinking: Boolean,
    onToggleDeepThinking: () -> Unit,
    webSearch: Boolean,
    onToggleWebSearch: () -> Unit,
    onSend: () -> Unit,
    onAddAttachment: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    isRecording: Boolean = false,
    hasAttachment: Boolean = false,
    acrylic: Boolean = false,
    acrylicTint: Int = 38,
    /** 亚克力局部磨砂：背景图 + 模糊半径（px） */
    bgBitmap: android.graphics.Bitmap? = null,
    blurPx: Int = 0,
    /** 面板在屏幕上的位置（供背景层裁剪磨砂区域） */
    onBounds: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    previewEnabled: Boolean = false,
    onTogglePreview: () -> Unit = {},
    sendEnabled: Boolean = true,
    onStop: () -> Unit = {}
) {
    val p = RsTheme
    val shape = RoundedCornerShape(28.dp)
    val surfaceColor = if (acrylic) {
        if (p.isDark) {
            Color(0xFF0C0F18).copy(alpha = (acrylicTint / 100f).coerceIn(0.1f, 0.92f))
        } else {
            Color.White.copy(alpha = (acrylicTint / 100f + 0.22f).coerceIn(0.2f, 0.92f))
        }
    } else {
        p.surface
    }
    val acrylicBorder = if (acrylic) {
        androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (p.isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.65f)
        )
    } else null

    Box(
        modifier = modifier
            .clip(shape)
            .onGloballyPositioned { coords ->
                onBounds?.invoke(coords.boundsInRoot())
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = surfaceColor,
            border = acrylicBorder
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                // Markdown 预览（勾选后显示，参考 DSH 的输入框选项）
                if (previewEnabled && text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (acrylic) p.textPrimary.copy(alpha = 0.06f)
                                else p.surfaceElevated,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        MarkdownText(
                            text = text,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            acrylic = acrylic,
                            acrylicTint = acrylicTint
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    maxLines = 6,
                    textStyle = TextStyle(color = p.textPrimary, fontSize = 16.sp),
                    cursorBrush = SolidColor(p.brand),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                text = "发消息或按住说话",
                                color = p.textPlaceholder,
                                fontSize = 16.sp
                            )
                        }
                        inner()
                    }
                )
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 滚动视口：边缘淡化随滚动位置出现（初始左端不虚化，滑动后才淡出）
                    val chipScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 96.dp)
                            .clipToBounds()
                            .graphicsLayer(
                                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                            )
                            .drawWithContent {
                                drawContent()
                                val fade = 20.dp.toPx()
                                val scroll = chipScroll.value.toFloat()
                                val maxScroll = chipScroll.maxValue.toFloat()
                                // 左侧：有滚动时才开始淡化
                                val leftAlpha = (scroll / fade).coerceIn(0f, 1f)
                                if (leftAlpha > 0f) {
                                    drawRect(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = leftAlpha),
                                                Color.Transparent
                                            ),
                                            startX = 0f,
                                            endX = fade
                                        ),
                                        blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                                    )
                                }
                                // 右侧：还能继续滚动时才淡化，滚到尽头消失
                                val rightAlpha = if (maxScroll <= 0f) {
                                    0f
                                } else {
                                    ((maxScroll - scroll) / fade).coerceIn(0f, 1f)
                                }
                                if (rightAlpha > 0f) {
                                    drawRect(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = rightAlpha)
                                            ),
                                            startX = size.width - fade,
                                            endX = size.width
                                        ),
                                        blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                                    )
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(chipScroll),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModeChip(
                                text = "深度思考",
                                enabled = deepThinking,
                                onClick = onToggleDeepThinking
                            ) {
                                DeepThinkIcon(
                                    Modifier.size(16.dp),
                                    if (deepThinking) p.brand else p.textSecondary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            ModeChip(
                                text = "智能搜索",
                                enabled = webSearch,
                                onClick = onToggleWebSearch
                            ) {
                                Icon(
                                    Icons.Outlined.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (webSearch) p.brand else p.textSecondary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            ModeChip(
                                text = "预览",
                                enabled = previewEnabled,
                                onClick = onTogglePreview
                            ) {
                                Icon(
                                    Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (previewEnabled) p.brand else p.textSecondary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }

                    // 右侧固定圆钮：加号 + 语音/发送/停止
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleIconButton(onClick = onAddAttachment) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "添加附件",
                                modifier = Modifier.size(20.dp),
                                tint = p.textPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        val canSend = text.isNotBlank() || hasAttachment
                        when {
                            isStreaming -> SolidCircleButton(
                                onClick = onStop,
                                color = p.danger
                            ) {
                                // 实心正方形
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White)
                                )
                            }
                            canSend -> SolidCircleButton(
                                onClick = onSend,
                                color = p.brand,
                                enabled = sendEnabled
                            ) {
                                Icon(
                                    Icons.Outlined.ArrowUpward,
                                    contentDescription = "发送",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                            else -> CircleIconButton(onClick = onVoice) {
                                Icon(
                                    Icons.Outlined.Mic,
                                    contentDescription = "语音输入",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isRecording) p.danger else p.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
