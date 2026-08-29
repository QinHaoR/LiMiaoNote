package com.limiao.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DayNote
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.FlowLevels
import com.limiao.notes.data.IdGen
import com.limiao.notes.data.Period
import com.limiao.notes.data.PeriodSymptoms
import java.util.Calendar

// ==================== 经期规则（对应 Web 版 periodOps） ====================

private fun addDays(date: String, n: Int): String {
    val c = Calendar.getInstance()
    val parts = date.split("-").map { it.toInt() }
    c.set(parts[0], parts[1] - 1, parts[2])
    c.add(Calendar.DAY_OF_MONTH, n)
    val p = { x: Int -> x.toString().padStart(2, '0') }
    return "${c.get(Calendar.YEAR)}-${p(c.get(Calendar.MONTH) + 1)}-${p(c.get(Calendar.DAY_OF_MONTH))}"
}

/** 合并重叠/相邻段（间隔 ≤1 天合并） */
private fun normalize(periods: List<Period>): List<Period> {
    val sorted = periods.sortedBy { it.startDate }
    val out = ArrayList<Period>()
    for (cur in sorted) {
        val last = out.lastOrNull()
        if (last == null) { out.add(cur); continue }
        if (last.endDate == null) continue // 未结束吸收后续
        if (cur.endDate == null) {
            if (cur.startDate <= addDays(last.endDate, 1)) out[out.size - 1] = last.copy(endDate = null)
            else out.add(cur)
            continue
        }
        if (cur.startDate <= addDays(last.endDate, 1)) {
            if (cur.endDate > last.endDate) out[out.size - 1] = last.copy(endDate = cur.endDate)
        } else out.add(cur)
    }
    return out
}

/** 所有经期日集合（Set<String>，供日历标记） */
internal fun periodDateSet(periods: List<Period>): Set<String> {
    val set = HashSet<String>()
    for (p in periods) {
        val end = p.endDate ?: p.startDate
        var d = p.startDate
        while (d <= end) { set.add(d); d = addDays(d, 1) }
    }
    return set
}

/** 预测下次开始日 */
internal fun predictNext(periods: List<Period>, avgCycle: Int): String? {
    val ends = periods.filter { it.endDate != null }.sortedBy { it.startDate }
    val last = ends.lastOrNull() ?: return null
    return addDays(last.startDate, avgCycle)
}

