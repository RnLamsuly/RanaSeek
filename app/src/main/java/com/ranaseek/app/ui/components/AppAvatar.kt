package com.ranaseek.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ranaseek.app.R

/** 助手头像：圆形裁剪，保留人物上半身。 */
@Composable
fun AppAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 92.dp
) {
    Image(
        painter = painterResource(R.drawable.avatar),
        contentDescription = "RanaSeek",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}
