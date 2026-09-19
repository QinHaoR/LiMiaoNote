package com.limiao.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.CardBg
import com.limiao.notes.ui.CardBorder
import com.limiao.notes.ui.Divider
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Dim

/** 统计小格（等宽，居中，上数值下标签） */
@Composable
fun RowScope.StatCell(value: String, label: String, onDark: Boolean = false) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = if (onDark) Color.White else TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (onDark) Color.White.copy(alpha = 0.7f) else TextTertiary,
        )
    }
}

/**
 * 宫格入口（微信小程序那种：圆角方块图标 + 名称）。
 * [width] 由调用方用 `BoxWithConstraints` 的 maxWidth / 4 算出来，保证 1/4 等宽。
 */
@Composable
fun EntryTile(
    name: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    width: Dp,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(Dim.radiusInner))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Dim.tile)
                .clip(RoundedCornerShape(Dim.radiusTile))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, name, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
        if (badge != null) {
            Text(badge, fontSize = 10.sp, color = TextTertiary, maxLines = 1)
        }
    }
}

/** 列表行：图标 + 标题 + 副标题 + 右箭头（「我的」页那种入口） */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = TextSecondary,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dim.cardPad, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, title, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            Text(trailing, fontSize = 13.sp, color = TextTertiary)
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 「标签 —— 值」一行（只读数据行） */
@Composable
fun ValueRow(label: String, value: String, accent: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (accent) FontWeight.Medium else FontWeight.Normal,
            color = if (accent) Accent else TextPrimary,
        )
    }
}

/** 细线分隔（卡片内部） */
@Composable
fun ThinDivider(modifier: Modifier = Modifier, color: Color = Divider) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/** 数据小格（小米体脂秤那种网格：上小标签，下大数字，带 1dp 描边） */
@Composable
fun DataCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Dim.radiusInner))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(Dim.radiusInner))
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(label, fontSize = 11.sp, color = TextTertiary)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