// ==================== 页面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodScreen(data: AppData, onSave: (AppData) -> Unit) {
    var ym by remember { mutableStateOf(currentYm()) }
    var selected by remember { mutableStateOf<String?>(null) }

    val (year, month) = ym.split("-").map { it.toInt() }
    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=周日
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = DateFmt.today()

    val periodSet = periodDateSet(data.periods)
    val next = predictNext(data.periods, data.settings.avgCycleLength)
    val todayInPeriod = periodSet.contains(today)

    // 今日状态
    val todayPeriod = data.periods.find { today >= it.startDate && (it.endDate == null || today <= it.endDate) }
    val dayIndex = todayPeriod?.let { addDays(it.startDate, 1).let { _ -> 0 } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("经期", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        // 今日状态卡
        Card(colors = CardDefaults.cardColors(containerColor = Primary), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (todayInPeriod) "今天在经期" else "今天不在经期",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    next?.let { "预计下次：$it" } ?: "还没有经期记录",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 月份切换 + 日历
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("‹", modifier = Modifier.clickable { ym = shiftMonth(ym, -1) }.padding(8.dp), color = Muted, fontSize = 18.sp)
                    Text(monthLabel(ym), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    Text("›", modifier = Modifier.clickable { ym = shiftMonth(ym, 1) }.padding(8.dp), color = Muted, fontSize = 18.sp)
                }
                // 星期表头
                Row(Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                // 日历网格
                val startOffset = firstDow - 1
                val cells = startOffset + daysInMonth
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val dayNum = r * 7 + col - startOffset + 1
                            val date = if (dayNum in 1..daysInMonth) {
                                val m = month.toString().padStart(2, '0')
                                val d = dayNum.toString().padStart(2, '0')
                                "$year-$m-$d"
                            } else null
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(
                                        when {
                                            date == null -> Color.Transparent
                                            periodSet.contains(date) -> Primary
                                            date == today -> Bg
                                            else -> Color.Transparent
                                        },
                                        CircleShape,
                                    )
                                    .clickable(enabled = date != null) { selected = date },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (date != null) {
                                    Text(
                                        dayNum.toString(),
                                        color = when {
                                            periodSet.contains(date) -> Color.White
                                            date == today -> Primary
                                            else -> Ink
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 周期历史
        Text("历史周期", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        if (data.periods.isEmpty()) {
            Text("还没有经期记录，点击日历日期标记", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            data.periods.sortedByDescending { it.startDate }.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        "${p.startDate}  ~  ${p.endDate ?: "进行中"}",
                        color = InkSoft, fontSize = 14.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // 选中某天的底部面板
    if (selected != null) {
        val date = selected!!
        val dayNote = data.dayNotes.find { it.date == date }
        val inPeriod = data.periods.find { date >= it.startDate && (it.endDate == null || date <= it.endDate) }
        var flow by remember { mutableStateOf(dayNote?.flow ?: "") }
        var symptoms by remember { mutableStateOf<List<String>>(dayNote?.symptoms ?: emptyList()) }
        var noteText by remember { mutableStateOf(dayNote?.note ?: "") }

        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(date, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (inPeriod != null) {
                    Text(
                        if (inPeriod.endDate == null) "正在经期" else "经期第 ${addDays(inPeriod.startDate, 1).let { _ -> 1 }} 天",
                        color = Primary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))

                // 流量
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowLevels.ALL.forEach { f ->
                        Text(
                            f,
                            modifier = Modifier
                                .clickable { flow = if (flow == f) "" else f }
                                .background(if (flow == f) Primary else Bg, RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            color = if (flow == f) Color.White else InkSoft,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // 症状
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodSymptoms.ALL.forEach { s ->
                        val on = symptoms.contains(s)
                        Text(
                            s,
                            modifier = Modifier
                                .clickable { symptoms = if (on) symptoms - s else symptoms + s }
                                .background(if (on) Primary else Bg, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            color = if (on) Color.White else InkSoft, fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // 备注
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("备注（选填）") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(14.dp))

                // 保存按天记录
                Button(
                    onClick = {
                        val dn = DayNote(date, flow.ifBlank { null }, symptoms, noteText.trim(), date)
                        val next = data.copy(
                            dayNotes = data.dayNotes.filter { it.date != date } + dn
                        )
                        onSave(next)
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary),
                ) { Text("保存当天记录") }
                Spacer(Modifier.height(10.dp))

                // 设为开始 / 结束
                Row {
                    Button(
                        onClick = {
                            val next = markStart2(data.periods, date)
                            onSave(data.copy(periods = next)); selected = null
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary),
                    ) { Text("设为开始") }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val next = markEnd2(data.periods, date)
                            onSave(data.copy(periods = next)); selected = null
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Line, contentColor = Ink),
                    ) { Text("设为结束") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** 设为开始：已在段内则改开始日，否则新建 */
private fun markStart2(periods: List<Period>, date: String): List<Period> {
    val hit = periods.find { date >= it.startDate && (it.endDate == null || date <= it.endDate) }
    val next = if (hit != null) {
        periods.map { if (it.id == hit.id) it.copy(startDate = date) else it }
    } else {
        periods + Period(IdGen.new(), date, null, emptyList(), null, java.time.Instant.now().toString())
    }
    return normalize(next)
}

/** 设为结束：开始日 ≤ date 的最后一段，优先未结束 */
private fun markEnd2(periods: List<Period>, date: String): List<Period> {
    val candidates = periods.filter { it.startDate <= date }
    val target = candidates.firstOrNull { it.endDate == null }
        ?: candidates.maxByOrNull { it.startDate }
        ?: return periods
    val next = periods.map { if (it.id == target.id) it.copy(endDate = date) else it }
    return normalize(next)
}
