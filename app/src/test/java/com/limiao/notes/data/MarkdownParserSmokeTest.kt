package com.limiao.notes.data

/**
 * Markdown 解析器冒烟测试（纯 JVM，不依赖 Android 框架）
 *
 * 验证：表格、行内代码、粗体/斜体/链接、标题、引用、列表、分割线、代码块、段落
 * 跑法：在项目根执行
 *   env -u CODEBUDDY_SESSION_ID -u CLAUDE_SESSION_ID \
 *     "C:/Users/Public/jdk21/bin/java.exe" -classpath "gradle/wrapper/gradle-wrapper.jar" \
 *     org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest --tests "*MarkdownParserSmokeTest"
 */
fun main() {
    var failed = 0
    fun assert(name: String, cond: Boolean, detail: String = "") {
        val tag = if (cond) "PASS" else "FAIL"
        if (!cond) failed++
        println("[$tag] $name${if (detail.isNotEmpty()) "  ($detail)" else ""}")
    }

    // 1) 标题
    val h1 = Markdown.parse("# 标题").single() as MdBlock.Heading
    assert("heading h1", h1.level == 1 && (h1.inlines.single() as MdInline.Text).s == "标题")

    val h3 = Markdown.parse("### 三级").single() as MdBlock.Heading
    assert("heading h3", h3.level == 3)

    // 2) 段落
    val p = Markdown.parse("普通段落").single() as MdBlock.Paragraph
    assert("paragraph", (p.inlines.single() as MdInline.Text).s == "普通段落")

    // 3) 粗体 + 斜体 + 行内代码 + 链接
    val inlines = Markdown.inline("**粗** *斜* `code` [go](https://x.com) ~~del~~ 文本")
    val types = inlines.map { it::class.simpleName }
    val hasBold = inlines.any { it is MdInline.Bold }
    val hasItalic = inlines.any { it is MdInline.Italic }
    val hasCode = inlines.any { it is MdInline.Code && it.s == "code" }
    val hasLink = inlines.any { it is MdInline.Link && it.text == "go" && it.url == "https://x.com" }
    val hasStrike = inlines.any { it is MdInline.Strike }
    assert("inline bold", hasBold, "types=$types")
    assert("inline italic", hasItalic, "types=$types")
    assert("inline code", hasCode, "types=$types")
    assert("inline link", hasLink, "types=$types")
    assert("inline strike", hasStrike, "types=$types")

    // 4) 列表（无序 + 有序）
    val ul = Markdown.parse("- a\n- b\n- c").single() as MdBlock.ListBlock
    assert("unordered list", !ul.ordered && ul.items.size == 3)

    val ol = Markdown.parse("1. a\n2. b").single() as MdBlock.ListBlock
    assert("ordered list", ol.ordered && ol.items.size == 2)

    // 5) 引用
    val q = Markdown.parse("> 引用行").single() as MdBlock.Blockquote
    assert("blockquote", (q.inlines.single() as MdInline.Text).s == "引用行")

    // 6) 代码块
    val cb = Markdown.parse("```kotlin\nfun main() = 0\n```").single() as MdBlock.CodeBlock
    assert("codeblock", cb.lang == "kotlin" && cb.code.contains("fun main"))

    // 7) 分割线
    val blocks = Markdown.parse("上\n\n---\n\n下")
    assert("divider", blocks.size == 3 && blocks[1] is MdBlock.Divider)

    // 8) 表格（重点新增）
    val md = """
| 名称 | 状态 | 备注 |
| ---- | :---: | ---: |
| 表格 | ✅ 支持 | 3 列 |
| 行内代码 | `ok()` | 有底色 |
| 链接 | [点我](https://g) | 可点击 |
""".trim()
    val tbl = Markdown.parse(md).single() as MdBlock.Table
    assert("table headers", tbl.headers == listOf("名称", "状态", "备注"), "got=${tbl.headers}")
    assert("table rows count", tbl.rows.size == 3, "got=${tbl.rows.size}")
    assert("table row 0", tbl.rows[0] == listOf("表格", "✅ 支持", "3 列"), "got=${tbl.rows[0]}")
    assert(
        "table cell has inline code",
        tbl.rows[1][1] == "`ok()`",
        "got='${tbl.rows[1][1]}'",
    )
    assert(
        "table cell has link inline",
        (Markdown.inline(tbl.rows[2][1]).any { it is MdInline.Link && it.url == "https://g" }),
    )

    // 9) 表格分隔行（---）不能被当 Divider 误判
    val afterTbl = Markdown.parse("# 标题\n\n| a | b |\n| - | - |\n| 1 | 2 |\n\n结尾")
    val hasTbl = afterTbl.any { it is MdBlock.Table }
    val hasEndPara = (afterTbl.lastOrNull() as? MdBlock.Paragraph)?.inlines?.firstOrNull() is MdInline.Text
    assert("table with surrounding blocks", hasTbl && hasEndPara, "blocks=$afterTbl")

    println()
    if (failed == 0) {
        println("ALL ${10 + 4} CHECKS PASSED")
    } else {
        println("$failed CHECK(S) FAILED")
        kotlin.system.exitProcess(1)
    }
}
