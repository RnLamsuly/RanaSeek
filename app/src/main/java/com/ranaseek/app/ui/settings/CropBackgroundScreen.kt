package com.ranaseek.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.data.image.ImageStore
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 手动裁剪聊天背景：双指缩放 + 拖动，全屏取景，导出 1080 宽的 JPEG。
 */
@Composable
fun CropBackgroundScreen(
    uri: android.net.Uri,
    onCancel: () -> Unit,
    onDone: (String) -> Unit
) {
    val context = LocalContext.current
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = ImageStore.loadBitmap(context, uri)?.let { downscale(it, 2048) }
    }
    val bitmap = bitmapState.value

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(Size.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "取消",
                color = Color(0xFFB4B4B4),
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(8.dp)
            )
            Spacer(Modifier.weight(1f))
            Text("调整背景", color = Color.White, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "完成",
                color = Color(0xFF507BF2),
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(enabled = bitmap != null && viewSize != Size.Zero) {
                        val bmp = bitmap ?: return@clickable
                        val path = exportCrop(context, bmp, scale, offset, viewSize)
                        if (path != null) onDone(path)
                    }
                    .padding(8.dp)
            )
        }

        Text(
            "双指缩放 · 单指拖动",
            color = Color(0xFF8A8A8A),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 6.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val boxW = constraints.maxWidth.toFloat()
            val boxH = constraints.maxHeight.toFloat()
            viewSize = Size(boxW, boxH)

            if (bitmap == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF507BF2))
                }
                return@BoxWithConstraints
            }

            val image = bitmap.asImageBitmap()
            val baseScale = min(boxW / bitmap.width, boxH / bitmap.height)
            val dispW = bitmap.width * baseScale
            val dispH = bitmap.height * baseScale

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            val maxX = max(0f, (dispW * newScale - boxW) / 2f)
                            val maxY = max(0f, (dispH * newScale - boxH) / 2f)
                            scale = newScale
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    }
            ) {
                withTransform({
                    translate(center.x + offset.x, center.y + offset.y)
                    scale(scale, scale)
                    translate(-center.x, -center.y)
                }) {
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(
                            ((boxW - dispW) / 2f).roundToInt(),
                            ((boxH - dispH) / 2f).roundToInt()
                        ),
                        dstSize = IntSize(dispW.roundToInt(), dispH.roundToInt())
                    )
                }
            }

            Text(
                "背景会铺满对话页，姐姐建议选暗一些的图哦",
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

private fun downscale(src: android.graphics.Bitmap, maxSide: Int): android.graphics.Bitmap {
    val side = max(src.width, src.height)
    if (side <= maxSide) return src
    val ratio = maxSide.toFloat() / side
    return android.graphics.Bitmap.createScaledBitmap(
        src,
        (src.width * ratio).roundToInt().coerceAtLeast(1),
        (src.height * ratio).roundToInt().coerceAtLeast(1),
        true
    )
}

/**
 * 按取景窗口（viewSize）导出裁剪结果。计算与绘制时的变换完全对应：
 * 显示位置 = 窗口中心 + offset + (源点 - 窗口中心) * scale，图片先按 baseScale 适配窗口。
 */
private fun exportCrop(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap,
    scale: Float,
    offset: Offset,
    viewSize: Size
): String? {
    return runCatching {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val boxW = viewSize.width
        val boxH = viewSize.height
        val baseScale = min(boxW / bw, boxH / bh)

        // 窗口左上角在源图坐标系中的位置
        val srcX = ((0f - boxW / 2f - offset.x) / scale + boxW / 2f) / baseScale
        val srcY = ((0f - boxH / 2f - offset.y) / scale + boxH / 2f) / baseScale
        val srcW = (boxW / scale) / baseScale
        val srcH = (boxH / scale) / baseScale

        val x = srcX.coerceIn(0f, max(0f, bw - srcW))
        val y = srcY.coerceIn(0f, max(0f, bh - srcH))
        val w = srcW.coerceIn(1f, bw - x)
        val h = srcH.coerceIn(1f, bh - y)

        val cropped = android.graphics.Bitmap.createBitmap(
            bitmap,
            x.roundToInt(),
            y.roundToInt(),
            w.roundToInt().coerceAtLeast(1),
            h.roundToInt().coerceAtLeast(1)
        )
        ImageStore.saveBackgroundJpeg(context, cropped)
    }.getOrNull()
}
