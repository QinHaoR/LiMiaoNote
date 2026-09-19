package com.limiao.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.limiao.notes.ui.CardBg
import com.limiao.notes.ui.CardBorder
import com.limiao.notes.ui.DarkBanner
import com.limiao.notes.ui.theme.Dim

/**
 * 卡片容器 —— **全站唯一的卡片外观**。
 *
 * 白底 + 大圆角 + 1dp 描边 + 无阴影（不用 M3 的 Card，它的 elevation 会破坏扁平观感）。
 * 页面里不要再手写 `Card(colors = ...)`，否则圆角/描边会各处漂移。
 */
@Composable
fun SkinCard(
    modifier: Modifier = Modifier,
    padding: Dp = Dim.cardPad,
    bordered: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dim.radiusCard)
    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(
        m
            .clip(shape)
            .background(CardBg)
            .then(if (bordered) Modifier.border(1.dp, CardBorder, shape) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** 深色横幅卡片（本周小结那种）。黑白极简配色下最强的视觉锚点。 */
@Composable
fun BannerCard(
    modifier: Modifier = Modifier,
    padding: Dp = Dim.cardPad,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dim.radiusCard)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkBanner)
            .padding(padding),
        content = content,
    )
}
