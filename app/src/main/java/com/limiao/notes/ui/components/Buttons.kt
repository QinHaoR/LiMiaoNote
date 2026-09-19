package com.limiao.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.CardBg
import com.limiao.notes.ui.CardBorder
import com.limiao.notes.ui.ChartGrid
import com.limiao.notes.ui.OnAccent
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Dim

/**
 * 满宽药丸主按钮（参考图底部那条长按钮）。
 * 一屏只有它一个"主操作"时用它。
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val bg = if (enabled) Accent else ChartGrid
    val fg = if (enabled) OnAccent else TextTertiary
    Row(
        modifier
            .fillMaxWidth()
            .height(Dim.buttonH)
            .clip(RoundedCornerShape(Dim.radiusChip))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

/** 次要按钮：白底 + 描边（与主按钮并列时用） */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(Dim.buttonH)
            .clip(RoundedCornerShape(Dim.radiusChip))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(Dim.radiusChip))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) TextSecondary else TextTertiary,
        )
    }
}
