package com.limiao.notes.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.Ink
import com.limiao.notes.ui.InkSoft
import com.limiao.notes.ui.Line
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.Primary

/** 目标日期的快捷选项（天） */
private val TARGET_PRESETS = listOf(0 to "不设", 90 to "3个月", 180 to "6个月", 365 to "1年")

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
        HealthTopBar("身体数据与目标", onBack)

        Column(Modifier.padding(horizontal = 16.dp)) {

            // ===== 身体数据 =====
            HCard {
                HLabel("身体数据")
                Spacer(Modifier.height(6.dp))

                StepRow(
                    label = "身高",
                    value = "${h.heightCm} cm",
                    onMinus = { if (h.heightCm > 100) update(h.copy(heightCm = h.heightCm - 1)) },
                    onPlus = { if (h.heightCm < 250) update(h.copy(heightCm = h.heightCm + 1)) },
                )
                HorizontalDivider(color = Line)

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("性别", fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
                    Row(Modifier.width(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HChip("男", h.gender == 1) { update(h.copy(gender = 1)) }
                        HChip("女", h.gender == 2) { update(h.copy(gender = 2)) }
                    }
                }
                HorizontalDivider(color = Line)

                StepRow(
                    label = "年龄",
                    value = "$age 岁",
                    onMinus = { if (age > 10) update(h.copy(birthYear = h.birthYear + 1)) },
                    onPlus = { if (age < 120) update(h.copy(birthYear = h.birthYear - 1)) },
                )
                HorizontalDivider(color = Line)

                Spacer(Modifier.height(10.dp))
                Text("日常活动量", fontSize = 13.sp, color = InkSoft)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthProfile.ACTIVITY_LABELS.forEachIndexed { i, label ->
                        HChip(label, h.activityLevel == i + 1) {
                            update(h.copy(activityLevel = i + 1))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "久坐＝几乎不运动，轻度＝每周 1-2 次，中度＝每周 3-5 次，重度＝每周 6 次以上",
                    fontSize = 11.sp, color = Muted,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ===== 目标 =====
            HCard {
                HLabel("体重与目标")
                Spacer(Modifier.height(8.dp))

                // 起始体重
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("起始体重", fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
                    Text(
                        if (init > 0) "${fmt1(init)} kg" else "未设",
                        fontSize = 14.sp,
                        color = if (init > 0) Ink else Muted,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(10.dp))
                    EditChip { weightDialog = "init" }
                }
                HorizontalDivider(color = Line)

                // 目标体重
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("目标体重", fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
                    Text(
                        if (h.targetWeight > 0) "${fmt1(h.targetWeight)} kg" else "未设",
                        fontSize = 14.sp,
                        color = if (h.targetWeight > 0) Primary else Muted,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(10.dp))
                    EditChip { weightDialog = "target" }
                }
                HorizontalDivider(color = Line)

                // 当前体重（只读，来自体重记录）
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("当前体重", fontSize = 13.sp, color = InkSoft, modifier = Modifier.weight(1f))
                    Text(
                        if (cur > 0) "${fmt1(cur)} kg" else "还没有记录",
                        fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("目标日期", fontSize = 13.sp, color = InkSoft)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TARGET_PRESETS.forEach { (days, label) ->
                        val sel = if (days == 0) {
                            h.targetDate.isBlank()
                        } else {
                            h.targetDate == datePlus(days)
                        }
                        HChip(label, sel) {
                            update(h.copy(targetDate = if (days == 0) "" else datePlus(days)))
                        }
                    }
                }
                if (h.targetDate.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    val left = HealthCalc.daysBetween(DateFmt.today(), h.targetDate)
                    Text(
                        "目标日期 ${h.targetDate}（还有 $left 天）",
                        fontSize = 11.sp, color = Muted,
                    )
                    if (cur > 0 && h.targetWeight > 0 && left > 0 && cur > h.targetWeight) {
                        val deficit = HealthCalc.requiredDeficitPerDay(cur, h.targetWeight, h.targetDate)
                        if (deficit > 0) {
                            Text(
                                "按这个日期，平均每天需要 ${fmt0(deficit)} kcal 的热量缺口",
                                fontSize = 11.sp, color = Muted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 自动推算 =====
            HCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HLabel("自动推算")
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Primary.copy(alpha = 0.10f))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) { Text("只读", fontSize = 11.sp, color = Primary) }
                }
                Spacer(Modifier.height(8.dp))
                if (cur <= 0) {
                    Text(
                        "先记录一次体重，这里就会出现你的基础代谢和每日预算",
                        fontSize = 12.sp, color = Muted,
                    )
                } else {
                    ReportRow("基础代谢 BMR", "${fmt0(bmr)} kcal")
                    ReportRow("每日消耗 TDEE", "${fmt0(tdee)} kcal")
                    ReportRow("每日热量预算", "${fmt0(budget)} kcal", accent = true)
                    ReportRow("BMI", "${fmt1(bmi)}（${HealthCalc.bmiLabel(bmi)}）")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "预算 = BMR − 400 kcal，并保证不低于 1200 kcal。" +
                                "BMR 用 Mifflin-St Jeor 公式按当前体重实时计算。",
                        fontSize = 11.sp, color = Muted,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "以上数值为通用公式的估算结果，仅用于日常参考，不能作为医疗或营养处方依据。",
                fontSize = 11.sp, color = Muted,
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

@Composable
private fun EditChip(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) { Text("修改", fontSize = 12.sp, color = InkSoft) }
}

private fun datePlus(days: Int): String {
    val c = java.util.Calendar.getInstance()
    c.add(java.util.Calendar.DAY_OF_MONTH, days)
    return DateFmt.ymd(c)
}
