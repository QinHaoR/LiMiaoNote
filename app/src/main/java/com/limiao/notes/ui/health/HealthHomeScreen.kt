package com.limiao.notes.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.HealthCalc
import com.limiao.notes.data.HealthData
import com.limiao.notes.data.fmt0
import com.limiao.notes.data.fmt1
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.IncomeGreen
import com.limiao.notes.ui.Ink
import com.limiao.notes.ui.InkSoft
import com.limiao.notes.ui.Line
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.Primary

private val WaterBlue = Color(0xFF4A9BFF)
private val OverRed = Color(0xFFDC2626)

/** 饮水目标（ml） */
private const val WATER_TARGET = 2000
private const val WATER_STEP = 250

/**
 * ① 概览 —— 「健康」tab 的首屏，同时也是整个模块的入口
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthHomeScreen(
    data: AppData,
    onSave: (AppData) -> Unit,
    onOpenWeight: () -> Unit,
    onOpenMeal: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenAi: () -> Unit,
) {
    val today = DateFmt.today()
    val h = data.health
    val cur = HealthData.currentWeight(data)
    val bmi = HealthCalc.bmi(cur, h.heightCm)
    val bmr = HealthCalc.bmr(cur, h.heightCm, HealthCalc.age(h.birthYear), h.gender)
    val budget = HealthCalc.dailyBudget(bmr)
    val intake = HealthData.caloriesOf(data, today)
    val water = HealthData.waterOf(data, today)
    val lost = HealthData.lostWeight(data)
    val week = remember(data) { HealthData.weekSummary(data, budget, HealthData.thisWeekDates()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("健康", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        // ===== 今日摘要 =====
        HCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("当前体重", fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    HNumber(if (cur > 0) fmt1(cur) else "—", 34, unit = "kg")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BMI", fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    HNumber(if (bmi > 0) fmt1(bmi) else "—", 22)
                    Text(HealthCalc.bmiLabel(bmi), fontSize = 11.sp, color = Muted)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)

            if (budget > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日摄入", fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
                    Text("${fmt0(intake)} / ${fmt0(budget)} kcal", fontSize = 13.sp, color = Ink)
                }
                Spacer(Modifier.height(8.dp))
                HBar(if (budget > 0) (intake / budget).toFloat() else 0f, Primary)
                Spacer(Modifier.height(6.dp))
                val left = budget - intake
                Text(
                    if (left >= 0) "剩余 ${fmt0(left)} kcal" else "已超 ${fmt0(-left)} kcal",
                    fontSize = 12.sp,
                    color = if (left >= 0) IncomeGreen else OverRed,
                )
            } else {
                Text(
                    "先记录一次体重，才能算出你的每日热量预算",
                    fontSize = 13.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary)
                        .clickable(onClick = onOpenWeight)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("去记录体重", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 饮水 =====
        HCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HLabel("饮水")
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg)
                        .clickable {
                            onSave(HealthData.setWater(data, today, water - WATER_STEP))
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) { Text("－", fontSize = 16.sp, color = Muted) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg)
                        .clickable {
                            onSave(HealthData.setWater(data, today, water + WATER_STEP))
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) { Text("＋", fontSize = 16.sp, color = Muted) }
            }
            Spacer(Modifier.height(10.dp))
            Text("$water ml / $WATER_TARGET ml", fontSize = 13.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            WaterDots(water)
        }

        Spacer(Modifier.height(12.dp))

        // ===== 功能入口（宫格）=====
        HCard {
            HLabel("健康功能")
            Text(
                "点图标进各子页面",
                fontSize = 12.sp, color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = maxWidth / 4
                FlowRow(
                    maxItemsInEachRow = 4,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EntryTile(
                        "体重记录", Icons.Filled.InsertChart,
                        Color(0xFF0369A1), Color(0xFFE0F2FE), cell,
                        onClick = onOpenWeight,
                    )
                    EntryTile(
                        "饮食记录", Icons.Filled.Restaurant,
                        Color(0xFF9A3412), Color(0xFFFFEDD5), cell,
                        onClick = onOpenMeal,
                    )
                    EntryTile(
                        "身体数据", Icons.Filled.FitnessCenter,
                        Color(0xFF6D28D9), Color(0xFFEDE9FE), cell,
                        onClick = onOpenProfile,
                    )
                    EntryTile(
                        "报告", Icons.Filled.Description,
                        Color(0xFFB45309), Color(0xFFFEF3C7), cell,
                        onClick = onOpenReport,
                    )
                    EntryTile(
                        "AI 助手", Icons.AutoMirrored.Filled.Chat,
                        Color(0xFFBE185D), Color(0xFFFCE7F3), cell,
                        badge = "待接",
                        onClick = onOpenAi,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 目标进度 =====
        if (h.hasTarget && cur > 0) {
            val init = HealthData.initialWeight(data)
            val progress = HealthCalc.goalProgress(cur, h.targetWeight, init)
            val remain = cur - h.targetWeight
            HCard {
                HLabel("目标进度")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (remain > 0) "距目标还差" else "已达成目标",
                            fontSize = 12.sp, color = Muted,
                        )
                        Spacer(Modifier.height(4.dp))
                        HNumber(
                            if (remain > 0) fmt1(remain) else fmt1(0.0),
                            26, color = Primary, unit = "kg",
                        )
                    }
                    Text(
                        "目标 ${fmt1(h.targetWeight)} kg",
                        fontSize = 12.sp, color = Muted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (progress >= 0f) {
                    HBar(progress, Primary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "已完成 ${fmt0((progress * 100).toDouble())}%",
                        fontSize = 12.sp, color = Muted,
                    )
                }
                if (h.targetDate.isNotBlank()) {
                    val days = HealthCalc.daysBetween(today, h.targetDate)
                    if (days > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "距目标日期还有 $days 天",
                            fontSize = 12.sp, color = Muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ===== 本周小结 =====
        HCard {
            HLabel("本周小结")
            Text(
                "本周已记录 ${week.dayCount} 天",
                fontSize = 12.sp, color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            ReportRow("达标天数", if (budget > 0) "${week.onTargetDays} / ${week.dayCount}" else "—")
            ReportRow("日均摄入", if (week.avgCalories > 0) "${fmt0(week.avgCalories)} kcal" else "—")
            ReportRow(
                "周减重",
                when {
                    week.weightChange == 0.0 -> "—"
                    week.weightChange > 0 -> "↓ ${fmt1(week.weightChange)} kg"
                    else -> "↑ ${fmt1(-week.weightChange)} kg"
                },
                accent = week.weightChange > 0,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 饮水进度圆点（共 8 格，每格 250ml） */
@Composable
private fun WaterDots(ml: Int) {
    val total = WATER_TARGET / WATER_STEP
    val filled = (ml / WATER_STEP).coerceIn(0, total)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (i < filled) WaterBlue else Line),
            )
        }
    }
}
