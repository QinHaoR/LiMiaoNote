package com.limiao.notes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Typ

/**
 * 大写小标签（`体重` / `SEP 20` 这种）。
 *
 * 内部统一 uppercase + 正字距，调用方只传文案，标签风格永不漂移。
 * 中文字符 uppercase 后不变，所以中英混排都安全。
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = Typ.label,
        color = TextTertiary,
        modifier = modifier,
    )
}

/** 卡片标题行：左边标题，右边可选「更多 ›」 */
@Composable
fun CardTitleRow(
    title: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Typ.section, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (onMore != null) {
            Text(
                "更多 ›",
                fontSize = 12.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable(onClick = onMore).padding(start = 8.dp),
            )
        }
    }
}

/** 右侧「›」迷你提示文字（用于整卡可点时的引导） */
@Composable
fun MoreHint(text: String = "查看趋势 ›", modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, color = TextTertiary, modifier = modifier)
}
