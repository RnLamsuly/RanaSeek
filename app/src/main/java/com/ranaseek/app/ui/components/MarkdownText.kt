package com.ranaseek.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.ui.theme.RsTheme

private sealed interface MdSegment {
    data class Code(val code: String, val lang: String) : MdSegment
    data class Plain(val lines: List<String>) : MdSegment
}

/** 极简 Markdown：代码块、标题、列表、粗体、行内代码。够对话用，不引入重量级依赖。 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 26.sp,
    acrylic: Boolean = false,
    acrylicTint: Int = 38
) {
    val p = RsTheme
    val textColor = color ?: p.textPrimary
    val segments = remember(text) { splitSegments(text) }
    val codeBg = if (acrylic) {
        if (p.isDark) Color(0xFF0C0F18).copy(alpha = (acrylicTint / 100f).coerceIn(0.1f, 0.92f))
        else Color.White.copy(alpha = (acrylicTint / 100f + 0.22f).coerceIn(0.2f, 0.92f))
    } else {
        p.surface
    }

    Column(modifier = modifier) {
        segments.forEach { seg ->
            when (seg) {
                is MdSegment.Code -> {
                    val lang = seg.lang
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(
                                if (acrylic) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = if (p.isDark) Color.White.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                } else Modifier
                            )
                            .background(codeBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (lang.isNotBlank()) {
                            Text(
                                lang,
                                color = p.textSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            seg.code,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                is MdSegment.Plain -> {
                    seg.lines.forEach { raw ->
                        val line = raw.trimEnd()
                        when {
                            line.isBlank() -> Spacer(Modifier.height(8.dp))
                            line.startsWith("### ") -> Text(
                                buildInline(line.removePrefix("### "), textColor, codeBg),
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 24.sp
                            )
                            line.startsWith("## ") -> Text(
                                buildInline(line.removePrefix("## "), textColor, codeBg),
                                color = textColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 25.sp
                            )
                            line.startsWith("# ") -> Text(
                                buildInline(line.removePrefix("# "), textColor, codeBg),
                                color = textColor,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 27.sp
                            )
                            line.startsWith("- ") || line.startsWith("* ") -> Row(
                                modifier = Modifier.padding(vertical = 1.dp)
                            ) {
                                Text("•  ", color = textColor, fontSize = fontSize, lineHeight = lineHeight)
                                Text(
                                    buildInline(line.substring(2), textColor, codeBg),
                                    color = textColor,
                                    fontSize = fontSize,
                                    lineHeight = lineHeight
                                )
                            }
                            else -> Text(
                                buildInline(line, textColor, codeBg),
                                color = textColor,
                                fontSize = fontSize,
                                lineHeight = lineHeight
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun splitSegments(text: String): List<MdSegment> {
    val segments = mutableListOf<MdSegment>()
    val lines = text.split('\n')
    val plain = mutableListOf<String>()
    var inCode = false
    var codeLang = ""
    val code = StringBuilder()

    fun flushPlain() {
        if (plain.isNotEmpty()) {
            segments += MdSegment.Plain(plain.toList())
            plain.clear()
        }
    }

    lines.forEach { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                // 结束代码块
                segments += MdSegment.Code(code.toString().trimEnd('\n'), codeLang)
                code.clear()
                codeLang = ""
                inCode = false
            } else {
                flushPlain()
                inCode = true
                codeLang = trimmed.removePrefix("```").trim()
            }
        } else if (inCode) {
            code.appendLine(line)
        } else {
            plain += line
        }
    }
    if (inCode && code.isNotEmpty()) {
        segments += MdSegment.Code(code.toString().trimEnd('\n'), codeLang)
    }
    flushPlain()
    return segments
}

/** 行内解析：**粗体**、`代码`。 */
private fun buildInline(text: String, textColor: Color, codeBg: Color): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`)")
        var last = 0
        regex.findAll(text).forEach { m ->
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val token = m.value
            when {
                token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.removePrefix("**").removeSuffix("**"))
                }
                token.startsWith("`") -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg,
                        fontSize = 14.sp
                    )
                ) {
                    append(token.removePrefix("`").removeSuffix("`"))
                }
            }
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}
