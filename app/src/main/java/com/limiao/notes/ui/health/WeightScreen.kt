package com.limiao.notes.ui.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.HealthData
import com.limiao.notes.data.WeightRecord
import com.limiao.notes.data.fmt1
import com.limiao.notes.data.prettyDate
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.IncomeGreen
import com.limiao.notes.ui.Ink
import com.limiao.notes.ui.InkSoft
import com.limiao.notes.ui.Line
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.Primary

private val RANGES = listOf(7 to "近 7 天", 30 to "近 30 天", 90 to "近 90 天")

/**
 * ② 体重
 *
 * 曲线是 Canvas 手绘的（零第三方图表库），只有一条折线 + 数据点 + 目标虚线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    data: AppData,
    onSave: (AppData) -> Unit,
    onBack: () -> Unit,
) {
    val today = DateFmt.today()
    var rangeDays by remember { mutableStateOf(30) }
    var editing by remember { mutableStateOf<WeightRecord?>(null) }
    var adding by remember { mutableStateOf(false) }
    var actionFor by remember { mutableStateOf<WeightRecord?>(null) }

    val cur = HealthData.currentWeight(data)
    val lost = HealthData.lostWeight(data)
    val target = data.health.targetWeight

    val from = remember(rangeDays) { HealthData.recentDates(rangeDays).first() }
    val shown = remember(data.weights, rangeDays) {
        data.weights.filter { it.weight > 0 && it.date >= from }.sortedBy { it.date }
    }
    val history = remember(data.weights) {
        data.weights.filter { it.weight > 0 }.sortedByDescending { it.date }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        HealthTopBar("体重", onBack)

        Column(Modifier.padding(horizontal = 16.dp)) {
            // ===== 当前体重 =====
            HCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("当前体重", fontSize = 12.sp, color = Muted)
                        Spacer(Modifier.height(6.dp))
                        HNumber(if (cur > 0) fmt1(cur) else "—", 38, unit = "kg")
                    }
                    if (lost != 0.0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("已减重", fontSize = 12.sp, color = Muted)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${fmt1(lost)} kg",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (lost > 0) IncomeGreen else Ink,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 曲线 =====
            HCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HLabel("体重曲线")
                    Spacer(Modifier.weight(1f))
                    RANGES.forEach { (d, label) ->
                        val sel = d == rangeDays
                        Box(
                            Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (sel) Primary else Bg)
                                .clickable { rangeDays = d }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                color = if (sel) Color.White else InkSoft,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (shown.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("这段时间还没有体重记录", fontSize = 13.sp, color = Muted)
                    }
                } else {
                    WeightChart(
                        records = shown,
                        target = target,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        ChartStat("最高", fmt1(shown.maxOf { it.weight }) + " kg")
                        ChartStat("最低", fmt1(shown.minOf { it.weight }) + " kg")
                        ChartStat("记录", "${shown.size} 次")
                    }
                    if (target > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "粉色虚线 = 目标体重 ${fmt1(target)} kg",
                            fontSize = 11.sp, color = Muted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 记录按钮 =====
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary)
                    .clickable { adding = true }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (history.any { it.date == today }) "修改今日体重" else "记录今日体重",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                )
            }

            // ===== 历史 =====
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HLabel("历史记录")
                Spacer(Modifier.height(8.dp))
                HCard(padding = 0.dp) {
                    history.take(60).forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(start = 16.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { actionFor = r }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                prettyDate(r.date),
                                fontSize = 13.sp, color = InkSoft,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${fmt1(r.weight)} kg",
                                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink,
                            )
                        }
                    }
                }
                if (history.size > 60) {
                    Spacer(Modifier.height(6.dp))
                    Text("只显示最近 60 条", fontSize = 11.sp, color = Muted)
                }
            }
        }
    }

    // ===== 记录 / 修改 =====
    val targetRecord = editing ?: if (adding) WeightRecord(today, cur, "") else null
    if (targetRecord != null) {
        InputNumberDialog(
            title = if (editing != null) "修改 ${prettyDate(targetRecord.date)}" else "记录今日体重",
            initial = if (targetRecord.weight > 0) fmt1(targetRecord.weight) else "",
            suffix = "kg",
            onDismiss = { editing = null; adding = false },
            onConfirm = { v ->
                onSave(HealthData.setWeight(data, targetRecord.date, v))
                editing = null; adding = false
            },
        )
    }

    // ===== 点击历史：编辑 / 删除 =====
    actionFor?.let { rec ->
        ModalBottomSheet(
            onDismissRequest = { actionFor = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Text(
                prettyDate(rec.date) + "　" + fmt1(rec.weight) + " kg",
                fontSize = 13.sp, color = Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            ListItem(
                headlineContent = { Text("修改这条记录") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editing = rec
                        actionFor = null
                    },
            )
            ListItem(
                headlineContent = {
                    Text("删除这条记录", color = Color(0xFFDC2626))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSave(HealthData.removeWeight(data, rec.date))
                        actionFor = null
                    },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.ChartStat(label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

/**
 * 体重折线图 —— Canvas 手绘。
 * 只有三样东西：三条浅色网格线、一条折线 + 数据点、一条目标虚线。
 */
@Composable
private fun WeightChart(
    records: List<WeightRecord>,
    target: Double,
    modifier: Modifier = Modifier,
) {
    if (records.isEmpty()) return
    val line = Primary
    val grid = Line

    val minW = records.minOf { it.weight }
    val maxW = records.maxOf { it.weight }
    val pad = ((maxW - minW) * 0.25).coerceAtLeast(0.4)
    val lo = minW - pad
    val hi = maxW + pad
    val span = (hi - lo).coerceAtLeast(0.8)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        for (i in 0..2) {
            val y = h * i / 2f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
        }

        fun px(i: Int): Float =
            if (records.size == 1) w / 2f else i * w / (records.size - 1).toFloat()

        fun py(v: Double): Float = ((hi - v) / span * h).toFloat()

        // 目标体重虚线
        if (target > 0 && target in lo..hi) {
            val ty = py(target)
            var x = 0f
            while (x < w) {
                drawLine(
                    line.copy(alpha = 0.4f),
                    Offset(x, ty),
                    Offset((x + 9f).coerceAtMost(w), ty),
                    strokeWidth = 3f,
                )
                x += 16f
            }
        }

        val path = Path()
        records.forEachIndexed { i, r ->
            val x = px(i)
            val y = py(r.weight)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path, line,
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        records.forEachIndexed { i, r ->
            val c = Offset(px(i), py(r.weight))
            drawCircle(Color.White, radius = 8f, center = c)
            drawCircle(line, radius = 5f, center = c)
        }
    }
}
