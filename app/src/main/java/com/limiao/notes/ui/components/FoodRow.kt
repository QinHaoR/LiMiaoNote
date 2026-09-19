package com.limiao.notes.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.Divider
import com.limiao.notes.ui.OnAccent
import com.limiao.notes.ui.Success
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary

/**
 * 食物库列表行（照参考图「记早餐」那句）：左侧名称 + 细节，右侧圆形按钮。
 *
 * 右侧按钮状态一眼可辨：未选 = **橙色实心 ＋**；已选 = **绿色实心 ✓**（名称也跟着变绿）。
 * 需要多选计数的场景用 [badge]。
 */
@Composable
fun FoodRow(
    name: String,
    detail: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    badge: Int = 0,
    showDivider: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 15.sp,
                    color = if (selected) Success else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    detail,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(10.dp))
            AddCircleButton(selected = selected, badge = badge)
        }
        if (showDivider) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp)
                    .height(1.dp)
                    .background(Divider),
            )
        }
    }
}

/**
 * 圆形状态按钮（列表右侧那个）。
 * [selected] = true → 绿底白 ✓；否则橙底白 ＋（[badge] > 0 时显示数字）。
 */
@Composable
fun AddCircleButton(selected: Boolean = false, badge: Int = 0, size: Dp = 28.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) Success else Accent),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                "已选",
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        } else {
            Text(
                if (badge > 0) "$badge" else "＋",
                fontSize = if (badge > 0) 13.sp else 15.sp,
                color = OnAccent,
            )
        }
    }
}

/** 等宽大图标入口（弹层里「识别图片或文 / 从食物库添加」那种并排两块） */
@Composable
fun RowScope.BigActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color,
    bg: Color,
) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary)
    }
}
