package com.limiao.notes.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.MdBlock
import com.limiao.notes.data.MdInline
import com.limiao.notes.data.Markdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 外部 .md 文件打开请求 */
data class MdOpen(val uriString: String, val name: String)

private val CodeBg = Color(0xFFF4F4F5)
private val QuoteBg = Primary.copy(alpha = 0.05f)

@Composable
fun MdReaderScreen(open: MdOpen, onClose: () -> Unit) {
    BackHandler { onClose() }
    val context = LocalContext.current
    var content by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(open.uriString, attempt) {
        content = null
        error = null
        val res = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(open.uriString)
                val ins = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("无法打开文件，可能已被移动或删除")
                ins.use { s ->
                    val raw = s.readBytes().toString(Charsets.UTF_8)
                    if (raw.startsWith("\uFEFF")) raw.substring(1) else raw  // 去 BOM
                }
            }
        }
        res.onSuccess { content = it }.onFailure { error = it.message ?: "读取失败" }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        // 顶部条：文件名 + 完成
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .statusBarsPadding()
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                open.name,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onClose) { Text("完成", color = Primary) }
        }

        when {
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(80.dp))
                    Text("打开失败", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { attempt++ }) { Text("重试", color = Primary) }
                }
            }
            content == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                }
            }
            else -> {
                val blocks = remember(content) { Markdown.parse(content!!) }
                if (blocks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("（空文件）", color = Muted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 18.dp, vertical = 16.dp,
                        ),
                    ) {
                        blocks.forEachIndexed { idx, b ->
                            item(key = idx) { MdBlockView(b) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MdBlockView(b: MdBlock) {
    when (b) {
        is MdBlock.Heading -> {
            val size = when (b.level) {
                1 -> 24.sp; 2 -> 21.sp; 3 -> 18.sp; else -> 16.sp
            }
            val pad = if (b.level <= 2) 10.dp else 6.dp
            Text(
                inlineAnnotated(b.inlines),
                fontSize = size, lineHeight = size * 1.3f,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.padding(top = pad, bottom = 4.dp),
            )
        }
        is MdBlock.Paragraph -> Text(
            inlineAnnotated(b.inlines),
            fontSize = 15.sp, lineHeight = 24.sp, color = Ink,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        is MdBlock.CodeBlock -> {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(CodeBg, RoundedCornerShape(10.dp)),
            ) {
                Column(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    val lines = b.code.split("\n")
                    lines.forEach { line ->
                        Text(
                            if (line.isEmpty()) " " else line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp, lineHeight = 20.sp,
                            color = InkSoft,
                        )
                    }
                }
            }
        }
        is MdBlock.Blockquote -> {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(QuoteBg, RoundedCornerShape(4.dp))
                    .height(IntrinsicSize.Min),
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(Primary.copy(alpha = 0.55f)),
                )
                Text(
                    inlineAnnotated(b.inlines),
                    fontSize = 14.sp, lineHeight = 22.sp, color = InkSoft,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        is MdBlock.ListBlock -> {
            Column(Modifier.padding(vertical = 4.dp)) {
                b.items.forEachIndexed { idx, item ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(
                            if (b.ordered) "${idx + 1}." else "•",
                            color = Primary, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            inlineAnnotated(item),
                            fontSize = 15.sp, lineHeight = 23.sp, color = Ink,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        is MdBlock.Divider -> HorizontalDivider(
            Modifier.padding(vertical = 10.dp),
            color = Line.copy(alpha = 0.8f),
        )
    }
}

/** 行内标记 → AnnotatedString（粗体/斜体/删除线/行内代码/链接） */
private fun inlineAnnotated(list: List<MdInline>): AnnotatedString = buildAnnotatedString {
    appendInline(list, bold = false, italic = false, strike = false)
}

private fun AnnotatedString.Builder.appendInline(
    items: List<MdInline>,
    bold: Boolean,
    italic: Boolean,
    strike: Boolean,
) {
    items.forEach { sp ->
        when (sp) {
            is MdInline.Text -> {
                pushStyle(
                    SpanStyle(
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (strike) TextDecoration.LineThrough else null,
                    )
                )
                append(sp.s)
                pop()
            }
            is MdInline.Bold -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInline(sp.children, false, italic, strike)
                pop()
            }
            is MdInline.Italic -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInline(sp.children, bold, false, strike)
                pop()
            }
            is MdInline.Strike -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendInline(sp.children, bold, italic, false)
                pop()
            }
            is MdInline.Code -> {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = InkSoft))
                append(sp.s)
                pop()
            }
            is MdInline.Link -> {
                pushStyle(SpanStyle(color = Primary, textDecoration = TextDecoration.Underline))
                append(sp.text)
                pop()
            }
        }
    }
}
