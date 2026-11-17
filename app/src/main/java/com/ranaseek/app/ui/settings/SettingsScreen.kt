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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.data.settings.AppSettings
import com.ranaseek.app.data.settings.ThemeMode
import com.ranaseek.app.ui.theme.RsTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPersona: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestApiKey: suspend (String) -> Result<String>
) {
    val p = RsTheme
    var showApiKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bgSettings)
            .statusBarsPadding()
    ) {
        SettingsTopBar(title = "设置", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionLabel("账户")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Key,
                    title = "API Key",
                    value = if (settings.hasApiKey) "已设置" else "未设置",
                    onClick = { showApiKeyDialog = true }
                )
                SettingsRow(
                    icon = Icons.Outlined.Person,
                    title = "全局人设",
                    value = if (settings.globalPrompt.isBlank()) "未设置" else "已设置",
                    onClick = onOpenPersona
                )
            }

            SectionLabel("应用")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "外观",
                    value = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> "跟随系统"
                        ThemeMode.LIGHT -> "浅色"
                        ThemeMode.DARK -> "深色"
                    },
                    onClick = onOpenAppearance
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            initial = settings.apiKey,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = {
                onApiKeyChange(it)
                showApiKeyDialog = false
            },
            onTest = onTestApiKey
        )
    }
}

@Composable
fun SettingsTopBar(title: String, onBack: () -> Unit) {
    val p = RsTheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = title,
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
    }
}

@Composable
internal fun SectionLabel(text: String) {
    val p = RsTheme
    Text(
        text = text,
        color = p.textSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
internal fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val p = RsTheme
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shape,
        color = p.surfaceElevated
    ) {
        Column(
            modifier = Modifier.clip(shape),
            content = content
        )
    }
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit = {},
    showArrow: Boolean = true
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            tint = p.iconMuted
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            color = p.textPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                color = p.textSecondary,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
        if (showArrow) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = p.textSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onTest: suspend (String) -> Result<String>
) {
    val p = RsTheme
    var text by rememberSaveable { mutableStateOf(initial) }
    var testing by remember { mutableStateOf(false) }
    var testMsg by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surfaceElevated,
        title = { Text("填写 API Key", color = p.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("sk-...", color = p.textPlaceholder) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            if (text.isBlank()) {
                                testMsg = "先填 Key 再测哦"
                                return@TextButton
                            }
                            testing = true
                            testMsg = "测试中…"
                            scope.launch {
                                val result = onTest(text.trim())
                                testMsg = result.fold(
                                    { "✓ $it" },
                                    { "✗ ${it.message ?: "连接失败"}" }
                                )
                                testing = false
                            }
                        },
                        enabled = !testing
                    ) {
                        Text(if (testing) "测试中…" else "测试连接", fontSize = 13.sp)
                    }
                    if (testMsg != null) {
                        Text(
                            testMsg!!,
                            color = if (testMsg!!.startsWith("✓")) p.brand else p.danger,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                    "Key 只保存在本机，用于直接调用 DeepSeek 开放平台",
                    color = p.textSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
