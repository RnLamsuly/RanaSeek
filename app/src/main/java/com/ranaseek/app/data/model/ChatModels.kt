package com.ranaseek.app.data.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** 消息角色。 */
object Roles {
    const val USER = "user"
    const val ASSISTANT = "assistant"
}

/** 一条对话消息：content 对 user 是字符串，对 assistant 是内容块数组（原样回传）。 */
data class ChatMessage(
    val role: String,
    val content: JsonElement
)

data class Citation(
    val url: String,
    val title: String?
) {
    companion object {
        fun fromJson(el: JsonObject): Citation? {
            val url = el["url"]?.jsonPrimitive?.contentOrNull ?: return null
            return Citation(url = url, title = el["title"]?.jsonPrimitive?.contentOrNull)
        }
    }
}

data class SearchResult(
    val title: String?,
    val url: String?,
    val siteName: String?,
    val snippet: String?,
    val siteIcon: String?,
    val pageAge: String?
) {
    companion object {
        fun fromJson(el: JsonObject): SearchResult = SearchResult(
            title = el["title"]?.jsonPrimitive?.contentOrNull,
            url = el["url"]?.jsonPrimitive?.contentOrNull,
            siteName = el["site_name"]?.jsonPrimitive?.contentOrNull,
            snippet = el["snippet"]?.jsonPrimitive?.contentOrNull,
            siteIcon = el["site_icon"]?.jsonPrimitive?.contentOrNull,
            pageAge = el["page_age"]?.jsonPrimitive?.contentOrNull
        )
    }
}

/** 助手回复的状态。 */
enum class AssistantStatus { STREAMING, DONE, ERROR }

/** 错误信息。 */
data class StreamError(val type: String?, val message: String?)

/** tool_use 块里提取出的搜索词。 */
fun JsonObject.queriesOf(): List<String> {
    val input = this["input"]?.asJsonObjectOrNull() ?: return emptyList()
    val arr = input["queries"] as? JsonArray ?: return emptyList()
    return arr.mapNotNull { it.jsonPrimitive.contentOrNull }
}

/** tool_use 块里提取出的 URL。 */
fun JsonObject.urlOf(): String? {
    val input = this["input"]?.asJsonObjectOrNull() ?: return null
    return input["url"]?.jsonPrimitive?.contentOrNull
}

fun JsonElement.asJsonObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

fun JsonElement.asLongOrNull(): Long? = runCatching { jsonPrimitive.longOrNull }.getOrNull()
