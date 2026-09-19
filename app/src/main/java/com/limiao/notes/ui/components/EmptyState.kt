package com.limiao.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.AccentSoft
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Dim

/**
 * 空状态 / 未开放页面占位。
 * 大圆角图标块（强调色浅底）+ 标题 + 说明，可选一个操作按钮。
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            desc,
            fontSize = 13.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PillButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 卡片内的空状态（比整页空状态轻，用于"这一块还没数据"） */
@Composable
fun InlineEmpty(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        color = TextTertiary,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dim.gapItem),
    )
}
