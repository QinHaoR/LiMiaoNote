package com.limiao.notes.ui.health

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.HealthCalc
import com.limiao.notes.data.HealthData
import com.limiao.notes.data.fmt0
import com.limiao.notes.data.fmt1
import com.limiao.notes.data.prettyDate
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.AccentSoft
import com.limiao.notes.ui.Danger
import com.limiao.notes.ui.Success
import com.limiao.notes.ui.SuccessSoft
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.Water
import com.limiao.notes.ui.WaterSoft
import com.limiao.notes.ui.components.BannerCard
import com.limiao.notes.ui.components.BigNumber
import com.limiao.notes.ui.components.EntryTile
import com.limiao.notes.ui.components.MacroRow
import com.limiao.notes.ui.components.MacroSpec
import com.limiao.notes.ui.components.PillButton
import com.limiao.notes.ui.components.RingProgress
import com.limiao.notes.ui.components.SectionLabel
import com.limiao.notes.ui.components.SegmentedBar
import com.limiao.notes.ui.components.SkinCard
import com.limiao.notes.ui.components.StepButton
import com.limiao.notes.ui.components.TripleStat
import com.limiao.notes.ui.components.ThinBar
import com.limiao.notes.ui.components.ThinDivider
import com.limiao.notes.ui.theme.Dim
import com.limiao.notes.ui.theme.Typ

/** 饮水目标（ml） */
private const val WATER_TARGET = 2000
private const val WATER_STEP = 250

