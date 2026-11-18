package com.ranaseek.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ranaseek.app.data.model.ChatMessage
import com.ranaseek.app.data.model.Citation
import com.ranaseek.app.data.model.Roles
import com.ranaseek.app.data.model.SearchResult
import com.ranaseek.app.data.model.asJsonObjectOrNull
import com.ranaseek.app.data.model.queriesOf
import com.ranaseek.app.data.model.urlOf
import com.ranaseek.app.data.repo.ChatRepository
import com.ranaseek.app.data.repo.ChatStreamEvent
import com.ranaseek.app.data.session.SessionStore
import com.ranaseek.app.data.session.StoredFile
import com.ranaseek.app.data.session.StoredMessage
import com.ranaseek.app.data.session.StoredSession
import com.ranaseek.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

sealed interface UiBlock {
    data class Thinking(
        val text: String = "",
        val done: Boolean = false,
        val durationMs: Long = 0
    ) : UiBlock

    data class Text(
        val text: String = "",
        val citations: List<Citation> = emptyList()
    ) : UiBlock

    data class Search(
        val toolUseId: String? = null,
        val queries: List<String> = emptyList(),
        val kind: String? = null,
        val results: List<SearchResult> = emptyList(),
        val open: Boolean = false
    ) : UiBlock

    data class ReadLink(
        val toolUseId: String? = null,
        val url: String? = null,
        val title: String? = null
    ) : UiBlock
}

data class UsageInfo(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cacheReadTokens: Int = 0,
    val thinkingTokens: Int = 0
) {
    val isEmpty: Boolean get() = inputTokens == 0 && outputTokens == 0

    fun formatted(): String {
        fun fmt(n: Int) =
            if (n >= 10000) "${n / 1000}k"
            else if (n >= 1000) String.format("%.1fk", n / 1000f)
            else "$n"

        val sb = StringBuilder("↑${fmt(inputTokens)} · ↓${fmt(outputTokens)}")
        if (thinkingTokens > 0) sb.append(" · 思考${fmt(thinkingTokens)}")
        if (cacheReadTokens > 0) sb.append(" · 缓存${fmt(cacheReadTokens)}")
        return sb.toString()
    }
}

