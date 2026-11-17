package com.ranaseek.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.toArgb
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
    data class Quote(val lines: List<String>) : MdSegment
    data class Latex(val code: String) : MdSegment
    data class LatexLine(val line: String) : MdSegment
    data object Divider : MdSegment
}

/** 极简 Markdown：代码块、标题、列表、粗体、行内代码、引用、分割线、LaTeX 公式。 */
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

                is MdSegment.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(
                                    // 用最小高度占位，竖线随内容由外层 Row 拉伸不便，这里用固定近似
                                    (seg.lines.size * 22).dp.coerceAtLeast(18.dp)
                                )
                                .background(p.divider, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            seg.lines.forEach { qLine ->
                                if (qLine.isBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                } else {
                                    Text(
                                        buildInline(qLine, p.textSecondary, codeBg),
                                        color = p.textSecondary,
                                        fontSize = fontSize,
                                        lineHeight = lineHeight
                                    )
                                }
                            }
                        }
                    }
                }

                is MdSegment.Latex -> {
                    LatexView(
                        text = seg.code,
                        displayMode = true,
                        textColor = textColor.toArgb(),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is MdSegment.LatexLine -> {
                    LatexView(
                        text = seg.line,
                        displayMode = false,
                        textColor = textColor.toArgb(),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                is MdSegment.Divider -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(1.dp)
                            .background(p.divider)
                    )
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
    var inQuote = false
    val quoteLines = mutableListOf<String>()
    var inLatex = false
    val latexCode = StringBuilder()

    fun flushPlain() {
        if (plain.isNotEmpty()) {
            segments += MdSegment.Plain(plain.toList())
            plain.clear()
        }
    }

    fun flushQuote() {
        if (quoteLines.isNotEmpty()) {
            segments += MdSegment.Quote(quoteLines.toList())
            quoteLines.clear()
        }
    }

    lines.forEach { line ->
        val trimmed = line.trimStart()
        val compact = trimmed.replace(" ", "")

        when {
            // 代码块围栏
            trimmed.startsWith("```") -> {
                if (inCode) {
                    segments += MdSegment.Code(code.toString().trimEnd('\n'), codeLang)
                    code.clear()
                    codeLang = ""
                    inCode = false
                } else {
                    flushPlain()
                    flushQuote()
                    inCode = true
                    codeLang = trimmed.removePrefix("```").trim()
                }
            }
            inCode -> code.appendLine(line)

            // 块级公式（$$...$$）
            inLatex -> {
                if (trimmed.endsWith("$$")) {
                    val content = trimmed.removeSuffix("$$").trim()
                    if (content.isNotBlank()) latexCode.appendLine(content)
                    segments += MdSegment.Latex(latexCode.toString().trim())
                    latexCode.clear()
                    inLatex = false
                } else {
                    latexCode.appendLine(line)
                }
            }
            trimmed.startsWith("$$") -> {
                flushPlain()
                flushQuote()
                val rest = trimmed.removePrefix("$$").trim()
                if (rest.endsWith("$$") && rest.length >= 2) {
                    // 单行 $$...$$
                    segments += MdSegment.Latex(rest.removeSuffix("$$").trim())
                } else {
                    inLatex = true
                    if (rest.isNotBlank()) latexCode.appendLine(rest)
                }
            }

            // 引用块（> 开头，连续多行合并）
            trimmed.startsWith(">") -> {
                flushPlain()
                inQuote = true
                quoteLines += trimmed.removePrefix(">").trimStart()
            }

            // 分割线（--- / *** / ___，允许空格）
            compact.length >= 3 &&
                (compact.all { it == '-' } || compact.all { it == '*' } || compact.all { it == '_' }) -> {
                flushPlain()
                flushQuote()
                segments += MdSegment.Divider
            }

            // 含行内公式 $...$ 的行
            Regex("\\$[^$\\n]+\\$").containsMatchIn(trimmed) -> {
                flushPlain()
                flushQuote()
                segments += MdSegment.LatexLine(trimmed)
            }

            else -> {
                inQuote = false
                flushQuote()
                plain += line
            }
        }
    }
    if (inCode && code.isNotEmpty()) {
        segments += MdSegment.Code(code.toString().trimEnd('\n'), codeLang)
    }
    if (inLatex && latexCode.isNotEmpty()) {
        segments += MdSegment.Latex(latexCode.toString().trim())
    }
    flushQuote()
    flushPlain()
    return segments
}

/** 行内解析：**粗体**、~~删除线~~、`代码`。 */
private fun buildInline(text: String, textColor: Color, codeBg: Color): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("(\\*\\*[^*]+\\*\\*|~~[^~]+~~|`[^`]+`)")
        var last = 0
        regex.findAll(text).forEach { m ->
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val token = m.value
            when {
                token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.removePrefix("**").removeSuffix("**"))
                }
                token.startsWith("~~") -> withStyle(
                    SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                ) {
                    append(token.removePrefix("~~").removeSuffix("~~"))
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
