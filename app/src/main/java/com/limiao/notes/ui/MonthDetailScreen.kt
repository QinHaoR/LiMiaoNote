package com.limiao.notes.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.CategoryColors

/** 某月明细：汇总 + 支出结构 + 按天流水（点击项弹出编辑/删除） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(ym: String, data: AppData, onBack: () -> Unit, onSave: (AppData) -> Unit) {
    val txns = data.transactions.filter { it.date.startsWith(ym) }
    val expense = txns.filter { it.type == "expense" }.sumOf { it.amount }
    val income = txns.filter { it.type == "income" }.sumOf { it.amount }
    val byCat = txns.filter { it.type == "expense" }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList().sortedByDescending { it.second }
    val groups = txns.groupBy { it.date }.toSortedMap(compareByDescending { it })

    var editId by remember { mutableStateOf<String?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var actionFor by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 顶部：返回 + 月份
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "‹",
                fontSize = 20.sp,
                color = Muted,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Text(
                monthLabel(ym),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(28.dp))
        }
        Spacer(Modifier.height(12.dp))

        // 汇总
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MonthSum("支出", expense, Primary)
                MonthSum("收入", income, IncomeGreen)
                MonthSum("结余", income - expense, Ink)
            }
        }
        Spacer(Modifier.height(12.dp))

        // 支出结构
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("支出结构", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                if (byCat.isEmpty()) {
                    Text("这个月没有支出", color = Muted, fontSize = 12.sp)
                } else {
                    byCat.take(6).forEach { (cat, amt) ->
                        val colors = CategoryColors.of(cat)
                        val pct = if (expense > 0) amt / expense else 0.0
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                cat,
                                modifier = Modifier
                                    .background(colors.bg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                color = colors.text, fontSize = 11.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(Line, RoundedCornerShape(3.dp)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((pct * 100).toFloat() / 100f)
                                        .height(6.dp)
                                        .background(Primary, RoundedCornerShape(3.dp)),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("¥${fmtMoney(amt)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // 流水（按天，点击项弹出编辑/删除）
        if (groups.isEmpty()) {
            Text("这个月没有记录", color = Muted, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            groups.forEach { (day, items) ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Row(
                        Modifier.fillMaxWidth().background(Bg).padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(day, color = Muted, fontSize = 12.sp)
                        val de = items.filter { it.type == "expense" }.sumOf { it.amount }
                        val di = items.filter { it.type == "income" }.sumOf { it.amount }
                        Text(
                            buildString {
                                if (de > 0) append("支出 ¥${fmtMoney(de)}")
                                if (de > 0 && di > 0) append("  ")
                                if (di > 0) append("收入 ¥${fmtMoney(di)}")
                            },
                            color = Muted, fontSize = 12.sp,
                        )
                    }
                    HorizontalDivider()
                    items.forEach { t ->
                        if (editId == t.id) {
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = editAmount,
                                    onValueChange = { editAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = editNote,
                                    onValueChange = { editNote = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    Button(
                                        onClick = {
                                            val amt = editAmount.toDoubleOrNull()
                                            if (amt != null && amt > 0) {
                                                onSave(data.copy(
                                                    transactions = data.transactions.map {
                                                        if (it.id == t.id) it.copy(amount = amt, note = editNote.trim()) else it
                                                    }
                                                ))
                                            }
                                            editId = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    ) { Text("保存") }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = { editId = null }) { Text("取消", color = Muted) }
                                }
                            }
                        } else {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { actionFor = t.id }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val colors = CategoryColors.of(t.category)
                                Text(
                                    t.category,
                                    modifier = Modifier.background(colors.bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = colors.text, fontSize = 12.sp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (t.note.isBlank()) "—" else t.note,
                                    color = InkSoft, fontSize = 14.sp, maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                                t.time?.let {
                                    Text(it, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                                }
                                Text(
                                    (if (t.type == "income") "+" else "-") + "¥" + fmtMoney(t.amount),
                                    color = if (t.type == "income") IncomeGreen else Ink,
                                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // 记录操作弹层（点击列表项弹出：编辑 / 删除）
    actionFor?.let { id ->
        val t = data.transactions.find { it.id == id }
        ModalBottomSheet(
            onDismissRequest = { actionFor = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "${t?.category ?: ""}  ${t?.note?.ifBlank { "" } ?: ""}    ${if (t?.type == "income") "+" else "-"}¥${fmtMoney(t?.amount ?: 0.0)}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (t != null) { editId = t.id; editAmount = t.amount.toString(); editNote = t.note }
                            actionFor = null
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Edit, null, tint = InkSoft, modifier = Modifier.padding(end = 12.dp))
                    Text("编辑这条记录", color = Ink)
                }
                HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (t != null) {
                                onSave(data.copy(transactions = data.transactions.filter { it.id != t.id }))
                            }
                            actionFor = null
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("删除这条记录", color = androidx.compose.ui.graphics.Color(0xFFDC2626))
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MonthSum(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text("¥${fmtMoney(value)}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
