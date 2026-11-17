package com.ranaseek.app.data.session

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

@Serializable
data class StoredFile(
    val id: String,
    val name: String,
    val size: Long = 0
)

@Serializable
data class StoredMessage(
    val role: String,
    val text: String = "",
    /** assistant 的原始内容块 JSON 数组（字符串），回传时原样使用 */
    val raw: String? = null,
    // 旧版单附件字段（保留用于兼容读取）
    val image: String? = null,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0,
    // 多附件
    val images: List<String> = emptyList(),
    val files: List<StoredFile> = emptyList(),
    /** 这一轮消耗的 token */
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cacheReadTokens: Int = 0,
    val thinkingTokens: Int = 0
)

@Serializable
data class StoredSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<StoredMessage> = emptyList(),
    /** 压缩后的上下文摘要（发请求时替代前 summaryCount 条历史，UI 不受影响） */
    val summary: String? = null,
    val summaryCount: Int = 0,
    /** 置顶 */
    val pinned: Boolean = false
)

/** 会话本地存储（app 私有目录，纯文件 JSON）。 */
class SessionStore(context: Context) {

    private val file = File(context.filesDir, "sessions.json")
    private val backupFile = File(context.filesDir, "sessions.backup.json")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 上次加载是否解析失败（失败后禁止用空列表覆盖原文件） */
    private var poisoned = false

    fun load(): List<StoredSession> {
        if (!file.exists()) return emptyList()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptyList()

        // 先尝试整体解析
        runCatching { json.decodeFromString<List<StoredSession>>(text) }
            .onSuccess {
                poisoned = false
                return it
            }

        // 整体失败：备份损坏文件，再逐条容错解析（坏会话跳过，好的保留）
        runCatching { file.copyTo(backupFile, overwrite = true) }
        poisoned = true
        return runCatching {
            val arr = json.parseToJsonElement(text) as JsonArray
            arr.mapNotNull { el ->
                runCatching { json.decodeFromJsonElement<StoredSession>(el) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun save(sessions: List<StoredSession>) {
        // 保护：上次解析失败且当前列表为空时，绝不覆盖原文件（避免把损坏但可恢复的数据顶掉）
        if (poisoned && sessions.isEmpty()) return
        runCatching {
            val tmp = File(file.parentFile, "sessions.json.tmp")
            tmp.writeText(json.encodeToString(sessions))
            if (file.exists()) {
                // 覆盖前留一份备份
                runCatching { file.copyTo(backupFile, overwrite = true) }
                file.delete()
            }
            tmp.renameTo(file)
        }
    }

    fun delete(id: String): List<StoredSession> {
        val updated = load().filterNot { it.id == id }
        save(updated)
        return updated
    }
}