data class UiMessage(
    val id: String,
    val role: String,
    val text: String = "",
    val imagePaths: List<String> = emptyList(),
    val files: List<StoredFile> = emptyList(),
    val blocks: List<UiBlock> = emptyList(),
    val rawContent: JsonArray? = null,
    val streaming: Boolean = false,
    val stopReason: String? = null,
    val error: String? = null,
    val usage: UsageInfo? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val streaming: Boolean = false,
    val title: String? = null,
    val error: String? = null,
    val compressing: Boolean = false,
    /** 压缩摘要与实际发送无关的 UI 历史并存 */
    val summary: String? = null,
    val summaryCount: Int = 0
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val store = SessionStore(app)
    private val repo = ChatRepository()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _sessions = MutableStateFlow<List<StoredSession>>(emptyList())
    val sessions: StateFlow<List<StoredSession>> = _sessions.asStateFlow()

    private var currentSessionId: String? = null
    private var streamJob: Job? = null

    private val blockRaw = mutableMapOf<Int, JsonObject>()
    private val blockUi = mutableMapOf<Int, UiBlock>()
    private val blockOrder = mutableListOf<Int>()
    private var thinkingStartMs = 0L
    private var thinkingEndMs = 0L

    // ---------- 朗读 TTS ----------

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var currentTtsFile: java.io.File? = null
    private val _speakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _sessions.value = store.load()
                .sortedWith(compareByDescending<StoredSession> { it.pinned }.thenByDescending { it.updatedAt })
        }
    }

    // ---------- 会话管理 ----------

    fun newChat() {
        streamJob?.cancel()
        currentSessionId = null
        resetAccumulator()
        _state.value = ChatUiState()
    }

    fun openSession(id: String) {
        if (currentSessionId == id) return
        val session = _sessions.value.firstOrNull { it.id == id } ?: return
        streamJob?.cancel()
        currentSessionId = id
        resetAccumulator()
        _state.value = ChatUiState(
            messages = session.messages.map { storedToUi(it) },
            title = session.title,
            summary = session.summary,
            summaryCount = session.summaryCount
        )
    }

    fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
        viewModelScope.launch(Dispatchers.IO) { store.save(_sessions.value) }
        if (currentSessionId == id) newChat()
    }

    fun deleteSessions(ids: List<String>) {
        val set = ids.toSet()
        _sessions.value = _sessions.value.filterNot { it.id in set }
        viewModelScope.launch(Dispatchers.IO) { store.save(_sessions.value) }
        if (currentSessionId in set) newChat()
    }

    fun renameSession(id: String, title: String) {
        val clean = title.trim()
        if (clean.isBlank()) return
        _sessions.value = _sessions.value.map {
            if (it.id == id) it.copy(title = clean, updatedAt = System.currentTimeMillis()) else it
        }
        if (currentSessionId == id) {
            _state.value = _state.value.copy(title = clean)
        }
        viewModelScope.launch(Dispatchers.IO) { store.save(_sessions.value) }
    }

    fun togglePin(id: String) {
        _sessions.value = _sessions.value.map {
            if (it.id == id) it.copy(pinned = !it.pinned) else it
        }.sortedWith(compareByDescending<StoredSession> { it.pinned }.thenByDescending { it.updatedAt })
        viewModelScope.launch(Dispatchers.IO) { store.save(_sessions.value) }
    }

    fun setPinned(ids: List<String>, pinned: Boolean) {
        val set = ids.toSet()
        _sessions.value = _sessions.value.map {
            if (it.id in set) it.copy(pinned = pinned) else it
        }.sortedWith(compareByDescending<StoredSession> { it.pinned }.thenByDescending { it.updatedAt })
        viewModelScope.launch(Dispatchers.IO) { store.save(_sessions.value) }
    }

    // ---------- 发送 ----------

    fun send(
        prompt: String,
        deepThinking: Boolean,
        webSearch: Boolean,
        imagePaths: List<String> = emptyList(),
        files: List<StoredFile> = emptyList()
    ) {
        if (_state.value.streaming) return
        val clean = prompt.trim()
        if (clean.isEmpty() && imagePaths.isEmpty() && files.isEmpty()) return

        resetAccumulator()

        val userMsg = UiMessage(
            id = UUID.randomUUID().toString(),
            role = Roles.USER,
            text = clean,
            imagePaths = imagePaths,
            files = files
        )
        val assistantMsg = UiMessage(
            id = UUID.randomUUID().toString(),
            role = Roles.ASSISTANT,
            streaming = true
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg + assistantMsg,
            streaming = true,
            error = null
        )
        persist()

        streamJob = viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val apiKey = settings.apiKey
            if (apiKey.isBlank()) {
                finishWithError("还没有填写 API Key，去「设置 → API Key」填一下吧", assistantMsg.id)
                return@launch
            }

            val baseMessages = _state.value.messages.dropLast(1)
            val summary = _state.value.summary
            val cut = _state.value.summaryCount.coerceAtMost(baseMessages.size)
            val historyMessages = if (!summary.isNullOrBlank() && cut > 0) {
                baseMessages.drop(cut)
            } else {
                baseMessages
            }
            val history = historyMessages.map { it.toChatMessage() }

            val effectiveSystem = buildString {
                if (!settings.globalPrompt.isNullOrBlank()) append(settings.globalPrompt)
                if (!summary.isNullOrBlank() && cut > 0) {
                    if (isNotEmpty()) append("\n\n")
                    append("【之前对话的摘要（较早历史已压缩）】\n").append(summary)
                }
            }

            val titleEnabled = history.none { it.role == Roles.ASSISTANT }
            val hasFileReference = _state.value.messages.any { it.files.isNotEmpty() }

            val code = try {
                repo.streamChat(
                    apiKey = apiKey,
                    model = settings.model.ifBlank { "deepseek-flash" },
                    systemPrompt = effectiveSystem,
                    messages = history,
                    deepThinking = deepThinking,
                    webSearch = webSearch,
                    titleEnabled = titleEnabled,
                    hasFileReference = hasFileReference
                ) { event -> handleEvent(event, assistantMsg.id) }
            } catch (e: Exception) {
                finishWithError("网络异常：${e.message ?: "连接失败"}", assistantMsg.id)
                return@launch
            }

            if (code != 200) {
                // HTTP 错误（401 鉴权失败 / 429 限流 / 5xx 等）直接显示具体原因，
                // 不要当成「网络异常」，避免误导
                finishWithError(ChatRepository.httpErrorMessage(code, null), assistantMsg.id)
            } else if (_state.value.messages.lastOrNull()?.streaming == true) {
                finishStreaming(assistantMsg.id, incomplete = true)
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        val last = _state.value.messages.lastOrNull() ?: return
        if (last.streaming) {
            finishStreaming(last.id, incomplete = true)
        }
    }

    /**
     * 重新生成：找到这条助手消息前面最近的用户消息，截断后重发。
     */
    fun regenerate(assistantMessageId: String, deepThinking: Boolean, webSearch: Boolean) {
        if (_state.value.streaming) return
        val messages = _state.value.messages
        val idx = messages.indexOfFirst { it.id == assistantMessageId }
        if (idx < 0 || messages[idx].role != Roles.ASSISTANT) return
        val userIdx = (idx - 1 downTo 0).firstOrNull { messages[it].role == Roles.USER } ?: return
        val userMsg = messages[userIdx]
        _state.value = _state.value.copy(messages = messages.take(userIdx), summary = null, summaryCount = 0)
        persist()
        send(
            prompt = userMsg.text,
            deepThinking = deepThinking,
            webSearch = webSearch,
            imagePaths = userMsg.imagePaths,
            files = userMsg.files
        )
    }

    /**
     * 回溯/修改消息：把某条用户消息改成新内容，并截断它之后的所有消息重新生成。
     */
    fun editAndResend(
        messageId: String,
        newText: String,
        deepThinking: Boolean,
        webSearch: Boolean
    ) {
        if (_state.value.streaming) return
        val messages = _state.value.messages
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx < 0 || messages[idx].role != Roles.USER) return
        val target = messages[idx]
        val newClean = newText.trim()
        if (newClean.isBlank() && target.imagePaths.isEmpty() && target.files.isEmpty()) return

        // 截断到这条消息之前（摘要基于旧历史，一并清掉）
        _state.value = _state.value.copy(messages = messages.take(idx), summary = null, summaryCount = 0)
        persist()

        send(
            prompt = newClean,
            deepThinking = deepThinking,
            webSearch = webSearch,
            imagePaths = target.imagePaths,
            files = target.files
        )
    }

    /** 语音识别（ASR）：录音文件 → 文本。 */
    suspend fun transcribe(file: java.io.File): Result<String> {
        val settings = settingsRepo.settings.first()
        val key = settings.apiKey
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("还没有填写 API Key"))
        }
        return repo.transcribe(key, file)
    }

    /** 上传本地文件（PDF/Word 等）到 Files API。 */
    suspend fun uploadAttachment(
        context: android.content.Context,
        uri: android.net.Uri
    ): Result<com.ranaseek.app.data.repo.UploadedFile> {
        val settings = settingsRepo.settings.first()
        val key = settings.apiKey
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("还没有填写 API Key"))
        }
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                var name = "file"
                resolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) {
                        name = c.getString(idx) ?: "file"
                    }
                }
                val mime = resolver.getType(uri)
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("读取文件失败")
                repo.uploadFile(key, name, mime, bytes).getOrThrow()
            }
        }
    }

    /**
     * 压缩上下文：把整段对话总结成一条摘要；UI 保留全部消息，仅请求时用摘要替代。
     */
    fun compressContext() {
        if (_state.value.streaming || _state.value.compressing) return
        val messages = _state.value.messages
        if (messages.isEmpty()) return

        val transcript = messages.joinToString("\n\n") { m ->
            if (m.role == Roles.USER) {
                buildString {
                    append("用户：")
                    if (m.text.isNotBlank()) append(m.text)
                    if (m.imagePaths.isNotEmpty()) append(" [图片×${m.imagePaths.size}]")
                    if (m.files.isNotEmpty()) append(" [文件" + m.files.joinToString("、") { it.name } + "]")
                }
            } else {
                "助手：" + m.blocks.filterIsInstance<UiBlock.Text>().joinToString("\n") { it.text }
            }
        }.trim()
        if (transcript.isBlank()) return

        _state.value = _state.value.copy(compressing = true)
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val key = settings.apiKey
            if (key.isBlank()) {
                _state.value = _state.value.copy(compressing = false)
                return@launch
            }
            val builder = StringBuilder()
            runCatching {
                repo.streamChat(
                    apiKey = key,
                    model = settings.model.ifBlank { "deepseek-flash" },
                    systemPrompt = null,
                    messages = listOf(
                        ChatMessage(
                            role = Roles.USER,
                            content = JsonPrimitive(
                                "请把下面的对话压缩成一段简洁的上下文摘要，保留关键事实、结论、" +
                                    "用户偏好和未完成的事项。只输出摘要本身，不要客套话。\n\n$transcript"
                            )
                        )
                    ),
                    deepThinking = false,
                    webSearch = false,
                    titleEnabled = false,
                    hasFileReference = false
                ) { event ->
                    if (event is ChatStreamEvent.TextDelta) builder.append(event.text)
                }
            }
            val summary = builder.toString().trim()
            _state.value = _state.value.copy(compressing = false)
            if (summary.isBlank()) {
                android.widget.Toast.makeText(
                    getApplication(),
                    "压缩失败，请再试一次",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            // UI 保留全部历史，只记录摘要；发请求时用摘要替代前 N 条消息
            _state.value = _state.value.copy(
                summary = summary,
                summaryCount = _state.value.messages.size
            )
            persist()
            android.widget.Toast.makeText(
                getApplication(),
                "上下文已压缩（界面保留历史，仅请求使用摘要）",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 朗读：只有点击按钮才调用 TTS；同一消息有缓存时直接播放，不重复请求。
     */
    fun speak(messageId: String, text: String) {
        if (_speakingId.value == messageId) {
            stopSpeak()
            return
        }
        stopSpeak()
        if (text.isBlank()) return
        _speakingId.value = messageId
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            val cacheFile = java.io.File(cacheDir, "tts_${messageId}.wav")

            // 没有缓存时才请求 TTS
            if (!cacheFile.exists() || cacheFile.length() < 1000) {
                val settings = settingsRepo.settings.first()
                val key = settings.apiKey
                if (key.isBlank()) {
                    _speakingId.value = null
                    return@launch
                }
                val result = repo.tts(key, text)
                val bytes = result.getOrNull()
                if (bytes == null) {
                    android.util.Log.e("RanaSeekTTS", "tts failed", result.exceptionOrNull())
                    _speakingId.value = null
                    return@launch
                }
                // 只保留当前消息的音频缓存
                cacheDir.listFiles { f -> f.name.startsWith("tts_") && f.name != cacheFile.name }
                    ?.forEach { it.delete() }
                runCatching { cacheFile.writeBytes(bytes) }
            }

            runCatching {
                val mp = android.media.MediaPlayer()
                mp.setDataSource(cacheFile.absolutePath)
                mp.setOnCompletionListener { stopSpeak() }
                mp.setOnErrorListener { _, what, extra ->
                    android.util.Log.e("RanaSeekTTS", "media error what=$what extra=$extra")
                    stopSpeak()
                    true
                }
                mp.prepare()
                mp.start()
                mediaPlayer = mp
                currentTtsFile = cacheFile
            }.onFailure {
                android.util.Log.e("RanaSeekTTS", "play failed", it)
                _speakingId.value = null
            }
        }
    }

    fun stopSpeak() {
        mediaPlayer?.let { mp ->
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
        _speakingId.value = null
    }

    // ---------- 流式事件 ----------

    private suspend fun handleEvent(event: ChatStreamEvent, messageId: String) {
        when (event) {
            is ChatStreamEvent.Started -> Unit

            is ChatStreamEvent.BlockStarted -> {
                if (thinkingStartMs == 0L && event.block["type"]?.jsonPrimitive?.contentOrNull == "thinking") {
                    thinkingStartMs = System.currentTimeMillis()
                }
                blockRaw[event.index] = event.block
                if (!blockOrder.contains(event.index)) blockOrder.add(event.index)
                val ui = blockToUi(event.block)
                if (ui != null) {
                    blockUi[event.index] = ui
                    publishBlocks(messageId)
                }
            }

            is ChatStreamEvent.TextDelta -> {
                val cur = blockUi[event.index] as? UiBlock.Text ?: UiBlock.Text()
                val updated = cur.copy(text = cur.text + event.text)
                blockUi[event.index] = updated
                appendRawText(event.index, "text", updated.text)
                publishBlocks(messageId)
            }

            is ChatStreamEvent.ThinkingDelta -> {
                val cur = blockUi[event.index] as? UiBlock.Thinking ?: UiBlock.Thinking()
                val updated = cur.copy(text = cur.text + event.text)
                blockUi[event.index] = updated
                appendRawText(event.index, "thinking", updated.text)
                publishBlocks(messageId)
            }

            is ChatStreamEvent.SignatureDelta -> {
                val raw = blockRaw[event.index] ?: return
                blockRaw[event.index] = JsonObject(raw + ("signature" to JsonPrimitive(event.signature)))
            }

            is ChatStreamEvent.CitationDelta -> {
                val cur = blockUi[event.index] as? UiBlock.Text ?: UiBlock.Text()
                blockUi[event.index] = cur.copy(citations = cur.citations + event.citation)
                publishBlocks(messageId)
            }

            is ChatStreamEvent.BlockStopped -> {
                when (val cur = blockUi[event.index]) {
                    is UiBlock.Thinking -> {
                        if (thinkingEndMs == 0L) thinkingEndMs = System.currentTimeMillis()
                        val duration =
                            if (thinkingStartMs > 0) (thinkingEndMs - thinkingStartMs).coerceAtLeast(300) else 0
                        blockUi[event.index] = cur.copy(done = true, durationMs = duration)
                        publishBlocks(messageId)
                    }
                    else -> Unit
                }
            }

            is ChatStreamEvent.MessageDelta -> {
                val title = event.title
                if (!title.isNullOrBlank()) {
                    _state.value = _state.value.copy(title = title)
                }
                if (event.stopReason != null) {
                    updateAssistant(messageId) { it.copy(stopReason = event.stopReason) }
                }
                event.usage?.let { u ->
                    val details = u["output_tokens_details"]?.asJsonObjectOrNull()
                    val info = UsageInfo(
                        inputTokens = u["input_tokens"]?.jsonPrimitive?.intOrNull ?: 0,
                        outputTokens = u["output_tokens"]?.jsonPrimitive?.intOrNull ?: 0,
                        cacheReadTokens = u["cache_read_input_tokens"]?.jsonPrimitive?.intOrNull ?: 0,
                        thinkingTokens = details?.get("thinking_tokens")?.jsonPrimitive?.intOrNull ?: 0
                    )
                    updateAssistant(messageId) { it.copy(usage = info) }
                }
                if (event.stopReason == "refusal") {
                    blockUi.clear()
                    blockOrder.clear()
                    publishBlocks(messageId)
                }
            }

            is ChatStreamEvent.Stopped -> finishStreaming(messageId, incomplete = false)

            is ChatStreamEvent.Failed -> {
                finishWithError(
                    event.error.message ?: "服务端返回错误（${event.error.type ?: "unknown"}）",
                    messageId
                )
            }
        }
    }

    private fun blockToUi(block: JsonObject): UiBlock? {
        return when (block["type"]?.jsonPrimitive?.contentOrNull) {
            "thinking" -> UiBlock.Thinking()
            "text" -> {
                val citations = (block["citations"] as? JsonArray)?.mapNotNull {
                    Citation.fromJson(it.asJsonObjectOrNull() ?: return@mapNotNull null)
                } ?: emptyList()
                UiBlock.Text(citations = citations)
            }
            "server_tool_use" -> {
                val name = block["name"]?.jsonPrimitive?.contentOrNull
                val kind = block["kind"]?.jsonPrimitive?.contentOrNull
                val id = block["id"]?.jsonPrimitive?.contentOrNull
                if (name == "deepseek_web_search") {
                    UiBlock.Search(toolUseId = id, queries = block.queriesOf(), kind = kind)
                } else {
                    UiBlock.ReadLink(toolUseId = id, url = block.urlOf())
                }
            }
            "web_search_tool_result" -> {
                val toolUseId = block["tool_use_id"]?.jsonPrimitive?.contentOrNull
                val results = (block["content"] as? JsonArray)?.mapNotNull { el ->
                    val obj = el.asJsonObjectOrNull() ?: return@mapNotNull null
                    if (obj["type"]?.jsonPrimitive?.contentOrNull == "web_search_result") {
                        SearchResult.fromJson(obj)
                    } else null
                } ?: emptyList()
                val existingIndex = blockUi.entries.firstOrNull {
                    (it.value as? UiBlock.Search)?.toolUseId == toolUseId
                }?.key
                if (existingIndex != null) {
                    val cur = blockUi[existingIndex] as UiBlock.Search
                    blockUi[existingIndex] = cur.copy(results = results)
                    null // 已合并到 tool_use 的卡片，不再单独显示
                } else {
                    UiBlock.Search(toolUseId = toolUseId, results = results)
                }
            }
            "web_fetch_tool_result" -> {
                val toolUseId = block["tool_use_id"]?.jsonPrimitive?.contentOrNull
                val content = block["content"]?.asJsonObjectOrNull()
                val url = content?.get("url")?.jsonPrimitive?.contentOrNull
                val title = content?.get("content")?.asJsonObjectOrNull()
                    ?.get("title")?.jsonPrimitive?.contentOrNull
                UiBlock.ReadLink(toolUseId = toolUseId, url = url, title = title)
            }
            else -> null
        }
    }

    private fun appendRawText(index: Int, field: String, text: String) {
        val raw = blockRaw[index] ?: JsonObject(emptyMap())
        blockRaw[index] = JsonObject(raw + (field to JsonPrimitive(text)))
    }

    private fun publishBlocks(messageId: String) {
        val blocks = mergeTextBlocks(blockOrder.mapNotNull { blockUi[it] })
        updateAssistant(messageId) { it.copy(blocks = blocks) }
    }

    /** 把连续的 text 块合并成一段，避免正文被拆成碎片、引用重复。 */
    private fun mergeTextBlocks(blocks: List<UiBlock>): List<UiBlock> {
        val merged = mutableListOf<UiBlock>()
        blocks.forEach { b ->
            if (b is UiBlock.Text) {
                val last = merged.lastOrNull()
                if (last is UiBlock.Text) {
                    merged[merged.lastIndex] = last.copy(
                        text = last.text + b.text,
                        citations = last.citations + b.citations
                    )
                } else {
                    merged += b
                }
            } else {
                merged += b
            }
        }
        return merged
    }

    private fun updateAssistant(messageId: String, transform: (UiMessage) -> UiMessage) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.map { if (it.id == messageId) transform(it) else it }
        )
    }

    private fun finishStreaming(messageId: String, incomplete: Boolean) {
        val raw = buildJsonArray {
            blockOrder.forEach { idx ->
                blockRaw[idx]?.let { add(it) }
            }
        }
        updateAssistant(messageId) {
            val hasContent = it.blocks.any { b ->
                (b is UiBlock.Text && b.text.isNotBlank()) ||
                    b is UiBlock.Search || b is UiBlock.ReadLink ||
                    (b is UiBlock.Thinking && b.text.isNotBlank())
            }
            val error = when {
                incomplete -> "网络异常，请检查网络后重试"
                !hasContent && it.error == null && it.stopReason != "refusal" ->
                    "生成失败，请重试"
                else -> it.error
            }
            it.copy(
                streaming = false,
                rawContent = raw,
                stopReason = it.stopReason ?: if (incomplete) "incomplete" else "end_turn",
                error = error
            )
        }
        _state.value = _state.value.copy(streaming = false)
        persist()
    }

    private fun finishWithError(message: String, messageId: String) {
        updateAssistant(messageId) {
            it.copy(streaming = false, error = message)
        }
        _state.value = _state.value.copy(streaming = false, error = message)
        persist()
    }

    private fun resetAccumulator() {
        blockRaw.clear()
        blockUi.clear()
        blockOrder.clear()
        thinkingStartMs = 0L
        thinkingEndMs = 0L
    }

    // ---------- 持久化 ----------

    private fun persist() {
        val messages = _state.value.messages
        if (messages.isEmpty()) return
        val existingId = currentSessionId
        val id = existingId ?: UUID.randomUUID().toString().also { currentSessionId = it }
        val now = System.currentTimeMillis()
        val existing = _sessions.value.firstOrNull { it.id == id }
        val title = _state.value.title?.takeIf { it.isNotBlank() }
            ?: existing?.title
            ?: fallbackTitle(messages)

        val storedMessages = messages.mapNotNull { storedMessage(it) }
        if (storedMessages.isEmpty()) return

        val session = StoredSession(
            id = id,
            title = title,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            messages = storedMessages,
            summary = _state.value.summary,
            summaryCount = _state.value.summaryCount,
            pinned = existing?.pinned ?: false
        )
        val updated = (listOf(session) + _sessions.value.filterNot { it.id == id })
            .sortedWith(compareByDescending<StoredSession> { it.pinned }.thenByDescending { it.updatedAt })
        _sessions.value = updated
        viewModelScope.launch(Dispatchers.IO) { store.save(updated) }
    }

    private fun fallbackTitle(messages: List<UiMessage>): String {
        val firstUser = messages.firstOrNull { it.role == Roles.USER }?.text.orEmpty()
        val text = firstUser.ifBlank { "新对话" }.replace('\n', ' ').trim()
        return if (text.length > 20) text.take(20) + "…" else text
    }

    private fun storedMessage(message: UiMessage): StoredMessage? {
        return if (message.role == Roles.ASSISTANT) {
            if (message.rawContent == null) return null
            StoredMessage(
                role = message.role,
                text = message.blocks.filterIsInstance<UiBlock.Text>().joinToString("") { it.text },
                raw = message.rawContent.toString(),
                inputTokens = message.usage?.inputTokens ?: 0,
                outputTokens = message.usage?.outputTokens ?: 0,
                cacheReadTokens = message.usage?.cacheReadTokens ?: 0,
                thinkingTokens = message.usage?.thinkingTokens ?: 0
            )
        } else {
            StoredMessage(
                role = message.role,
                text = message.text,
                images = message.imagePaths,
                files = message.files
            )
        }
    }

    private fun storedToUi(message: StoredMessage): UiMessage {
        return if (message.role == Roles.ASSISTANT) {
            val raw = message.raw?.let {
                runCatching { json.parseToJsonElement(it) as JsonArray }.getOrNull()
            }
            val blocks = raw?.let { parseRawBlocks(it) } ?: emptyList()
            val usage = if (message.inputTokens != 0 || message.outputTokens != 0) {
                UsageInfo(
                    inputTokens = message.inputTokens,
                    outputTokens = message.outputTokens,
                    cacheReadTokens = message.cacheReadTokens,
                    thinkingTokens = message.thinkingTokens
                )
            } else null
            // 空内容的历史消息视为失败，显示重试入口
            val hasContent = blocks.any { b ->
                (b is UiBlock.Text && b.text.isNotBlank()) ||
                    b is UiBlock.Search || b is UiBlock.ReadLink ||
                    (b is UiBlock.Thinking && b.text.isNotBlank())
            }
            UiMessage(
                id = UUID.randomUUID().toString(),
                role = message.role,
                blocks = blocks,
                rawContent = raw,
                streaming = false,
                stopReason = "end_turn",
                usage = usage,
                error = if (!hasContent) "生成失败，请重试" else null
            )
        } else {
            // 兼容旧版单附件字段
            UiMessage(
                id = UUID.randomUUID().toString(),
                role = message.role,
                text = message.text,
                imagePaths = message.images + listOfNotNull(message.image),
                files = message.files + listOfNotNull(
                    message.fileId?.let {
                        StoredFile(it, message.fileName ?: "附件", message.fileSize)
                    }
                )
            )
        }
    }

    private fun UiMessage.toChatMessage(): ChatMessage {
        return if (role == Roles.ASSISTANT) {
            val content = rawContent ?: buildJsonArray {
                add(
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("text"),
                            "text" to JsonPrimitive(
                                blocks.filterIsInstance<UiBlock.Text>().joinToString("") { it.text }
                            )
                        )
                    )
                )
            }
            ChatMessage(role = Roles.ASSISTANT, content = content)
        } else {
            val validImages = imagePaths.mapNotNull { path ->
                val f = java.io.File(path)
                if (f.exists()) f else null
            }
            if (validImages.isNotEmpty() || files.isNotEmpty()) {
                ChatMessage(
                    role = Roles.USER,
                    content = buildJsonArray {
                        validImages.forEach { f ->
                            val base64 = android.util.Base64.encodeToString(
                                f.readBytes(),
                                android.util.Base64.NO_WRAP
                            )
                            add(
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("image"),
                                        "source" to JsonObject(
                                            mapOf(
                                                "type" to JsonPrimitive("base64"),
                                                "media_type" to JsonPrimitive("image/jpeg"),
                                                "data" to JsonPrimitive(base64)
                                            )
                                        )
                                    )
                                )
                            )
                        }
                        files.forEach { f ->
                            add(
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("document"),
                                        "source" to JsonObject(
                                            mapOf(
                                                "type" to JsonPrimitive("file"),
                                                "file_id" to JsonPrimitive(f.id)
                                            )
                                        )
                                    )
                                )
                            )
                        }
                        if (text.isNotBlank()) {
                            add(
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("text"),
                                        "text" to JsonPrimitive(text)
                                    )
                                )
                            )
                        }
                    }
                )
            } else {
                ChatMessage(role = Roles.USER, content = JsonPrimitive(text))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.close()
    }
}
