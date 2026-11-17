package com.ranaseek.app.ui.chat

import com.ranaseek.app.data.model.Citation
import com.ranaseek.app.data.model.SearchResult
import com.ranaseek.app.data.model.asJsonObjectOrNull
import com.ranaseek.app.data.model.queriesOf
import com.ranaseek.app.data.model.urlOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 把 assistant 的原始内容块数组解析成可渲染的 UI 块。
 * 用于：流式结束后、加载历史会话时。
 */
fun parseRawBlocks(raw: JsonArray): List<UiBlock> {
    val result = mutableListOf<UiBlock>()
    val toolUseIndex = mutableMapOf<String, Int>()

    raw.forEach { el ->
        val obj = el.asJsonObjectOrNull() ?: return@forEach
        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "thinking" -> {
                val text = obj["thinking"]?.jsonPrimitive?.contentOrNull ?: ""
                result += UiBlock.Thinking(text = text, done = true)
            }

            "text" -> {
                val text = obj["text"]?.jsonPrimitive?.contentOrNull ?: ""
                val citations = (obj["citations"] as? JsonArray)?.mapNotNull {
                    Citation.fromJson(it.asJsonObjectOrNull() ?: return@mapNotNull null)
                } ?: emptyList()
                val last = result.lastOrNull()
                if (last is UiBlock.Text) {
                    result[result.lastIndex] = last.copy(
                        text = last.text + text,
                        citations = last.citations + citations
                    )
                } else {
                    result += UiBlock.Text(text = text, citations = citations)
                }
            }

            "server_tool_use" -> {
                val name = obj["name"]?.jsonPrimitive?.contentOrNull
                val id = obj["id"]?.jsonPrimitive?.contentOrNull
                if (name == "deepseek_web_search") {
                    result += UiBlock.Search(
                        toolUseId = id,
                        queries = obj.queriesOf(),
                        kind = obj["kind"]?.jsonPrimitive?.contentOrNull
                    )
                } else {
                    result += UiBlock.ReadLink(toolUseId = id, url = obj.urlOf())
                }
                if (id != null) toolUseIndex[id] = result.lastIndex
            }

            "web_search_tool_result" -> {
                val toolUseId = obj["tool_use_id"]?.jsonPrimitive?.contentOrNull
                val results = (obj["content"] as? JsonArray)?.mapNotNull { e ->
                    val o = e.asJsonObjectOrNull() ?: return@mapNotNull null
                    if (o["type"]?.jsonPrimitive?.contentOrNull == "web_search_result") {
                        SearchResult.fromJson(o)
                    } else null
                } ?: emptyList()
                val idx = toolUseId?.let { toolUseIndex[it] }
                if (idx != null && result.getOrNull(idx) is UiBlock.Search) {
                    result[idx] = (result[idx] as UiBlock.Search).copy(results = results)
                } else {
                    result += UiBlock.Search(toolUseId = toolUseId, results = results)
                }
            }

            "web_fetch_tool_result" -> {
                val toolUseId = obj["tool_use_id"]?.jsonPrimitive?.contentOrNull
                val content = obj["content"]?.asJsonObjectOrNull()
                val url = content?.get("url")?.jsonPrimitive?.contentOrNull
                val title = content?.get("content")?.asJsonObjectOrNull()
                    ?.get("title")?.jsonPrimitive?.contentOrNull
                result += UiBlock.ReadLink(toolUseId = toolUseId, url = url, title = title)
            }
        }
    }
    return result
}
