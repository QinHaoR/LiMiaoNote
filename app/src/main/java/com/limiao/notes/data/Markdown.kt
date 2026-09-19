package com.limiao.notes.data

/**
 * 零依赖轻量 Markdown 解析器（阅读场景够用）
 *
 * 块级：# 标题、段落、``` 代码块、> 引用、- / 1. 列表、--- 分割线
 * 行内：**粗体**、*斜体*、`行内代码`、~~删除线~~、[文字](链接)
 *
 * 不支持（v1 刻意不做）：表格、嵌套列表、图片、HTML、转义。
 */

// ---------------- 行内 ----------------

sealed class MdInline {
    data class Text(val s: String) : MdInline()
    data class Bold(val children: List<MdInline>) : MdInline()
    data class Italic(val children: List<MdInline>) : MdInline()
    data class Code(val s: String) : MdInline()
    data class Strike(val children: List<MdInline>) : MdInline()
    data class Link(val text: String, val url: String) : MdInline()
}

// ---------------- 块级 ----------------

sealed class MdBlock {
    data class Heading(val level: Int, val inlines: List<MdInline>) : MdBlock()
    data class Paragraph(val inlines: List<MdInline>) : MdBlock()
    data class CodeBlock(val lang: String, val code: String) : MdBlock()
    data class Blockquote(val inlines: List<MdInline>) : MdBlock()
    data class ListBlock(val ordered: Boolean, val items: List<List<MdInline>>) : MdBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock()
    data object Divider : MdBlock()
}

object Markdown {

