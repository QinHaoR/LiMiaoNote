package com.limiao.notes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.fmt0
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.Danger
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.theme.Dim
import com.limiao.notes.ui.theme.Typ

/** 一格营养素要显示的内容 */
data class MacroSpec(
    val name: String,
    val actualG: Double,
    val targetG: Double,   // 0 = 还没算出目标（比如没记录体重）
)

/**
 * 营养素三格（碳水 / 蛋白 / 脂肪）—— 概览页与饮食页共用。
 *
 * 显示逻辑（这是重点，不是随便摆三个数字）：
 * ```
 * 碳水                    还差 303     ← 名称 + 「可摄入」剩余量（超标时变红显示「超 X」）
 * 47 / 350 g                          ← 已摄入 / 目标
 * ▬▬▬▬▬░░░░░░                         ← 进度条（超标时变红）
 * ```
 * 没算出目标时（`targetG <= 0`）退化成只显示「47 g」，进度条留空，不显示"还差"。
 */
@Composable
fun MacroRow(
    specs: List<MacroSpec>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        specs.forEach { s ->
            MacroCell(spec = s, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MacroCell(spec: MacroSpec, modifier: Modifier = Modifier) {
    val hasTarget = spec.targetG > 0
    val left = spec.targetG - spec.actualG
    val over = hasTarget && left < 0
    val valueColor = if (over) Danger else TextPrimary

    Column(modifier) {
        // 名称 + 可摄入（剩余）量
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                spec.name,
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (hasTarget) {
                Text(
                    if (over) "超 ${fmt0(-left)}" else "还差 ${fmt0(left)}",
                    fontSize = 10.sp,
                    fontWeight = if (over) FontWeight.Medium else FontWeight.Normal,
                    color = if (over) Danger else TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 已摄入 / 目标
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                fmt0(spec.actualG),
                style = Typ.mid,
                color = valueColor,
            )
            Text(
                if (hasTarget) " / ${fmt0(spec.targetG)} g" else " g",
                fontSize = 10.sp,
                color = TextTertiary,
            )
        }

        Spacer(Modifier.height(6.dp))

        ThinBar(
            progress = if (hasTarget) (spec.actualG / spec.targetG).toFloat() else 0f,
            color = if (over) Danger else Accent,
            height = Dim.barHair,
        )
    }
}