/**
 * ① 概览 —— 「健康」tab 的首屏，同时也是整个模块的入口。
 *
 * 结构：页头 → 今日摄入环卡 → 体重卡 → 饮水卡 → 功能宫格 → 目标进度 → 本周小结（深色横幅）
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
    val macros = HealthData.macrosOf(data, today)
    val macroTarget = HealthCalc.macroTargets(budget)
    val water = HealthData.waterOf(data, today)
    val week = remember(data) { HealthData.weekSummary(data, budget, HealthData.thisWeekDates()) }

    val hasBudget = budget > 0
    val remaining = budget - intake

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dim.screen, vertical = 12.dp),
    ) {
        // ===== 页头 =====
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("健康", style = Typ.pageTitle, color = TextPrimary, modifier = Modifier.weight(1f))
            SectionLabel(prettyDate(today), modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(14.dp))

        // ===== 今日摄入（圆环）—— 整张卡可点，直接进饮食记录页 =====
        SkinCard(onClick = onOpenMeal) {
            // 卡片标题 + 可点提示（没有提示的话用户不知道这张卡能点进去）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("今日摄入")
                Spacer(Modifier.weight(1f))
                Text("记饮食 ›", fontSize = 12.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("已摄入")
                    Spacer(Modifier.height(6.dp))
                    BigNumber(fmt0(intake), style = Typ.mid)
                    Spacer(Modifier.height(2.dp))
                    Text("千卡", fontSize = 11.sp, color = TextTertiary)
                }
                RingProgress(
                    progress = if (hasBudget) (intake / budget).toFloat() else 0f,
                    diameter = 122.dp,
                    stroke = 10.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (!hasBudget) "待设定" else if (remaining >= 0) "还可摄入" else "已超出",
                            fontSize = 11.sp, color = TextTertiary,
                        )
                        Spacer(Modifier.height(2.dp))
                        BigNumber(
                            if (!hasBudget) "—" else fmt0(Math.abs(remaining)),
                            style = Typ.big,
                            color = if (hasBudget && remaining < 0) Danger else TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (hasBudget) "推荐 ${fmt0(budget)}" else "先记录体重",
                            fontSize = 11.sp, color = TextTertiary,
                        )
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    // 这里只放「目标」（剩余量已经由圆环中心表达了，不重复展示）
                    SectionLabel("目标")
                    Spacer(Modifier.height(6.dp))
                    BigNumber(if (hasBudget) fmt0(budget) else "—", style = Typ.mid)
                    Spacer(Modifier.height(2.dp))
                    Text("千卡", fontSize = 11.sp, color = TextTertiary)
                }
            }

            // 营养素三格
            Spacer(Modifier.height(16.dp))
            ThinDivider()
            Spacer(Modifier.height(14.dp))
            MacroRow(
                specs = listOf(
                    MacroSpec("碳水", macros.first, macroTarget.carbsG),
                    MacroSpec("蛋白", macros.second, macroTarget.proteinG),
                    MacroSpec("脂肪", macros.third, macroTarget.fatG),
                ),
            )

            // 还没体重 → 算不出预算
            if (!hasBudget) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "先记录一次体重，才能算出你的每日热量预算",
                    fontSize = 13.sp, color = TextTertiary,
                )
                Spacer(Modifier.height(10.dp))
                PillButton("去记录体重", onClick = onOpenWeight)
            }
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 体重 =====
        SkinCard(onClick = onOpenWeight) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("当前体重")
                    Spacer(Modifier.height(6.dp))
                    BigNumber(if (cur > 0) fmt1(cur) else "—", unit = "kg", style = Typ.hero)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (bmi > 0) {
                        val normal = bmi >= 18.5 && bmi < 24.0
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(Dim.radiusChip))
                                .background(if (normal) SuccessSoft else AccentSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "BMI ${fmt1(bmi)} ${HealthCalc.bmiLabel(bmi)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (normal) Success else Accent,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("查看趋势 ›", fontSize = 12.sp, color = TextTertiary)
                }
            }
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 饮水 =====
        SkinCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("饮水")
                Spacer(Modifier.weight(1f))
                StepButton("－") { onSave(HealthData.setWater(data, today, water - WATER_STEP)) }
                Spacer(Modifier.width(8.dp))
                StepButton("＋") { onSave(HealthData.setWater(data, today, water + WATER_STEP)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                BigNumber("$water", unit = "ml", style = Typ.mid)
                Spacer(Modifier.weight(1f))
                Text("/ $WATER_TARGET ml", fontSize = 12.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(12.dp))
            SegmentedBar(
                filled = water / WATER_STEP,
                total = WATER_TARGET / WATER_STEP,
                color = Water,
            )
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 功能入口（宫格）=====
        SkinCard {
            SectionLabel("健康功能")
            Spacer(Modifier.height(14.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell: Dp = maxWidth / 4
                FlowRow(
                    maxItemsInEachRow = 4,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EntryTile("体重记录", Icons.Filled.InsertChart, Accent, AccentSoft, cell, onClick = onOpenWeight)
                    EntryTile("饮食记录", Icons.Filled.Restaurant, Success, SuccessSoft, cell, onClick = onOpenMeal)
                    EntryTile("身体数据", Icons.Filled.FitnessCenter, Water, WaterSoft, cell, onClick = onOpenProfile)
                    EntryTile("报告", Icons.Filled.Description, Water, WaterSoft, cell, onClick = onOpenReport)
                    EntryTile(
                        "AI 助手", Icons.AutoMirrored.Filled.Chat, Accent, AccentSoft, cell,
                        badge = "待接", onClick = onOpenAi,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 目标进度 =====
        if (h.hasTarget && cur > 0) {
            val init = HealthData.initialWeight(data)
            val progress = HealthCalc.goalProgress(cur, h.targetWeight, init)
            val remain = cur - h.targetWeight
            SkinCard {
                SectionLabel("目标进度")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (remain > 0) "距目标还差" else "已达成目标",
                            fontSize = 12.sp, color = TextTertiary,
                        )
                        Spacer(Modifier.height(6.dp))
                        BigNumber(
                            if (remain > 0) fmt1(remain) else "0",
                            unit = "kg", style = Typ.big, color = Accent,
                        )
                    }
                    Text("目标 ${fmt1(h.targetWeight)} kg", fontSize = 12.sp, color = TextTertiary)
                }
                if (progress >= 0f) {
                    Spacer(Modifier.height(14.dp))
                    ThinBar(progress)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "已完成 ${fmt0((progress * 100).toDouble())}%",
                        fontSize = 12.sp, color = TextTertiary,
                    )
                }
                if (h.targetDate.isNotBlank()) {
                    val days = HealthCalc.daysBetween(today, h.targetDate)
                    if (days > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("距目标日期还有 $days 天", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            }
            Spacer(Modifier.height(Dim.gapSection))
        }

        // ===== 本周小结（深色横幅）=====
        BannerCard {
            SectionLabelOnDark("本周小结")
            Spacer(Modifier.height(14.dp))
            TripleStat(
                items = listOf(
                    "${week.dayCount}" to "已记录",
                    (if (hasBudget) "${week.onTargetDays}" else "—") to "达标天数",
                    when {
                        week.weightChange == 0.0 -> "—" to "周减重"
                        week.weightChange > 0 -> fmt1(week.weightChange) to "周减重"
                        else -> fmt1(-week.weightChange) to "周回升"
                    },
                ),
                valueColor = Color.White,
                labelColor = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (week.avgCalories > 0) "日均摄入 ${fmt0(week.avgCalories)} kcal" else "本周还没有饮食记录",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 深色卡片上的大写小标签（白卡片用 SectionLabel，深色底要换浅色字） */
@Composable
private fun SectionLabelOnDark(text: String) {
    Text(
        text.uppercase(),
        style = Typ.label,
        color = Color.White.copy(alpha = 0.6f),
    )
}
