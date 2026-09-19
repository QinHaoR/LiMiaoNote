package com.limiao.notes.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.limiao.notes.data.HealthProfile
import com.limiao.notes.data.fmt0
import com.limiao.notes.data.fmt1
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.AccentSoft
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.Danger
import com.limiao.notes.ui.Success
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.Warning
import com.limiao.notes.ui.Water
import com.limiao.notes.ui.components.BigNumber
import com.limiao.notes.ui.components.DataCell
import com.limiao.notes.ui.components.InputNumberDialog
import com.limiao.notes.ui.components.LTitleTopBar
import com.limiao.notes.ui.components.SectionLabel
import com.limiao.notes.ui.components.SegChipW
import com.limiao.notes.ui.components.SkinCard
import com.limiao.notes.ui.components.StepperRow
import com.limiao.notes.ui.components.ThinDivider
import com.limiao.notes.ui.components.ValueRow
import com.limiao.notes.ui.theme.Dim
import com.limiao.notes.ui.theme.Typ

/** 目标日期的快捷选项（天） */
private val TARGET_PRESETS = listOf(0 to "不设", 90 to "3 个月", 180 to "6 个月", 365 to "1 年")

/** BMI 色阶条的取值区间（用于把 BMI 映射到 0..1 的指针位置） */
private const val BMI_LO = 15.0
private const val BMI_HI = 35.0

/**
 * ④ 身体数据与目标
 *
 * 这里只存「静态数据」：身高、性别、出生年、活动量、起点/目标体重、目标日期。
 * BMR / TDEE / 每日预算 / BMI 都是**实时算出来的**，不落库，避免多处数据打架。
 */
