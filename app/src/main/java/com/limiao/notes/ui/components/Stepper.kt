package com.limiao.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary

/**
 * 「− 值 ＋」步进行。身高 / 年龄这类整数调节用它；
 * 体重这种需要精确输入的场景用 [InputNumberDialog]。
 */
@Composable
fun StepperRow(
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
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        StepButton("－", onMinus)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
        StepButton("＋", onPlus)
    }
}

/** 圆角方块步进按钮 */
@Composable
fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 15.sp, color = TextSecondary)
    }
}

/** 数字输入弹层（体重、身高等需要打字的场景）。只允许数字和小数点，最多 8 位。 */
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
                suffix = { Text(suffix, color = TextTertiary) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (parsed != null) onConfirm(parsed) },
                enabled = parsed != null && parsed > 0,
            ) { Text("保存", color = Accent, fontWeight = FontWeight.Medium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextTertiary) }
        },
    )
}
