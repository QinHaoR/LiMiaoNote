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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.limiao.notes.ui.CardBg
import com.limiao.notes.ui.ChartFill
import com.limiao.notes.ui.ChartGrid
import com.limiao.notes.ui.ChartLine
import com.limiao.notes.ui.Success
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.components.BigNumber
import com.limiao.notes.ui.components.InputNumberDialog
import com.limiao.notes.ui.components.LTitleTopBar
import com.limiao.notes.ui.components.PillButton
import com.limiao.notes.ui.components.SectionLabel
import com.limiao.notes.ui.components.SegRow
import com.limiao.notes.ui.components.SheetActionRow
import com.limiao.notes.ui.components.SheetDivider
import com.limiao.notes.ui.components.SheetShell
import com.limiao.notes.ui.components.SkinCard
import com.limiao.notes.ui.components.ThinDivider
import com.limiao.notes.ui.components.rememberSheetStateExpanded
import com.limiao.notes.ui.theme.Dim
import com.limiao.notes.ui.theme.Typ

private val RANGE_DAYS = listOf(7, 30, 90)
private val RANGE_LABELS = listOf("近 7 天", "近 30 天", "近 90 天")

/**
 * ② 体重
 *
 * 曲线是 Canvas 手绘的（零第三方图表库）：网格 + 面积填充 + 折线 + 数据点 + 目标虚线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    data: AppData,
    onSave: (AppData) -> Unit,
    onBack: () -> Unit,
) {
    val today = DateFmt.today()
    var rangeIndex by remember { mutableStateOf(1) }
    var editing by remember { mutableStateOf<WeightRecord?>(null) }
    var adding by remember { mutableStateOf(false) }
    var actionFor by remember { mutableStateOf<WeightRecord?>(null) }

    val rangeDays = RANGE_DAYS[rangeIndex]
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
        LTitleTopBar("体重", onBack)

        Column(Modifier.padding(horizontal = Dim.screen)) {
            // ===== 当前体重 =====
            SkinCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("当前体重")
                        Spacer(Modifier.height(8.dp))
                        BigNumber(if (cur > 0) fmt1(cur) else "—", unit = "kg", style = Typ.hero)
                    }
                    if (lost != 0.0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (lost > 0) "已减重" else "已回升",
                                fontSize = 12.sp, color = TextTertiary,
                            )
                            Spacer(Modifier.height(6.dp))
                            BigNumber(
                                fmt1(Math.abs(lost)),
                                unit = "kg",
                                style = Typ.mid,
                                color = if (lost > 0) Success else TextPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 曲线 =====
            SkinCard {
                SectionLabel("体重曲线")
                Spacer(Modifier.height(12.dp))
                SegRow(
                    options = RANGE_LABELS,
                    selectedIndex = rangeIndex,
                    onSelect = { rangeIndex = it },
                )
                Spacer(Modifier.height(14.dp))

                if (shown.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("这段时间还没有体重记录", fontSize = 13.sp, color = TextTertiary)
                    }
                } else {
                    WeightChart(
                        records = shown,
                        target = target,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        ChartStat("最高", fmt1(shown.maxOf { it.weight }) + " kg")
                        ChartStat("最低", fmt1(shown.minOf { it.weight }) + " kg")
                        ChartStat("记录", "${shown.size} 次")
                    }
                    if (target > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "橙色虚线 = 目标体重 ${fmt1(target)} kg",
                            fontSize = 11.sp, color = TextTertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 记录按钮 =====
            PillButton(
                text = if (history.any { it.date == today }) "修改今日体重" else "记录今日体重",
                onClick = { adding = true },
            )

            // ===== 历史 =====
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("历史记录")
                Spacer(Modifier.height(10.dp))
                SkinCard(padding = 0.dp) {
                    history.take(60).forEachIndexed { i, r ->
                        if (i > 0) {
                            ThinDivider(Modifier.padding(start = Dim.cardPad, end = Dim.cardPad))
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { actionFor = r }
                                .padding(horizontal = Dim.cardPad, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                prettyDate(r.date),
                                fontSize = 13.sp, color = TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${fmt1(r.weight)} kg",
                                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                            )
                        }
                    }
                }
                if (history.size > 60) {
                    Spacer(Modifier.height(6.dp))
                    Text("只显示最近 60 条", fontSize = 11.sp, color = TextTertiary)
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
            sheetState = rememberSheetStateExpanded(),
            containerColor = CardBg,
            dragHandle = null,
        ) {
            SheetShell(
                title = "${prettyDate(rec.date)}　${fmt1(rec.weight)} kg",
                onClose = { actionFor = null },
            ) {
                SheetDivider()
                SheetActionRow("修改这条记录") {
                    editing = rec
                    actionFor = null
                }
                SheetDivider()
                SheetActionRow("删除这条记录", danger = true) {
                    onSave(HealthData.removeWeight(data, rec.date))
                    actionFor = null
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RowScope.ChartStat(label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextTertiary)
    }
}

/**
 * 体重折线图 —— Canvas 手绘。
 * 由下到上：三条网格线 → 面积填充 → 折线 + 数据点 → 目标虚线。
 */
@Composable
private fun WeightChart(
    records: List<WeightRecord>,
    target: Double,
    modifier: Modifier = Modifier,
) {
    if (records.isEmpty()) return
    // 这几个颜色必须在 Composable 作用域先读出来，Canvas 的 DrawScope 里不能读令牌
    val line = ChartLine
    val grid = ChartGrid
    val fill = ChartFill
    val dotRing = CardBg

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

        // 面积填充（把折线下方补上淡淡的色，曲线才有"体量")
        val area = Path().apply {
            moveTo(px(0), h)
            records.forEachIndexed { i, r -> lineTo(px(i), py(r.weight)) }
            lineTo(px(records.size - 1), h)
            close()
        }
        drawPath(area, fill)

        // 目标体重虚线
        if (target > 0 && target in lo..hi) {
            val ty = py(target)
            var x = 0f
            while (x < w) {
                drawLine(
                    line.copy(alpha = 0.45f),
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
            drawCircle(dotRing, radius = 8f, center = c)
            drawCircle(line, radius = 5f, center = c)
        }
    }
}
