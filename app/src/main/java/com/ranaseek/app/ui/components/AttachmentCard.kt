package com.ranaseek.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.data.image.ImageStore
import com.ranaseek.app.ui.theme.RsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 附件卡片（图片缩略图 / 文档图标 + 文件名 + 大小 + 可移除）。 */
@Composable
fun UserAttachmentCard(
    name: String,
    sizeText: String,
    imagePath: String?,
    onRemove: () -> Unit
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .widthIn(max = 210.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(p.surface)
            .border(1.dp, p.divider, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imagePath != null) {
            val bmp by produceState<android.graphics.Bitmap?>(null, imagePath) {
                value = withContext(Dispatchers.IO) {
                    ImageStore.loadChatImage(imagePath)
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.brand.copy(alpha = 0.15f))
            ) {
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.brand.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = p.brand
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, color = p.textPrimary, fontSize = 14.sp, maxLines = 1)
            if (sizeText.isNotBlank() && sizeText != "已就绪") {
                Text(sizeText, color = p.textSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "×",
            color = p.textSecondary,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(horizontal = 4.dp)
        )
    }
}

/** 把字节数格式化成 42KB / 1.2MB。 */
fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1fMB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format("%.0fKB", bytes / 1024f)
    bytes > 0 -> "${bytes}B"
    else -> "已就绪"
}
