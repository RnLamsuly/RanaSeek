package com.ranaseek.app.ui.chat

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ranaseek.app.data.image.ImageStore
import com.ranaseek.app.data.model.Roles
import com.ranaseek.app.data.session.StoredFile
import com.ranaseek.app.data.settings.AppSettings
import com.ranaseek.app.data.settings.BubbleStyle
import com.ranaseek.app.ui.components.AppAvatar
import com.ranaseek.app.ui.components.InputPanel
import com.ranaseek.app.ui.components.MarkdownText
import com.ranaseek.app.ui.theme.RsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    settings: AppSettings,
    initialPrompt: String,
    initialDeepThinking: Boolean,
    initialWebSearch: Boolean,
    initialImages: List<String> = emptyList(),
    initialFiles: List<com.ranaseek.app.data.session.StoredFile> = emptyList(),
    sessions: List<com.ranaseek.app.data.session.StoredSession>,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteSessions: (List<String>) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onSetPinned: (List<String>, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit,
    onBackToHome: () -> Unit,
    vm: ChatViewModel
) {
    val p = RsTheme
    val state by vm.state.collectAsStateWithLifecycle()
    val speakingId by vm.speakingId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var input by rememberSaveable { mutableStateOf("") }
    var deepThinking by rememberSaveable { mutableStateOf(initialDeepThinking) }
    var webSearch by rememberSaveable { mutableStateOf(initialWebSearch) }
    var previewEnabled by rememberSaveable { mutableStateOf(false) }
    var autoSent by rememberSaveable { mutableStateOf(false) }

    // 语音输入
    val recorder = remember(context) { com.ranaseek.app.data.audio.VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var transcribing by remember { mutableStateOf(false) }
    val composeScope = androidx.compose.runtime.rememberCoroutineScope()
    val micPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
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
            composeScope.launch {
                val result = vm.transcribe(file)
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
                if (!recording) {
                    android.widget.Toast.makeText(context, "录音启动失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // 长按消息：浮动菜单 / 编辑对话框
    var editTarget by remember { mutableStateOf<UiMessage?>(null) }

    fun copyText(text: String) {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("RanaSeek", text))
        android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(
                android.content.Intent.createChooser(intent, "分享").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    // 图片输入（支持多选）
    var pendingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            composeScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    uris.mapNotNull {
                        com.ranaseek.app.data.image.ImageStore.saveChatImage(context, it)
                    }
                }
                pendingImages = pendingImages + saved
            }
        }
    }

    // 文件输入（上传到 Files API，支持多选）
    var pendingFiles by remember { mutableStateOf<List<StoredFile>>(emptyList()) }
    var uploadingCount by remember { mutableStateOf(0) }
    var showAttachSheet by remember { mutableStateOf(false) }
    val isAcrylic = settings.bubbleStyle == BubbleStyle.ACRYLIC
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadingCount += uris.size
            composeScope.launch {
                uris.forEach { uri ->
                    val mime = withContext(Dispatchers.IO) { context.contentResolver.getType(uri) }
                    if (mime?.startsWith("image/") == true) {
                        // 图片走缩略图预览，不走 Files API
                        val path = withContext(Dispatchers.IO) {
                            com.ranaseek.app.data.image.ImageStore.saveChatImage(context, uri)
                        }
                        uploadingCount -= 1
                        if (path != null) pendingImages = pendingImages + path
                    } else {
                        val result = vm.uploadAttachment(context, uri)
                        uploadingCount -= 1
                        result.onSuccess { f ->
                            pendingFiles = pendingFiles + StoredFile(f.id, f.name, f.sizeBytes)
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

    LaunchedEffect(Unit) {
        if (!autoSent && (initialPrompt.isNotBlank() || initialImages.isNotEmpty() || initialFiles.isNotEmpty())) {
            autoSent = true
            vm.send(
                prompt = initialPrompt,
                deepThinking = initialDeepThinking,
                webSearch = initialWebSearch,
                imagePaths = initialImages,
                files = initialFiles
            )
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.blocks) {
        if (state.messages.isNotEmpty()) {
            // 滚到末尾的锚点，保证长回答也能看到最新内容
            runCatching { listState.animateScrollToItem(state.messages.size) }
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
    // 消息气泡的位置（气泡也套用磨砂材质）
    val bubbleBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    // 滚动时只保留可见消息的磨砂区域，避免翻出窗口后残留
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String } }
            .collect { visibleKeys ->
                val alive = visibleKeys.toSet()
                bubbleBounds.keys.retainAll { it in alive }
            }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    // 系统返回键回到首页（保留当前会话）
    androidx.activity.compose.BackHandler { onBackToHome() }

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
                com.ranaseek.app.ui.components.DrawerContent(
                    sessions = sessions,
                    onSelectSession = {
                        drawerScope.launch { drawerState.close() }
                        onSelectSession(it)
                    },
                    onDeleteSession = onDeleteSession,
                    onDeleteSessions = onDeleteSessions,
                    onRenameSession = onRenameSession,
                    onTogglePin = onTogglePin,
                    onSetPinned = onSetPinned,
                    onOpenSettings = {
                        drawerScope.launch { drawerState.close() }
                        onOpenSettings()
                    }
                )
            }
        }
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 聊天背景
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap!!.asImageBitmap(),
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
            // 亚克力磨砂层：只在输入框和气泡区域显示模糊背景（各用各的圆角）
            val ac = acrylicBg
            val targets = buildList<Pair<androidx.compose.ui.geometry.Rect, Float>> {
                // 输入框圆角 28dp
                inputBounds?.let { add(it to 28f) }
                state.messages.forEach { m ->
                    // 关闭助手气泡后，助手消息不再有磨砂；气泡圆角 18dp
                    if (m.role == Roles.USER || settings.assistantBubble) {
                        bubbleBounds[m.id]?.let { add(it to 18f) }
                    }
                }
            }
            if (ac != null && targets.isNotEmpty()) {
                Image(
                    bitmap = ac.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            targets.forEach { (rect, cornerDp) ->
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    addRoundRect(
                                        androidx.compose.ui.geometry.RoundRect(
                                            rect,
                                            androidx.compose.ui.geometry.CornerRadius(cornerDp.dp.toPx())
                                        )
                                    )
                                }
                                clipPath(path) {
                                    this@drawWithContent.drawContent()
                                }
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
            ChatTopBar(
                title = state.title ?: "新对话",
                onMenu = { drawerScope.launch { drawerState.open() } },
                onNewChat = onNewChat
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val fade = 36.dp.toPx()
                        // 底部渐隐：被裁切的气泡边缘柔和消失，不出现硬直边
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startY = size.height - fade,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstOut
                        )
                        // 顶部渐隐
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = 0f,
                                endY = fade * 0.5f
                            ),
                            blendMode = BlendMode.DstOut
                        )
                    },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    // 消息被回收时清掉它的磨砂区域
                    DisposableEffect(msg.id) {
                        onDispose { bubbleBounds.remove(msg.id) }
                    }
                    if (msg.role == Roles.USER) {
                        UserMessageRow(
                            message = msg,
                            style = settings.bubbleStyle,
                            onCopy = { copyText(msg.text) },
                            onEdit = { editTarget = msg },
                            onShare = { shareText(msg.text) },
                            onBounds = { bubbleBounds[msg.id] = it }
                        )
                    } else {
                        AssistantMessage(
                            message = msg,
                            style = settings.bubbleStyle,
                            showBubble = settings.assistantBubble,
                            speaking = speakingId == msg.id,
                            acrylicTint = settings.acrylicTint,
                            onBounds = { bubbleBounds[msg.id] = it },
                            onSpeak = {
                                vm.speak(
                                    msg.id,
                                    msg.blocks.filterIsInstance<UiBlock.Text>()
                                        .joinToString("\n") { it.text }
                                )
                            },
                            onCopy = {
                                copyText(
                                    msg.blocks.filterIsInstance<UiBlock.Text>()
                                        .joinToString("\n") { it.text }
                                )
                            },
                            onRegenerate = {
                                vm.regenerate(msg.id, deepThinking, webSearch)
                            }
                        )
                    }
                }
                // 末尾锚点：滚到它就等于滚到最新内容底部
                item(key = "anchor") {
                    Spacer(Modifier.height(1.dp))
                }
            }

            if (state.streaming) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("正在回复…", color = p.textSecondary, fontSize = 12.sp)
                }
            }

            if (state.compressing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("正在压缩上下文…", color = p.brand, fontSize = 12.sp)
                }
            }

            // 列表与输入面板之间留一点呼吸感，避免气泡底部圆角贴着输入框
            Spacer(Modifier.height(6.dp))

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

            // 待发送附件卡片（参考官方：图标 + 文件名 + 大小 + 关闭）
            if (pendingImages.isNotEmpty() || pendingFiles.isNotEmpty() || uploadingCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uploadingCount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(p.surfaceElevated, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "正在上传 $uploadingCount 个文件…",
                                color = p.brand,
                                fontSize = 13.sp
                            )
                        }
                    }
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
                }

                // 附件快捷指令
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickChip("分析一下") { input = "分析一下" }
                    QuickChip("答案是什么") { input = "答案是什么" }
                    QuickChip("总结") { input = "总结" }
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
                    if ((input.isNotBlank() || hasAttachment) && !state.streaming && uploadingCount == 0) {
                        vm.send(
                            prompt = input,
                            deepThinking = deepThinking,
                            webSearch = webSearch,
                            imagePaths = pendingImages,
                            files = pendingFiles
                        )
                        input = ""
                        pendingImages = emptyList()
                        pendingFiles = emptyList()
                    }
                },
                onAddAttachment = { showAttachSheet = true },
                onVoice = { handleVoice() },
                isStreaming = state.streaming,
                isRecording = recording,
                hasAttachment = pendingImages.isNotEmpty() || pendingFiles.isNotEmpty(),
                acrylic = isAcrylic,
                acrylicTint = settings.acrylicTint,
                bgBitmap = acrylicBg,
                blurPx = settings.acrylicBlur,
                onBounds = { inputBounds = it },
                previewEnabled = previewEnabled,
                onTogglePreview = { previewEnabled = !previewEnabled },
                sendEnabled = uploadingCount == 0,
                onStop = { vm.stop() },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }
    }
    }

    if (showAttachSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = p.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                AttachOption("相册", Icons.Outlined.PhotoLibrary) {
                    showAttachSheet = false
                    imagePicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
                AttachOption("文件", Icons.Outlined.AttachFile) {
                    showAttachSheet = false
                    filePicker.launch("*/*")
                }
                AttachOption("压缩上下文", Icons.Outlined.Compress) {
                    showAttachSheet = false
                    vm.compressContext()
                }
            }
        }
    }

    // 修改消息对话框
    val editing = editTarget
    if (editing != null) {
        var draft by rememberSaveable { mutableStateOf(editing.text) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editTarget = null },
            containerColor = p.surfaceElevated,
            title = { Text("修改消息", color = p.textPrimary) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入新的内容", color = p.textPlaceholder) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "发送后会截断这条消息之后的对话，重新生成回复",
                        color = p.textSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        vm.editAndResend(editing.id, draft, deepThinking, webSearch)
                        editTarget = null
                    }
                ) {
                    Text("发送")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
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
private fun QuickChip(text: String, onClick: () -> Unit) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(p.surfaceElevated.copy(alpha = 0.75f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = p.textPrimary, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = p.textSecondary
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1fMB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format("%.0fKB", bytes / 1024f)
    bytes > 0 -> "${bytes}B"
    else -> "已就绪"
}

@Composable
private fun AttachOption(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChatTopBar(title: String, onMenu: () -> Unit, onNewChat: () -> Unit) {
    val p = RsTheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(40.dp)
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            // 官方风格：两条横线（历史抽屉）
            Canvas(modifier = Modifier.size(22.dp)) {
                val stroke = 2.dp.toPx()
                drawLine(
                    color = p.textPrimary,
                    start = Offset(0f, size.height * 0.36f),
                    end = Offset(size.width, size.height * 0.36f),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = p.textPrimary,
                    start = Offset(0f, size.height * 0.64f),
                    end = Offset(size.width, size.height * 0.64f),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        Text(
            text = title,
            color = p.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(40.dp)
                .clickable(onClick = onNewChat),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AddCircleOutline,
                contentDescription = "新对话",
                modifier = Modifier.size(22.dp),
                tint = p.textPrimary
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun UserMessageRow(
    message: UiMessage,
    style: BubbleStyle,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onBounds: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    val p = RsTheme
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // 附件卡片：显示在气泡正上方，左右排列（参考官方）
        if (message.imagePaths.isNotEmpty() || message.files.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                message.imagePaths.forEachIndexed { index, imgPath ->
                    UserAttachmentCard(
                        name = "图片 ${index + 1}",
                        sizeText = "",
                        imagePath = imgPath,
                        onRemove = {}
                    )
                }
                message.files.forEach { file ->
                    UserAttachmentCard(
                        name = file.name,
                        sizeText = formatSize(file.size),
                        imagePath = null,
                        onRemove = {}
                    )
                }
            }
            if (message.text.isNotBlank()) Spacer(Modifier.height(6.dp))
        }

        // 文字气泡：只有文字非空才显示，避免空气泡
        if (message.text.isNotBlank()) {
            Box {
                val isAcrylic = style == BubbleStyle.ACRYLIC
                val shape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .then(
                            if (isAcrylic) {
                                Modifier
                                    .background(p.textPrimary.copy(alpha = 0.16f), shape)
                                    .border(1.dp, p.textPrimary.copy(alpha = 0.22f), shape)
                            } else {
                                Modifier.background(p.surface, shape)
                            }
                        )
                        .clip(shape)
                        .onGloballyPositioned { onBounds?.invoke(it.boundsInRoot()) }
                        .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        color = p.textPrimary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }

                // 浮动菜单（参考官方：复制 / 修改 / 分享）
                androidx.compose.material3.DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.width(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = p.surfaceElevated,
                    tonalElevation = 0.dp
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        DropdownEntry(Icons.Outlined.ContentCopy, "复制") {
                            menuOpen = false
                            onCopy()
                        }
                        DropdownEntry(Icons.Outlined.Edit, "修改") {
                            menuOpen = false
                            onEdit()
                        }
                        if (message.text.isNotBlank()) {
                            DropdownEntry(Icons.Outlined.Share, "分享") {
                                menuOpen = false
                                onShare()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 附件卡片（图片缩略图 / 文档图标 + 文件名 + 大小 + 可移除）。 */
@Composable
private fun UserAttachmentCard(
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
                    com.ranaseek.app.data.image.ImageStore.loadChatImage(imagePath)
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

@Composable
private fun DropdownEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            tint = p.textPrimary
        )
        Spacer(Modifier.width(14.dp))
        Text(title, color = p.textPrimary, fontSize = 16.sp)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AssistantMessage(
    message: UiMessage,
    style: BubbleStyle,
    showBubble: Boolean,
    speaking: Boolean,
    acrylicTint: Int,
    onBounds: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val p = RsTheme
    val isAcrylic = style == BubbleStyle.ACRYLIC
    val shape = RoundedCornerShape(18.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(size = 24.dp)
            Spacer(Modifier.width(8.dp))
            Text("RanaSeek", color = p.textSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        !showBubble -> Modifier
                        // 失败且没有内容时不要空气泡，只显示重试行
                        message.error != null && message.blocks.isEmpty() -> Modifier
                        isAcrylic -> Modifier
                            .background(p.textPrimary.copy(alpha = 0.14f), shape)
                            .border(1.dp, p.textPrimary.copy(alpha = 0.20f), shape)

                        else -> Modifier.background(p.surface, shape)
                    }
                )
                .clip(shape)
                .onGloballyPositioned { onBounds?.invoke(it.boundsInRoot()) }
                .padding(
                    horizontal = if (showBubble) 14.dp else 0.dp,
                    vertical = if (showBubble) 12.dp else 0.dp
                )
        ) {
            // 思考 / 搜索 / 浏览合并成一条可折叠的过程时间线（参考官方）
            val processBlocks = message.blocks.filter {
                it is UiBlock.Thinking || it is UiBlock.Search || it is UiBlock.ReadLink
            }
            if (processBlocks.isNotEmpty()) {
                ThinkingProcessView(
                    blocks = processBlocks,
                    streaming = message.streaming,
                    acrylicTint = acrylicTint
                )
                if (message.blocks.any { it is UiBlock.Text }) {
                    Spacer(Modifier.height(8.dp))
                }
            }
            message.blocks.filterIsInstance<UiBlock.Text>().forEach { block ->
                TextBlockView(
                    block,
                    streaming = message.streaming,
                    acrylic = style == BubbleStyle.ACRYLIC,
                    acrylicTint = acrylicTint
                )
            }

            if (message.streaming && message.blocks.isEmpty()) {
                Text("▍", color = p.brand, fontSize = 16.sp)
            }

            if (message.error != null) {
                // 失败状态：橙色重试按钮 + 提示文案（参考官方）
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0A020))
                            .clickable(onClick = onRegenerate),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "重试",
                            modifier = Modifier.size(19.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = message.error,
                        color = p.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            if (message.stopReason == "refusal") {
                Text(
                    text = "这条内容被安全策略拦截了，换个问法试试吧～",
                    color = p.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            val readable = message.blocks.filterIsInstance<UiBlock.Text>().joinToString("\n") { it.text }
            if (!message.streaming && readable.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 复制
                    IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "复制",
                            modifier = Modifier.size(17.dp),
                            tint = p.textSecondary
                        )
                    }
                    // 重新生成
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "重新生成",
                            modifier = Modifier.size(17.dp),
                            tint = p.textSecondary
                        )
                    }
                    // 朗读
                    IconButton(onClick = onSpeak, modifier = Modifier.size(30.dp)) {
                        Icon(
                            if (speaking) Icons.Outlined.Stop else Icons.Outlined.VolumeUp,
                            contentDescription = if (speaking) "停止朗读" else "朗读回答",
                            modifier = Modifier.size(17.dp),
                            tint = if (speaking) p.brand else p.textSecondary
                        )
                    }
                    val usage = message.usage
                    if (usage != null && !usage.isEmpty) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = usage.formatted(),
                            color = p.textSecondary.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingProcessView(
    blocks: List<UiBlock>,
    streaming: Boolean,
    acrylicTint: Int
) {
    val p = RsTheme
    var expanded by remember { mutableStateOf(true) }
    // 生成完毕后默认折叠
    LaunchedEffect(streaming) {
        if (!streaming) expanded = false
    }

    val durationMs = blocks.filterIsInstance<UiBlock.Thinking>()
        .maxOfOrNull { it.durationMs } ?: 0L
    val title = when {
        streaming -> "思考中…"
        durationMs > 0 -> "已深度思考（用时 ${(durationMs / 1000f).roundToInt()} 秒）"
        else -> "已深度思考"
    }

    Column {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = p.textSecondary, fontSize = 13.sp)
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(start = 2.dp)
                    .rotate(if (expanded) 180f else 0f),
                tint = p.textSecondary
            )
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            // 过滤：搜索 0 网页、空思考不显示
            val entries = blocks.filterNotNull().filter { b ->
                when (b) {
                    is UiBlock.Thinking -> b.text.isNotBlank()
                    is UiBlock.Search -> b.results.isNotEmpty()
                    is UiBlock.ReadLink -> true
                    else -> false
                }
            }
            Column {
                entries.forEachIndexed { index, block ->
                    TimelineRow(
                        isFirst = index == 0,
                        isLast = index == entries.lastIndex
                    ) {
                        when (block) {
                            is UiBlock.Thinking -> {
                                Text(
                                    text = block.text,
                                    color = p.textSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            is UiBlock.Search -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = p.textSecondary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "搜索到 ${block.results.size} 个网页",
                                        color = p.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            is UiBlock.ReadLink -> {
                                val label = block.title?.takeIf { it.isNotBlank() }
                                    ?: block.url?.takeIf { it.isNotBlank() }
                                    ?: "网页"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = p.brand.copy(alpha = 0.8f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "已浏览：$label",
                                        color = p.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

/** 时间线条目：左侧点线串联（参考官方 workflow 样式）。 */
@Composable
private fun TimelineRow(
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable () -> Unit
) {
    val p = RsTheme
    Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .drawBehind {
                    val cx = size.width / 2f
                    val dotY = 9.dp.toPx()
                    val lineWidth = 1.5.dp.toPx()
                    if (!isFirst) {
                        drawLine(
                            color = p.divider,
                            start = androidx.compose.ui.geometry.Offset(cx, 0f),
                            end = androidx.compose.ui.geometry.Offset(cx, dotY),
                            strokeWidth = lineWidth
                        )
                    }
                    if (!isLast) {
                        drawLine(
                            color = p.divider,
                            start = androidx.compose.ui.geometry.Offset(cx, dotY),
                            end = androidx.compose.ui.geometry.Offset(cx, size.height),
                            strokeWidth = lineWidth
                        )
                    }
                    drawCircle(
                        color = p.textSecondary.copy(alpha = 0.6f),
                        radius = 2.5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(cx, dotY)
                    )
                }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun TextBlockView(
    block: UiBlock.Text,
    streaming: Boolean,
    acrylic: Boolean = false,
    acrylicTint: Int = 38
) {
    val p = RsTheme
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column {
        MarkdownText(
            text = block.text,
            color = p.textPrimary,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            acrylic = acrylic,
            acrylicTint = acrylicTint
        )
        if (streaming && block.text.isBlank()) {
            Text("▍", color = p.brand, fontSize = 16.sp)
        }
        if (block.citations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row {
                block.citations.forEachIndexed { i, c ->
                    Text(
                        text = "[${i + 1}]",
                        color = p.brand,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { runCatching { uriHandler.openUri(c.url) } }
                    )
                }
            }
            block.citations.forEachIndexed { i, c ->
                Text(
                    text = "${i + 1}. ${c.title ?: c.url}",
                    color = p.brand.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable { runCatching { uriHandler.openUri(c.url) } }
                )
            }
        }
    }
}

@Composable
private fun SearchBlockView(block: UiBlock.Search) {
    val p = RsTheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(p.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, p.divider, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = p.brand
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (block.results.isEmpty() && block.queries.isNotEmpty())
                    "搜索中：${block.queries.joinToString(" / ")}"
                else "已搜索 ${block.results.size} 个网页",
                color = p.textSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = p.textSecondary
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            block.queries.forEach { q ->
                Text("· $q", color = p.textSecondary, fontSize = 12.sp)
            }
            block.results.forEach { r ->
                Column(Modifier.padding(top = 6.dp)) {
                    Text(
                        text = r.title ?: r.url ?: "",
                        color = p.textPrimary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = r.siteName ?: r.url ?: "",
                        color = p.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadLinkView(block: UiBlock.ReadLink) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .background(p.surface.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .border(1.dp, p.divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Link,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = p.brand
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "阅读网页：${block.title ?: block.url ?: ""}",
            color = p.textSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
