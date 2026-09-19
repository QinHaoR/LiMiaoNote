package com.limiao.notes.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Typ

/**
 * 大数字 + 可选单位。单位用小号字底部对齐，形成「50.0 kg」这种基线关系。
 *
 * 字号走 [style]（Typ.hero / Typ.big / Typ.mid），不要在这里传裸 sp。
 */
@Composable
fun BigNumber(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: TextStyle = Typ.big,
    color: Color = TextPrimary,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = style, color = color)
        if (unit != null) {
            Text(
                " $unit",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/** 左标签 + 右数值一组（卡片里常见的「距目标还差 / 2.7 kg」两行式） */
@Composable
fun NumPair(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueColor: Color = TextPrimary,
    style: TextStyle = Typ.mid,
) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = TextTertiary)
        Spacer(Modifier.height(4.dp))
        BigNumber(value, unit = unit, style = style, color = valueColor)
    }
}

/** 一行三段的大号数值（深色横幅里用，配色由调用方给） */
@Composable
fun TripleStat(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    labelColor: Color = TextTertiary,
    unit: String? = null,
) {
    Row(modifier) {
        items.forEach { (value, label) ->
            Column(Modifier.weight(1f)) {
                BigNumber(value, unit = unit, style = Typ.big, color = valueColor)
                Spacer(Modifier.height(3.dp))
                Text(label, fontSize = 11.sp, color = labelColor, fontWeight = FontWeight.Normal)
            }
        }
    }
}
