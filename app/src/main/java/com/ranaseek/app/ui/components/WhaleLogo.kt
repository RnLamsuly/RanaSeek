package com.ranaseek.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.ranaseek.app.ui.theme.RsTheme

/**
 * 自绘鲸鱼 logo（原创绘制，不复制官方素材）。
 * 造型：扁圆身体 + 右上短小分叉尾鳍 + 白色大眼 + 胸鳍。
 */
@Composable
fun WhaleLogo(
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val c = tint ?: RsTheme.brand
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)

        // 尾鳍：短小、上扬、V 形分叉
        val tail = Path().apply {
            moveTo(p(0.62f, 0.46f).x, p(0.62f, 0.46f).y)
            cubicTo(
                p(0.70f, 0.32f).x, p(0.70f, 0.32f).y,
                p(0.77f, 0.24f).x, p(0.77f, 0.24f).y,
                p(0.82f, 0.27f).x, p(0.82f, 0.27f).y
            )
            cubicTo(
                p(0.81f, 0.33f).x, p(0.81f, 0.33f).y,
                p(0.79f, 0.37f).x, p(0.79f, 0.37f).y,
                p(0.78f, 0.39f).x, p(0.78f, 0.39f).y
            )
            cubicTo(
                p(0.84f, 0.34f).x, p(0.84f, 0.34f).y,
                p(0.91f, 0.31f).x, p(0.91f, 0.31f).y,
                p(0.93f, 0.35f).x, p(0.93f, 0.35f).y
            )
            cubicTo(
                p(0.88f, 0.43f).x, p(0.88f, 0.43f).y,
                p(0.82f, 0.48f).x, p(0.82f, 0.48f).y,
                p(0.74f, 0.52f).x, p(0.74f, 0.52f).y
            )
            close()
        }
        drawPath(tail, c)

        drawOval(
            color = c,
            topLeft = p(0.12f, 0.36f),
            size = Size(0.66f * s, 0.50f * s)
        )

        val fin = Path().apply {
            moveTo(p(0.40f, 0.66f).x, p(0.40f, 0.66f).y)
            cubicTo(
                p(0.48f, 0.76f).x, p(0.48f, 0.76f).y,
                p(0.58f, 0.80f).x, p(0.58f, 0.80f).y,
                p(0.66f, 0.76f).x, p(0.66f, 0.76f).y
            )
            cubicTo(
                p(0.58f, 0.70f).x, p(0.58f, 0.70f).y,
                p(0.48f, 0.67f).x, p(0.48f, 0.67f).y,
                p(0.40f, 0.66f).x, p(0.40f, 0.66f).y
            )
            close()
        }
        drawPath(fin, c)

        drawCircle(color = Color.White, radius = 0.095f * s, center = p(0.34f, 0.56f))
        drawCircle(color = Color(0xFF0F0F0F), radius = 0.043f * s, center = p(0.355f, 0.555f))
    }
}
