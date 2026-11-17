package com.ranaseek.app.data.repo

import com.ranaseek.app.data.api.DeepSeekSseClient
import com.ranaseek.app.data.api.SseEvent
import com.ranaseek.app.data.model.ChatMessage
import com.ranaseek.app.data.model.Citation
import com.ranaseek.app.data.model.SearchResult
import com.ranaseek.app.data.model.StreamError
import com.ranaseek.app.data.model.asJsonObjectOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 流式事件（已解析为强类型）。 */
sealed interface ChatStreamEvent {
    data class Started(val id: String?, val model: String?) : ChatStreamEvent
    data class BlockStarted(val index: Int, val block: JsonObject) : ChatStreamEvent
    data class TextDelta(val index: Int, val text: String) : ChatStreamEvent
    data class ThinkingDelta(val index: Int, val text: String) : ChatStreamEvent
    data class SignatureDelta(val index: Int, val signature: String) : ChatStreamEvent
    data class CitationDelta(val index: Int, val citation: Citation) : ChatStreamEvent
    data class BlockStopped(val index: Int) : ChatStreamEvent
    data class MessageDelta(
        val stopReason: String?,
        val title: String?,
        val usage: JsonObject?
    ) : ChatStreamEvent

    data object Stopped : ChatStreamEvent
    data class Failed(val error: StreamError) : ChatStreamEvent
}

/**
 * DeepSeek 全家桶 Messages API 客户端（流式）。
 * 端点：POST https://api.deepseek.com/anthropic/v1/messages
 */