@Composable
fun HealthProfileScreen(
    data: AppData,
    onSave: (AppData) -> Unit,
    onBack: () -> Unit,
) {
    val h = data.health
    var weightDialog by remember { mutableStateOf<String?>(null) }

    val cur = HealthData.currentWeight(data)
    val init = HealthData.initialWeight(data)
    val age = HealthCalc.age(h.birthYear)
    val bmr = HealthCalc.bmr(cur, h.heightCm, age, h.gender)
    val tdee = HealthCalc.tdee(bmr, h.activityLevel)
    val budget = HealthCalc.dailyBudget(bmr)
    val bmi = HealthCalc.bmi(cur, h.heightCm)

    fun update(next: HealthProfile) = onSave(data.copy(health = next))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        LTitleTopBar("身体数据与目标", onBack)

        Column(Modifier.padding(horizontal = Dim.screen)) {

            // ===== BMI =====
            SkinCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("BMI", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Accent)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        SectionLabel("身体质量指数")
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            BigNumber(if (bmi > 0) fmt1(bmi) else "—", style = Typ.big)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                HealthCalc.bmiLabel(bmi),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = bmiColor(bmi),
                                modifier = Modifier.padding(bottom = 5.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                BmiScaleBar(bmi)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("偏瘦" to Water, "正常" to Success, "偏胖" to Warning, "肥胖" to Danger)
                        .forEach { (label, c) ->
                            Row(
                                Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(c),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(label, fontSize = 10.sp, color = TextTertiary)
                            }
                        }
                }
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 身体数据 =====
            SkinCard {
                SectionLabel("身体数据")
                Spacer(Modifier.height(8.dp))

                StepperRow(
                    label = "身高",
                    value = "${h.heightCm} cm",
                    onMinus = { if (h.heightCm > 100) update(h.copy(heightCm = h.heightCm - 1)) },
                    onPlus = { if (h.heightCm < 250) update(h.copy(heightCm = h.heightCm + 1)) },
                )
                ThinDivider(Modifier.padding(vertical = 4.dp))

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("性别", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    Row(Modifier.width(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegChipW("男", h.gender == 1) { update(h.copy(gender = 1)) }
                        SegChipW("女", h.gender == 2) { update(h.copy(gender = 2)) }
                    }
                }
                ThinDivider(Modifier.padding(vertical = 4.dp))

                StepperRow(
                    label = "年龄",
                    value = "$age 岁",
                    onMinus = { if (age > 10) update(h.copy(birthYear = h.birthYear + 1)) },
                    onPlus = { if (age < 120) update(h.copy(birthYear = h.birthYear - 1)) },
                )

                Spacer(Modifier.height(10.dp))
                Text("日常活动量", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthProfile.ACTIVITY_LABELS.forEachIndexed { i, label ->
                        SegChipW(label, h.activityLevel == i + 1) {
                            update(h.copy(activityLevel = i + 1))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "久坐＝几乎不运动，轻度＝每周 1-2 次，中度＝每周 3-5 次，重度＝每周 6 次以上",
                    fontSize = 11.sp, color = TextTertiary,
                )
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 目标 =====
            SkinCard {
                SectionLabel("体重与目标")
                Spacer(Modifier.height(8.dp))

                WeightRow(
                    label = "起始体重",
                    value = if (init > 0) "${fmt1(init)} kg" else "未设",
                    set = init > 0,
                    onEdit = { weightDialog = "init" },
                )
                ThinDivider(Modifier.padding(vertical = 4.dp))

                WeightRow(
                    label = "目标体重",
                    value = if (h.targetWeight > 0) "${fmt1(h.targetWeight)} kg" else "未设",
                    set = h.targetWeight > 0,
                    accent = true,
                    onEdit = { weightDialog = "target" },
                )
                ThinDivider(Modifier.padding(vertical = 4.dp))

                // 当前体重只读，来自体重记录
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("当前体重", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    Text(
                        if (cur > 0) "${fmt1(cur)} kg" else "还没有记录",
                        fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("目标日期", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TARGET_PRESETS.forEach { (days, label) ->
                        val sel = if (days == 0) {
                            h.targetDate.isBlank()
                        } else {
                            h.targetDate == datePlus(days)
                        }
                        SegChipW(label, sel) {
                            update(h.copy(targetDate = if (days == 0) "" else datePlus(days)))
                        }
                    }
                }
                if (h.targetDate.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    val left = HealthCalc.daysBetween(DateFmt.today(), h.targetDate)
                    Text(
                        "目标日期 ${h.targetDate}（还有 $left 天）",
                        fontSize = 11.sp, color = TextTertiary,
                    )
                    if (cur > 0 && h.targetWeight > 0 && left > 0 && cur > h.targetWeight) {
                        val deficit = HealthCalc.requiredDeficitPerDay(cur, h.targetWeight, h.targetDate)
                        if (deficit > 0) {
                            Text(
                                "按这个日期，平均每天需要 ${fmt0(deficit)} kcal 的热量缺口",
                                fontSize = 11.sp, color = TextTertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 自动推算（只读）=====
            SkinCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("自动推算")
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(Dim.radiusChip))
                            .background(AccentSoft)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) { Text("只读", fontSize = 11.sp, color = Accent) }
                }
                Spacer(Modifier.height(12.dp))
                if (cur <= 0) {
                    Text(
                        "先记录一次体重，这里就会出现你的基础代谢和每日预算",
                        fontSize = 13.sp, color = TextTertiary,
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DataCell("基础代谢 BMR", "${fmt0(bmr)}", Modifier.weight(1f))
                        DataCell("每日消耗 TDEE", "${fmt0(tdee)}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DataCell("每日热量预算", "${fmt0(budget)}", Modifier.weight(1f))
                        DataCell("BMI", "${fmt1(bmi)}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    ValueRow("预算口径", "BMR − 400，且不低于 1200", accent = true)
                    Text(
                        "BMR 用 Mifflin-St Jeor 公式按当前体重实时计算，全部本地完成。",
                        fontSize = 11.sp, color = TextTertiary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "以上数值为通用公式的估算结果，仅用于日常参考，不能作为医疗或营养处方依据。",
                fontSize = 11.sp, color = TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // ===== 体重输入 =====
    val dlg = weightDialog
    if (dlg != null) {
        val isInit = dlg == "init"
        InputNumberDialog(
            title = if (isInit) "起始体重" else "目标体重",
            initial = fmt1(if (isInit) init else h.targetWeight).takeIf { it != "0" } ?: "",
            suffix = "kg",
            onDismiss = { weightDialog = null },
            onConfirm = { v ->
                update(
                    if (isInit) h.copy(initialWeight = v) else h.copy(targetWeight = v)
                )
                weightDialog = null
            },
        )
    }
}

/** BMI 分级对应的强调色 */
@Composable
private fun bmiColor(bmi: Double): Color = when {
    bmi <= 0.0 -> TextTertiary
    bmi < 18.5 -> Water
    bmi < 24.0 -> Success
    bmi < 28.0 -> Warning
    else -> Danger
}

/**
 * BMI 色阶条：四段按区间宽度分配，指针标出当前位置。
 * 四段宽度对应 BMI 区间：15→18.5 / 18.5→24 / 24→28 / 28→35。
 */
@Composable
private fun BmiScaleBar(bmi: Double) {
    val fractions = listOf(3.5f, 5.5f, 4f, 7f)
    val total = fractions.sum()
    val colors = listOf(Water, Success, Warning, Danger)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val w = maxWidth
        Column {
            Row(Modifier.fillMaxWidth().height(8.dp)) {
                fractions.forEachIndexed { i, f ->
                    Box(
                        Modifier
                            .weight(f / total)
                            .height(8.dp)
                            .background(colors[i]),
                    )
                }
            }
            // 指针：夹在 0..1 之间，不会跑出条外
            Box(Modifier.fillMaxWidth().height(12.dp)) {
                if (bmi > 0) {
                    val frac = ((bmi - BMI_LO) / (BMI_HI - BMI_LO)).coerceIn(0.0, 1.0).toFloat()
                    Box(
                        Modifier
                            .offset(x = w * frac - 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TextPrimary),
                    )
                }
            }
        }
    }
}

/** 体重一行：标签 + 值 + 「修改」小胶囊 */
@Composable
private fun WeightRow(
    label: String,
    value: String,
    set: Boolean,
    onEdit: () -> Unit,
    accent: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 14.sp,
            color = if (!set) TextTertiary else if (accent) Accent else TextPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(Dim.radiusChip))
                .background(Bg)
                .clickable(onClick = onEdit)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) { Text("修改", fontSize = 12.sp, color = TextSecondary) }
    }
}

private fun datePlus(days: Int): String {
    val c = java.util.Calendar.getInstance()
    c.add(java.util.Calendar.DAY_OF_MONTH, days)
    return DateFmt.ymd(c)
}
