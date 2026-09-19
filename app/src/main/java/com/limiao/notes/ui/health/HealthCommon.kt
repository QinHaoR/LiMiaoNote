package com.limiao.notes.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.Ink
import com.limiao.notes.ui.InkSoft
import com.limiao.notes.ui.Line
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.Primary
import com.limiao.notes.ui.Surface

// ==================== 健康模块公用组件 ====================

/** 白色卡片容器（沿用全站规范：白底、圆角 16、无阴影） */
@Composable
fun HCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** 卡片小标题 */
@Composable
fun HLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
}

/** 大号数字（衬线感靠粗体字号体现，与全站一致） */
@Composable
fun HNumber(text: String, size: Int = 40, color: Color = Ink, unit: String? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text, fontSize = size.sp, fontWeight = FontWeight.Medium, color = color)
        if (unit != null) {
            Text(
                " $unit",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = (size / 6).dp),
            )
        }
    }
}

/** 细进度条（0..1） */
@Composable
fun HBar(progress: Float, color: Color = Primary, height: Dp = 6.dp) {
    val p = progress.coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(Line),
    ) {
        if (p > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(p)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height))
                    .background(color),
            )
        }
    }
}

/** 可选中的小胶囊 */
@Composable
fun RowScope.HChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Primary else Surface
    val fg = if (selected) Color.White else InkSoft
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = fg)
    }
}

/** 二级页顶部栏：返回箭头 + 标题 */
@Composable
fun HealthTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkSoft)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

/** 「标签 —— 值」一行 */
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
            color = if (onDark) Color.White else Ink,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = if (onDark) Color.White.copy(alpha = 0.75f) else Muted)
    }
}

/** 宫格入口图标（微信小程序风格：圆角方块 + 名称） */
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, name, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 12.sp, color = InkSoft, maxLines = 1)
        if (badge != null) {
            Text(badge, fontSize = 10.sp, color = Muted, maxLines = 1)
        }
    }
}

/** 列表行：标题 + 值（只读展示用） */
@Composable
fun ReportRow(label: String, value: String, accent: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (accent) FontWeight.Medium else FontWeight.Normal,
            color = if (accent) Primary else Ink,
        )
    }
}

/**
 * 「− 值 ＋」步进行。用于身高/年龄这类整数调节；体重这种需要精确输入的场景用 [InputNumberDialog]。
 */
@Composable
fun StepRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
        Text(
            "－",
            fontSize = 17.sp,
            color = Muted,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onMinus)
                .padding(horizontal = 13.dp, vertical = 4.dp),
        )
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
        )
        Text(
            "＋",
            fontSize = 17.sp,
            color = Muted,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onPlus)
                .padding(horizontal = 13.dp, vertical = 4.dp),
        )
    }
}

/** 数字输入弹层（体重、身高等需要打字的场景） */
@Composable
fun InputNumberDialog(
    title: String,
    initial: String,
    suffix: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val parsed = text.trim().toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { s ->
                    if (s.length <= 8 && s.all { it.isDigit() || it == '.' }) text = s
                },
                singleLine = true,
                suffix = { Text(suffix, color = Muted) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (parsed != null) onConfirm(parsed) },
                enabled = parsed != null && parsed > 0,
            ) { Text("保存", color = Primary, fontWeight = FontWeight.Medium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
        },
    )
}

/** 开发中占位页（⑤ 报告 / ⑥ AI 助手 本期只搭骨架） */
@Composable
fun HealthPlaceholder(
    icon: ImageVector,
    title: String,
    desc: String,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        HealthTopBar(title, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                desc,
                fontSize = 13.sp,
                color = Muted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}