class ChatRepository(
    private val sse: DeepSeekSseClient = DeepSeekSseClient()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun streamChat(
        apiKey: String,
        model: String,
        systemPrompt: String?,
        messages: List<ChatMessage>,
        deepThinking: Boolean,
        webSearch: Boolean,
        titleEnabled: Boolean,
        hasFileReference: Boolean = false,
        onEvent: suspend (ChatStreamEvent) -> Unit
    ): Int {
        val body = buildJsonObject {
            put("model", JsonPrimitive(model))
            if (!systemPrompt.isNullOrBlank()) put("system", JsonPrimitive(systemPrompt))
            put("stream", JsonPrimitive(true))
            put(
                "thinking",
                buildJsonObject { put("type", JsonPrimitive(if (deepThinking) "enabled" else "disabled")) }
            )
            if (webSearch) {
                put(
                    "tools",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("type", JsonPrimitive("deepseek_web_search"))
                                put("name", JsonPrimitive("deepseek_web_search"))
                            }
                        )
                        add(
                            buildJsonObject {
                                put("type", JsonPrimitive("deepseek_read_url"))
                                put("name", JsonPrimitive("deepseek_read_url"))
                            }
                        )
                    }
                )
            }
            put("safety_check", buildJsonObject { put("level", JsonPrimitive("high")) })
            if (titleEnabled) put("title", buildJsonObject { put("enabled", JsonPrimitive(true)) })
            put(
                "messages",
                buildJsonArray {
                    messages.forEach { m ->
                        add(
                            buildJsonObject {
                                put("role", JsonPrimitive(m.role))
                                put("content", m.content)
                            }
                        )
                    }
                }
            )
        }

        val requestBuilder = Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody(JSON_MEDIA))

        if (hasFileReference) {
            requestBuilder.header("anthropic-beta", "files-api-2025-04-14")
        }
        val request = requestBuilder.build()

        return sse.stream(request) { ev -> handleEvent(ev, onEvent) }
    }

    /** 语音识别：POST /v1/tools/asr（multipart 的 file 字段）。 */
    suspend fun transcribe(apiKey: String, file: java.io.File): Result<String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val body = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        okhttp3.RequestBody.create("audio/wav".toMediaType(), file)
                    )
                    .build()
                val req = Request.Builder()
                    .url("https://api.deepseek.com/v1/tools/asr")
                    .header("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()
                sse.httpClient().newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(httpErrorMessage(resp.code, text))
                    }
                    Json { ignoreUnknownKeys = true }
                        .parseToJsonElement(text).jsonObject["text"]
                        ?.jsonPrimitive?.contentOrNull ?: ""
                }
            }
        }

    /** 朗读：POST /v1/tools/tts，返回音频字节（wav）。 */
    suspend fun tts(apiKey: String, text: String): Result<ByteArray> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val body = buildJsonObject {
                    put("text", JsonPrimitive(text.take(10000)))
                    put("voice_id", JsonPrimitive("stella"))
                    put("format", JsonPrimitive("wav"))
                    put("filter_markdown", JsonPrimitive(true))
                }
                val req = Request.Builder()
                    .url("https://api.deepseek.com/v1/tools/tts")
                    .header("Authorization", "Bearer $apiKey")
                    .post(body.toString().toRequestBody(JSON_MEDIA))
                    .build()
                sse.httpClient().newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(httpErrorMessage(resp.code, resp.body?.string()))
                    }
                    resp.body?.bytes() ?: throw IllegalStateException("没有返回音频")
                }
            }
        }

    /** 上传文件到 /files（purpose 必须写在 file 之前）。 */
    suspend fun uploadFile(
        apiKey: String,
        displayName: String,
        mimeType: String?,
        bytes: ByteArray
    ): Result<UploadedFile> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val purpose = purposeFor(mimeType, displayName)
                val mediaType = (mimeType ?: "application/octet-stream").toMediaType()
                val body = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("purpose", purpose)
                    .addFormDataPart(
                        "file",
                        displayName,
                        bytes.toRequestBody(mediaType)
                    )
                    .build()
                val req = Request.Builder()
                    .url("https://api.deepseek.com/files")
                    .header("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()
                sse.httpClient().newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(httpErrorMessage(resp.code, text))
                    }
                    val obj = Json { ignoreUnknownKeys = true }
                        .parseToJsonElement(text).jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull
                        ?: throw IllegalStateException("上传成功但没有拿到文件 id")
                    UploadedFile(
                        id = id,
                        name = displayName,
                        purpose = purpose,
                        sizeBytes = bytes.size.toLong()
                    )
                }
            }
        }

    private fun purposeFor(mime: String?, name: String): String {
        val lower = name.lowercase()
        val docExtensions = listOf(".pdf", ".doc", ".docx", ".ppt", ".pptx")
        val isDocMime = mime?.startsWith("application/pdf") == true ||
            mime?.contains("word") == true ||
            mime?.contains("powerpoint") == true
        return if (isDocMime || docExtensions.any { lower.endsWith(it) }) {
            "text_recognition"
        } else {
            "user_data"
        }
    }

    fun close() = sse.close()

    /** 测试 API Key：调用 GET /models。 */
    suspend fun listModels(apiKey: String): Result<String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://api.deepseek.com/models")
                    .header("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                sse.httpClient().newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException(httpErrorMessage(resp.code, body))
                    }
                    val obj = Json { ignoreUnknownKeys = true }
                        .parseToJsonElement(body).jsonObject
                    val ids = (obj["data"] as? JsonArray)?.mapNotNull { el ->
                        el.asJsonObjectOrNull()?.get("id")?.jsonPrimitive?.contentOrNull
                    } ?: emptyList()
                    "连接成功，可用模型：" + ids.joinToString("、")
                }
            }
        }

    private suspend fun handleEvent(ev: SseEvent, onEvent: suspend (ChatStreamEvent) -> Unit) {
        val payload = ev.data.trim()
        if (payload.isEmpty()) return
        val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return
        val type = ev.event ?: obj["type"]?.jsonPrimitive?.contentOrNull

        when (type) {
            "message_start" -> {
                val m = obj["message"]?.asJsonObjectOrNull()
                onEvent(
                    ChatStreamEvent.Started(
                        id = m?.get("id")?.jsonPrimitive?.contentOrNull,
                        model = m?.get("model")?.jsonPrimitive?.contentOrNull
                    )
                )
            }

            "content_block_start" -> {
                val index = obj["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val block = obj["content_block"]?.asJsonObjectOrNull()
                if (block != null) onEvent(ChatStreamEvent.BlockStarted(index, block))
            }

            "content_block_delta" -> {
                val index = obj["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val delta = obj["delta"]?.asJsonObjectOrNull() ?: return
                when (delta["type"]?.jsonPrimitive?.contentOrNull) {
                    "text_delta" -> delta["text"]?.jsonPrimitive?.contentOrNull
                        ?.let { onEvent(ChatStreamEvent.TextDelta(index, it)) }

                    "thinking_delta" -> delta["thinking"]?.jsonPrimitive?.contentOrNull
                        ?.let { onEvent(ChatStreamEvent.ThinkingDelta(index, it)) }

                    "signature_delta" -> delta["signature"]?.jsonPrimitive?.contentOrNull
                        ?.let { onEvent(ChatStreamEvent.SignatureDelta(index, it)) }

                    "citations_delta" -> {
                        val c = delta["citation"]?.asJsonObjectOrNull() ?: return
                        Citation.fromJson(c)?.let { onEvent(ChatStreamEvent.CitationDelta(index, it)) }
                    }
                }
            }

            "content_block_stop" -> {
                val index = obj["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                onEvent(ChatStreamEvent.BlockStopped(index))
            }

            "message_delta" -> {
                val delta = obj["delta"]?.asJsonObjectOrNull()
                onEvent(
                    ChatStreamEvent.MessageDelta(
                        stopReason = delta?.get("stop_reason")?.jsonPrimitive?.contentOrNull,
                        title = delta?.get("title")?.jsonPrimitive?.contentOrNull,
                        usage = obj["usage"]?.asJsonObjectOrNull()
                    )
                )
            }

            "message_stop" -> onEvent(ChatStreamEvent.Stopped)

            "ping" -> Unit

            "error" -> {
                val err = obj["error"]?.asJsonObjectOrNull()
                onEvent(
                    ChatStreamEvent.Failed(
                        StreamError(
                            type = err?.get("type")?.jsonPrimitive?.contentOrNull,
                            message = err?.get("message")?.jsonPrimitive?.contentOrNull
                        )
                    )
                )
            }

            else -> {
                // 不带事件名的裸错误对象
                if (obj["error"] != null) {
                    val err = obj["error"]?.asJsonObjectOrNull()
                    onEvent(
                        ChatStreamEvent.Failed(
                            StreamError(
                                type = err?.get("type")?.jsonPrimitive?.contentOrNull,
                                message = err?.get("message")?.jsonPrimitive?.contentOrNull
                            )
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.deepseek.com/anthropic/v1/messages"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun httpErrorMessage(code: Int, body: String?): String {
            val serverMsg = runCatching {
                Json { ignoreUnknownKeys = true }
                    .parseToJsonElement(body ?: "")
                    .jsonObject["error"]?.asJsonObjectOrNull()?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            val prefix = when (code) {
                400 -> "请求参数有误"
                401 -> "API Key 无效或未授权"
                402 -> "账户余额不足"
                403 -> "没有权限"
                404 -> "接口不存在"
                413 -> "请求体过大"
                429 -> "请求太频繁，稍后再试"
                408, 504 -> "请求超时"
                500, 503 -> "服务暂时不可用"
                else -> "请求失败（HTTP $code）"
            }
            return if (serverMsg.isNullOrBlank()) prefix else "$prefix：$serverMsg"
        }
    }
}

fun JsonArray.isEmptyArray(): Boolean = size == 0
