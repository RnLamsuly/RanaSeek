package com.ranaseek.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.ui.theme.RsTheme

/** 全局人设（system prompt）编辑页。 */
@Composable
fun PersonaScreen(
    initial: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val p = RsTheme
    var text by rememberSaveable { mutableStateOf(initial) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bgSettings)
            .statusBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "全局人设",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = p.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .background(p.surfaceElevated, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = p.textPrimary
                )
            }
            TextButton(
                onClick = {
                    onSave(text.trim())
                    saved = true
                    onBack()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(if (saved) "已保存" else "保存", color = p.brand, fontSize = 15.sp)
            }
        }

        Text(
            "给助手的全局人设，会作为 system 提示词放在每次对话开头。\n" +
                "例如：你叫小海，说话温柔，回答简洁，喜欢用比喻。",
            color = p.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            textStyle = TextStyle(fontSize = 16.sp, color = p.textPrimary),
            placeholder = {
                Text(
                    "在这里写下你的人设…",
                    color = p.textPlaceholder,
                    fontSize = 15.sp
                )
            }
        )
        Spacer(Modifier.height(12.dp))
    }
}