    fun parse(src: String): List<MdBlock> {
        val text = src.replace("\r\n", "\n").replace('\r', '\n')
        val lines = text.split("\n")
        val blocks = mutableListOf<MdBlock>()
        var i = 0
        val n = lines.size

        while (i < n) {
            val trimmed = lines[i].trim()
            if (trimmed.isEmpty()) { i++; continue }

            // 代码块
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val sb = StringBuilder()
                i++
                while (i < n && !lines[i].trimStart().startsWith("```")) {
                    sb.append(lines[i]).append("\n"); i++
                }
                i++ // 跳过结束 fence
                blocks.add(MdBlock.CodeBlock(lang, sb.toString().trimEnd('\n')))
                continue
            }

            // ATX 标题
            if (trimmed.startsWith("#")) {
                var level = 0
                while (level < trimmed.length && level < 6 && trimmed[level] == '#') level++
                val after = trimmed.getOrNull(level)
                if (level in 1..6 && (after == null || after == ' ' || after == '\t')) {
                    var text2 = trimmed.drop(level).trim()
                    text2 = text2.trimEnd('#').trim()
                    blocks.add(MdBlock.Heading(level, parseInline(text2)))
                    i++
                    continue
                }
                // "#tag" 这类不算标题，落到下方段落
            }

            // 分割线
            if (trimmed.matches(Regex("(-{3,}|\\*{3,}|_{3,})\\s*"))) {
                blocks.add(MdBlock.Divider)
                i++
                continue
            }

            // 引用（连续 > 行合并）
            if (trimmed.startsWith(">")) {
                val q = mutableListOf<String>()
                while (i < n) {
                    val t = lines[i].trim()
                    if (t.startsWith(">")) { q.add(t.drop(1).trim()); i++ } else break
                }
                blocks.add(MdBlock.Blockquote(parseInline(q.joinToString("\n"))))
                continue
            }

            // 表格：当前行含 |，且下一行是 |---| 分隔行
            if (trimmed.contains("|") && i + 1 < n && isTableSeparator(lines[i + 1])) {
                val headers = splitCells(trimmed)
                val rows = mutableListOf<List<String>>()
                i += 2
                while (i < n) {
                    val t = lines[i].trim()
                    if (t.isEmpty()) break
                    if (!t.contains("|")) break
                    rows.add(splitCells(t))
                    i++
                }
                blocks.add(MdBlock.Table(headers, rows))
                continue
            }

            // 列表
            val ul = Regex("^[-*+]\\s+(.*)$").matchEntire(trimmed)
            val ol = Regex("^\\d+\\.\\s+(.*)$").matchEntire(trimmed)
            if (ul != null || ol != null) {
                val ordered = ol != null
                val items = mutableListOf<List<MdInline>>()
                items.add(parseInline((ul ?: ol)!!.groupValues[1]))
                i++
                while (i < n) {
                    val t = lines[i].trim()
                    if (t.isEmpty()) { i++; continue } // 容忍列表中间空行
                    val u2 = Regex("^[-*+]\\s+(.*)$").matchEntire(t)
                    val o2 = Regex("^\\d+\\.\\s+(.*)$").matchEntire(t)
                    if ((!ordered && u2 != null) || (ordered && o2 != null)) {
                        items.add(parseInline((u2 ?: o2)!!.groupValues[1])); i++
                    } else break
                }
                blocks.add(MdBlock.ListBlock(ordered, items))
                continue
            }

            // 普通段落：收集连续的非特殊行
            val para = mutableListOf(trimmed)
            i++
            while (i < n) {
                val t = lines[i].trim()
                if (t.isEmpty()) break
                val isSpecial = t.startsWith("#") || t.startsWith(">") || t.startsWith("```") ||
                    Regex("^[-*+]\\s+").containsMatchIn(t) ||
                    Regex("^\\d+\\.\\s+").containsMatchIn(t) ||
                    t.matches(Regex("(-{3,}|\\*{3,}|_{3,})\\s*"))
                if (isSpecial) break
                para.add(t)
                i++
            }
            blocks.add(MdBlock.Paragraph(parseInline(para.joinToString("\n"))))
        }
        return blocks
    }

    // ---------------- 行内解析 ----------------

    /** 供 UI 渲染表格单元格等场景使用的公开入口 */
    fun inline(text: String): List<MdInline> = parseInline(text)

    private fun isTableSeparator(line: String): Boolean {
        val t = line.trim()
        return t.contains("---") && t.matches(Regex("^[|:\\-\\s]+$"))
    }

    private fun splitCells(line: String): List<String> {
        val parts = line.split("|").map { it.trim() }
        return parts.dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
    }

    private fun parseInline(s: String): List<MdInline> {
        val out = mutableListOf<MdInline>()
        val buf = StringBuilder()
        var i = 0
        val n = s.length

        fun flush() {
            if (buf.isNotEmpty()) { out.add(MdInline.Text(buf.toString())); buf.clear() }
        }

        while (i < n) {
            val c = s[i]
            when {
                c == '`' -> {
                    val end = s.indexOf('`', i + 1)
                    if (end > i) {
                        flush()
                        out.add(MdInline.Code(s.substring(i + 1, end)))
                        i = end + 1
                    } else { buf.append(c); i++ }
                }
                c == '*' -> {
                    val double = i + 1 < n && s[i + 1] == '*'
                    val close = if (double) s.indexOf("**", i + 2) else s.indexOf('*', i + 1)
                    if (close > i) {
                        flush()
                        val inner = s.substring(i + (if (double) 2 else 1), close)
                        if (double) out.add(MdInline.Bold(parseInline(inner)))
                        else out.add(MdInline.Italic(parseInline(inner)))
                        i = close + (if (double) 2 else 1)
                    } else { buf.append(c); i++ }
                }
                c == '~' && i + 1 < n && s[i + 1] == '~' -> {
                    val close = s.indexOf("~~", i + 2)
                    if (close > i) {
                        flush()
                        out.add(MdInline.Strike(parseInline(s.substring(i + 2, close))))
                        i = close + 2
                    } else { buf.append(c); i++ }
                }
                c == '[' -> {
                    val cb = s.indexOf(']', i + 1)
                    if (cb > i && cb + 1 < n && s[cb + 1] == '(') {
                        val cp = s.indexOf(')', cb + 2)
                        if (cp > cb) {
                            flush()
                            out.add(MdInline.Link(s.substring(i + 1, cb), s.substring(cb + 2, cp)))
                            i = cp + 1
                        } else { buf.append(c); i++ }
                    } else { buf.append(c); i++ }
                }
                else -> { buf.append(c); i++ }
            }
        }
        flush()
        return out
    }
}
