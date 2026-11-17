package com.ranaseek.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** 一条 SSE 事件（event 名可为 null，data 为原始文本）。 */
data class SseEvent(val event: String?, val data: String)

/**
 * DeepSeek Messages API 的 SSE 流读取。
 * 文档要点：以 message_stop 判定完整；: keep-alive 为注释心跳；data 可能是不带事件名的裸错误对象。
 */
class DeepSeekSseClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * 发起流式请求并逐条回调事件。
     * @return HTTP 状态码（失败时也返回，便于区分鉴权/限流等错误）
     */
    suspend fun stream(request: Request, onEvent: suspend (SseEvent) -> Unit): Int =
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val body = response.body ?: return@use response.code
                val source = body.source()
                var eventName: String? = null
                val data = StringBuilder()

                suspend fun dispatch() {
                    val payload = data.toString()
                    if (payload.isNotEmpty() && eventName != null || payload.startsWith("{")) {
                        onEvent(SseEvent(eventName, payload))
                    }
                    eventName = null
                    data.clear()
                }

                while (true) {
                    val line = source.readUtf8Line() ?: break
                    when {
                        line.isEmpty() -> dispatch()
                        line.startsWith(":") -> Unit // keep-alive 注释
                        line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> {
                            val chunk = line.removePrefix("data:").trim()
                            if (data.isNotEmpty()) data.append('\n')
                            data.append(chunk)
                        }
                        else -> Unit
                    }
                }
                if (data.isNotEmpty()) dispatch()
                response.code
            }
        }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    fun httpClient(): OkHttpClient = client
}
