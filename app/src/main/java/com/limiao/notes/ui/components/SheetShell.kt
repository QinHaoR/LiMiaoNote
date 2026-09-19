package com.limiao.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.ChartGrid
import com.limiao.notes.ui.Danger
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Typ

/**
 * 底部弹层统一使用的 sheet 状态：**打开就展开，不做「先半屏、再上滑」**。
 *
 * ⚠️ 所有 `ModalBottomSheet` 都必须传 `sheetState = rememberSheetStateExpanded()`。
 * 不传的话 Material3 默认从 `PartiallyExpanded` 起步 —— 也就是只展开半屏，
 * 用户还得再往上滑一下才能看到底部的保存按钮。这个坑踩过两次了，别再手写
 * `rememberModalBottomSheetState()`。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSheetStateExpanded(): SheetState =
    rememberModalBottomSheetState(skipPartiallyExpanded = true)

/**
 * 底部弹层内容的标准骨架：居中标题 + 右上关闭。
 *
 * 用法（注意 sheetState 那一行不能省）：
 * ```
 * ModalBottomSheet(
 *     onDismissRequest = ...,
 *     sheetState = rememberSheetStateExpanded(),
 *     dragHandle = null,
 * ) {
 *     SheetShell(title = "记饮食", onClose = ..., modifier = Modifier.fillMaxHeight(0.9f)) { ... }
 * }
 * ```
 * 内容高的弹层记得给 SheetShell 传 `Modifier.fillMaxHeight(0.9f)`，
 * 内部三段式（顶部 / 中间 weight(1f) 列表 / 底部常驻操作区）才立得住。
 */
@Composable
fun SheetShell(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = Typ.section, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.Close,
                "关闭",
                tint = TextTertiary,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(6.dp),
            )
        }
        content()
    }
}

/** 弹层里的一行操作（编辑 / 删除），[danger] = true 时用删除色 */
@Composable
fun SheetActionRow(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        color = if (danger) Danger else TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
    )
}

/** 弹层里的浅色分隔线 */
@Composable
fun SheetDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(ChartGrid),
    )
}

/** 弹层里的等宽两按钮行（取消 / 保存） */
@Composable
fun SheetButtonRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
