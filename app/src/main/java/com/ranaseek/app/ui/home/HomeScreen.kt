package com.ranaseek.app.ui.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.data.audio.VoiceRecorder
import com.ranaseek.app.data.image.ImageStore
import com.ranaseek.app.data.repo.UploadedFile
import com.ranaseek.app.data.session.StoredSession
import com.ranaseek.app.data.settings.AppSettings
import com.ranaseek.app.ui.components.AppAvatar
import com.ranaseek.app.ui.components.DrawerContent
import com.ranaseek.app.ui.components.InputPanel
import com.ranaseek.app.ui.components.UserAttachmentCard
import com.ranaseek.app.ui.components.formatSize
import com.ranaseek.app.ui.theme.RsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    sessions: List<StoredSession>,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteSessions: (List<String>) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onSetPinned: (List<String>, Boolean) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartChat: (String, Boolean, Boolean, List<String>, List<com.ranaseek.app.data.session.StoredFile>) -> Unit,
    uploadAttachment: suspend (android.content.Context, android.net.Uri) -> Result<UploadedFile>,
    transcribe: suspend (File) -> Result<String>
) {
    val p = RsTheme
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var input by rememberSaveable { mutableStateOf("") }
    var deepThinking by rememberSaveable { mutableStateOf(false) }
    var webSearch by rememberSaveable { mutableStateOf(false) }
    var previewEnabled by rememberSaveable { mutableStateOf(false) }

    // 附件与语音（支持多选）
    var pendingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingFiles by remember { mutableStateOf<List<com.ranaseek.app.data.session.StoredFile>>(emptyList()) }
    var uploadingCount by remember { mutableStateOf(0) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var transcribing by remember { mutableStateOf(false) }

    val recorder = remember(context) { VoiceRecorder(context) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    uris.mapNotNull { ImageStore.saveChatImage(context, it) }
                }
                pendingImages = pendingImages + saved
            }
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadingCount += uris.size
            scope.launch {
                uris.forEach { uri ->
                    val mime = withContext(Dispatchers.IO) { context.contentResolver.getType(uri) }
                    if (mime?.startsWith("image/") == true) {
                        val path = withContext(Dispatchers.IO) { ImageStore.saveChatImage(context, uri) }
                        uploadingCount -= 1
                        if (path != null) pendingImages = pendingImages + path
                    } else {
                        val result = uploadAttachment(context, uri)
                        uploadingCount -= 1
                        result.onSuccess { f ->
                            pendingFiles = pendingFiles + com.ranaseek.app.data.session.StoredFile(
                                f.id, f.name, f.sizeBytes
                            )
                        }.onFailure {
                            android.widget.Toast.makeText(
                                context,
                                it.message ?: "上传失败",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recording = recorder.start()
        } else {
            android.widget.Toast.makeText(context, "需要麦克风权限才能语音输入", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun handleVoice() {
        if (transcribing) return
        if (recording) {
            val file = recorder.stop()
            recording = false
            if (file == null) return
            transcribing = true
            scope.launch {
                val result = transcribe(file)
                transcribing = false
                result.onSuccess { if (it.isNotBlank()) input = it }
                    .onFailure {
                        android.widget.Toast.makeText(
                            context,
                            it.message ?: "识别失败",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                file.delete()
            }
        } else {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                recording = recorder.start()
            } else {
                micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val bgBitmap by produceState<android.graphics.Bitmap?>(null, settings.backgroundPath) {
        value = withContext(Dispatchers.IO) { ImageStore.loadBackground(settings.backgroundPath) }
    }
    // 亚克力磨砂：预生成一张模糊位图给输入面板用
    val acrylicBg = remember(bgBitmap, settings.acrylicBlur) {
        val b = bgBitmap
        if (b == null || settings.acrylicBlur <= 0) {
            null
        } else {
            runCatching { ImageStore.blurForAcrylic(b, settings.acrylicBlur) }.getOrNull()
        }
    }
    // 输入面板在屏幕上的位置（用于在背景层裁剪磨砂区域）
    var inputBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerShape = RectangleShape,
                drawerContainerColor = p.bgSecondary,
                drawerTonalElevation = 0.dp
            ) {
                DrawerContent(
                    sessions = sessions,
                    onSelectSession = {
                        scope.launch { drawerState.close() }
                        onSelectSession(it)
                    },
                    onDeleteSession = onDeleteSession,
                    onDeleteSessions = onDeleteSessions,
                    onRenameSession = onRenameSession,
                    onTogglePin = onTogglePin,
                    onSetPinned = onSetPinned,
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bmp = bgBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix().apply {
                            setToSaturation((settings.acrylicSaturation / 100f).coerceIn(1f, 2f))
                        }
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= 31 && settings.backgroundBlur > 0) {
                                Modifier.blur(settings.backgroundBlur.dp)
                            } else Modifier
                        )
                )
                // 亚克力磨砂层：只在输入框区域显示模糊背景（参考 DSH backdrop blur）
                val ac = acrylicBg
                val bounds = inputBounds
                if (ac != null && bounds != null) {
                    Image(
                        bitmap = ac.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    addRoundRect(
                                        androidx.compose.ui.geometry.RoundRect(
                                            bounds,
                                            androidx.compose.ui.geometry.CornerRadius(26.dp.toPx())
                                        )
                                    )
                                }
                                clipPath(path) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (p.isDark) {
                                Color(0xFF02050F).copy(alpha = (settings.backgroundDim / 100f + 0.12f).coerceIn(0f, 0.9f))
                            } else {
                                Color(0xFF070C18).copy(alpha = (settings.backgroundDim / 100f).coerceIn(0f, 0.8f))
                            }
                        )
                )
            } else {
                Box(Modifier.fillMaxSize().background(p.bg))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                HomeTopBar(
                    onMenu = { scope.launch { drawerState.open() } },
                    onNewChat = onNewChat
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-36).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppAvatar(size = 92.dp)
                        Spacer(Modifier.height(26.dp))
                        Text(
                            text = "你好，我能帮什么忙吗？",
                            style = MaterialTheme.typography.headlineLarge,
                            color = p.textPrimary
                        )
                    }
                }

                // 待发送附件卡片（多选，左右排列）
                if (pendingImages.isNotEmpty() || pendingFiles.isNotEmpty() || uploadingCount > 0) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pendingImages.forEachIndexed { index, imgPath ->
                            UserAttachmentCard(
                                name = "图片 ${index + 1}",
                                sizeText = "",
                                imagePath = imgPath,
                                onRemove = { pendingImages = pendingImages.filterIndexed { i, _ -> i != index } }
                            )
                        }
                        pendingFiles.forEachIndexed { index, file ->
                            UserAttachmentCard(
                                name = file.name,
                                sizeText = formatSize(file.size),
                                imagePath = null,
                                onRemove = { pendingFiles = pendingFiles.filterIndexed { i, _ -> i != index } }
                            )
                        }
                        if (uploadingCount > 0) {
                            Text(
                                "正在上传 $uploadingCount 个文件…",
                                color = p.brand,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                if (recording || transcribing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (recording) "正在录音…再点一次麦克风结束" else "正在识别语音…",
                            color = if (recording) p.danger else p.brand,
                            fontSize = 12.sp
                        )
                    }
                }

                InputPanel(
                    text = input,
                    onTextChange = { input = it },
                    deepThinking = deepThinking,
                    onToggleDeepThinking = { deepThinking = !deepThinking },
                    webSearch = webSearch,
                    onToggleWebSearch = { webSearch = !webSearch },
                    onSend = {
                        val hasAttachment = pendingImages.isNotEmpty() || pendingFiles.isNotEmpty()
                        if ((input.isNotBlank() || hasAttachment) && uploadingCount == 0) {
                            onStartChat(
                                input.trim(),
                                deepThinking,
                                webSearch,
                                pendingImages,
                                pendingFiles
                            )
                            input = ""
                            pendingImages = emptyList()
                            pendingFiles = emptyList()
                        }
                    },
                    onAddAttachment = { showAttachSheet = true },
                    onVoice = { handleVoice() },
                    isRecording = recording,
                    hasAttachment = pendingImages.isNotEmpty() || pendingFiles.isNotEmpty(),
                    acrylic = settings.bubbleStyle == com.ranaseek.app.data.settings.BubbleStyle.ACRYLIC,
                    acrylicTint = settings.acrylicTint,
                    bgBitmap = acrylicBg,
                    blurPx = settings.acrylicBlur,
                    onBounds = { inputBounds = it },
                    previewEnabled = previewEnabled,
                    onTogglePreview = { previewEnabled = !previewEnabled },
                    sendEnabled = uploadingCount == 0,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                )
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = p.surface
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
                AttachOption("相册", Icons.Outlined.PhotoLibrary) {
                    showAttachSheet = false
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                AttachOption("文件", Icons.Outlined.AttachFile) {
                    showAttachSheet = false
                    filePicker.launch("*/*")
                }
            }
        }
    }
}

@Composable
private fun AttachOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = p.textPrimary)
        Spacer(Modifier.width(16.dp))
        Text(title, color = p.textPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun HomeTopBar(
    onMenu: () -> Unit,
    onNewChat: () -> Unit
) {
    val p = RsTheme
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val stroke = 2.dp.toPx()
                drawLine(
                    color = p.textPrimary,
                    start = Offset(0f, size.height * 0.36f),
                    end = Offset(size.width, size.height * 0.36f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = p.textPrimary,
                    start = Offset(0f, size.height * 0.64f),
                    end = Offset(size.width, size.height * 0.64f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            // 已经在主页 = 已在新建对话状态，阻止并提示
            android.widget.Toast.makeText(
                context,
                "已在新对话中",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }) {
            Icon(
                Icons.Outlined.AddCircleOutline,
                contentDescription = "新对话",
                modifier = Modifier.size(24.dp),
                tint = p.textPrimary
            )
        }
    }
}
