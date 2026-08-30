package com.limiao.notes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.Categories
import com.limiao.notes.data.CategoryColors
import com.limiao.notes.data.DayPeriods
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.IdGen
import com.limiao.notes.data.QuickNotes
import com.limiao.notes.data.Transaction
import com.limiao.notes.data.VoiceParse
import com.limiao.notes.data.VoiceParsed

@OptIn(ExperimentalMaterial3Api::class)

private fun String.shortDisplay(): String = substring(8, 10).toInt().toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(data: AppData, onSave: (AppData) -> Unit, onOpenMonths: () -> Unit, onOpenMonthDetail: (String) -> Unit) {
    var filter by remember { mutableStateOf("all") }
    var month by remember { mutableStateOf(currentYm()) }

    // 表单状态
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("餐饮") }
    var txnType by remember { mutableStateOf("expense") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateFmt.today()) }
    var time by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var actionFor by remember { mutableStateOf<String?>(null) }

    // 语音记账（系统 SpeechRecognizer，0 成本）
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var voicePreview by remember { mutableStateOf<VoiceParsed?>(null) }
    var voiceUnavailable by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val voiceRecorder = remember(context) {
        VoiceRecorder(
            context = context.applicationContext,
            onText = { text ->
                listening = false
                voicePreview = VoiceParse.parse(text)
            },
            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
        )
    }
    DisposableEffect(Unit) { onDispose { voiceRecorder.cancel() } }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            listening = true
            voiceRecorder.start()
        } else {
            Toast.makeText(context, "需要录音权限才能语音记账", Toast.LENGTH_SHORT).show()
        }
    }
    fun startVoice() {
        if (!voiceRecorder.available()) {
            voiceUnavailable = true
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listening = true
            voiceRecorder.start()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val monthTxns = data.transactions.filter { it.date.startsWith(month) }
    val shown = monthTxns.filter { filter == "all" || it.type == filter }
    val expense = monthTxns.filter { it.type == "expense" }.sumOf { it.amount }
    val income = monthTxns.filter { it.type == "income" }.sumOf { it.amount }
    val groups = shown.groupBy { it.date }.toSortedMap(compareByDescending { it })

    fun addTxn() {
        val amt = amount.toDoubleOrNull() ?: return
        if (amt <= 0) return
        val t = Transaction(
            id = IdGen.new(), amount = amt, category = category, note = note.trim(),
            type = txnType, date = date, time = time ?: DateFmt.nowTime(),
            createdAt = java.time.Instant.now().toString(),
        )
        onSave(data.copy(transactions = data.transactions + t))
        amount = ""; note = ""; time = null
        category = if (txnType == "expense") "餐饮" else "工资"
    }

    fun saveEdit(id: String) {
        val amt = editAmount.toDoubleOrNull() ?: return
        if (amt <= 0) return
        val next = data.copy(
            transactions = data.transactions.map {
                if (it.id == id) it.copy(amount = amt, note = editNote.trim()) else it
            }
        )
        onSave(next); editId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 顶部：筛选 + 月份
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .background(Line, RoundedCornerShape(20.dp))
                    .padding(3.dp),
            ) {
                listOf("all" to "全部", "expense" to "支出", "income" to "收入").forEach { (f, label) ->
                    Text(
                        label,
                        modifier = Modifier
                            .clickable { filter = f }
                            .background(
                                if (filter == f) Primary else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(18.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        color = if (filter == f) androidx.compose.ui.graphics.Color.White else Muted,
                        fontSize = 13.sp,
                        fontWeight = if (filter == f) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // ‹ › 保持切月；中间月份可点 → 进「全部月份」总览页
            Text("‹", modifier = Modifier.clickable { month = shiftMonth(month, -1) }.padding(6.dp), color = Muted, fontSize = 18.sp)
            Text(
                monthLabel(month),
                modifier = Modifier
                    .clickable { onOpenMonths() }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                color = Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            )
            Text("›", modifier = Modifier.clickable { month = shiftMonth(month, 1) }.padding(6.dp), color = Muted, fontSize = 18.sp)
            // 不在当前月时才显示「今天」，避免当前月白占一行空间
            if (month != currentYm()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    "今天",
                    modifier = Modifier
                        .clickable { month = currentYm() }
                        .background(Primary.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 汇总卡片（点击进月份总览）
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMonths() },
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                SumCell("支出", expense, Primary)
                SumCell("收入", income, IncomeGreen)
                SumCell("结余", income - expense, Ink)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 记账卡片
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp)) {
                // 语音 / 文字 记账入口（共用同一套解析 + 预览确认）
                // 语音识别暂不可用：国产 ROM 无标准 RecognitionService，云端 ASR 后续再做
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Bg, RoundedCornerShape(14.dp))
                            .clickable {
                                Toast.makeText(context, "语音识别暂不可用，先用文字记账吧", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Mic, null, tint = Muted, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "语音识别暂不可用",
                            color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (listening) {
                                    listening = false
                                    voiceRecorder.cancel()
                                }
                                showTextInput = true
                            }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.EditNote, null, tint = Primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("文字记账", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 类型切换
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    listOf("expense" to "支出", "income" to "收入").forEach { (f, label) ->
                        Text(
                            label,
                            modifier = Modifier
                                .clickable {
                                    txnType = f
                                    category = if (f == "expense") "餐饮" else "工资"
                                }
                                .background(
                                    if (txnType == f) Primary else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 22.dp, vertical = 8.dp),
                            color = if (txnType == f) androidx.compose.ui.graphics.Color.White else Muted,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 金额（大字号，方便输入）
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.00") },
                    prefix = { Text("¥", fontSize = 22.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(10.dp))

                // 分类 chips（多排自动换行）
                val cats = if (txnType == "expense") Categories.EXPENSE else Categories.INCOME
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    cats.forEach { c ->
                        val colors = CategoryColors.of(c)
                        Text(
                            c,
                            modifier = Modifier
                                .clickable { category = c }
                                .background(if (category == c) Primary else colors.bg, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (category == c) androidx.compose.ui.graphics.Color.White else colors.text,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("备注（选填）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(8.dp))

                // 快捷备注（多排自动换行）
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    QuickNotes.ALL.forEach { q ->
                        Text(
                            q,
                            modifier = Modifier
                                .clickable { note = q }
                                .background(Bg, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = InkSoft, fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // 日期 + [今天] + 时间（一行，居中，日期放大）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        date,
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .background(Bg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = InkSoft, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    )
                    // 选了非今天的日期时，给个快捷回今天（时间不动，用户可自行调整）
                    if (date != DateFmt.today()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "今天",
                            modifier = Modifier
                                .clickable { date = DateFmt.today() }
                                .background(Primary.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        time ?: "选时间",
                        modifier = Modifier
                            .clickable { time = DateFmt.nowTime() }
                            .background(Bg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (time == null) Muted else InkSoft, fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                // 时段快捷（下一行，居中）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    DayPeriods.ALL.forEach { (label, t) ->
                        Text(
                            label,
                            modifier = Modifier
                                .clickable { time = t }
                                .background(Bg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            color = InkSoft, fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { addTxn() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary),
                ) { Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 流水列表（默认收起，只显示最近 3 天，可展开全部）
        val allGroups = groups.entries.toList()
        val showGroups = allGroups.take(3)
        if (groups.isEmpty()) {
            Text("这个月还没有记录", color = Muted, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), textAlign = TextAlign.Center)
        } else {
            showGroups.forEach { (day, items) ->
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
                            // 编辑态
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = editNote, onValueChange = { editNote = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    Button(onClick = { saveEdit(t.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary)) { Text("保存") }
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
            // 查看全部 → 直接跳当前月明细页
            if (groups.size > 3) {
                TextButton(
                    onClick = { onOpenMonthDetail(month) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text("查看全部 ${groups.size} 天 ›", color = Muted) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // 日期选择对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateToMillis(date))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = millisToDate(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
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

    // 语音识别结果预览：AI 只负责填表单，确认后才回填，绝不直接入库
    voicePreview?.let { v ->
        AlertDialog(
            onDismissRequest = { voicePreview = null },
            title = { Text(if (v.amount > 0) "识别结果" else "识别结果（金额没听清）") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        v.text,
                        color = InkSoft, fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Bg, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    PreviewRow("类型", if (v.type == "income") "收入" else "支出")
                    PreviewRow("类别", v.category)
                    PreviewRow("金额", if (v.amount > 0) "¥${fmtMoney(v.amount)}" else "⚠ 没识别到")
                    PreviewRow("日期", v.date)
                    PreviewRow("时间", v.time ?: "—")
                    PreviewRow("备注", v.note.ifBlank { "—" })
                    if (v.amount <= 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "没听清金额，可以放弃后重新说，把金额说清楚（如：花了 15 块）",
                            color = Color(0xFFDC2626), fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    amount = if (v.amount > 0) fmtAmountStr(v.amount) else amount
                    category = v.category
                    txnType = v.type
                    date = v.date
                    time = v.time
                    note = v.note
                    voicePreview = null
                    Toast.makeText(context, "已填入表单，检查后点保存", Toast.LENGTH_SHORT).show()
                }) { Text("填入表单") }
            },
            dismissButton = {
                TextButton(onClick = { voicePreview = null }) { Text("放弃") }
            },
        )
    }

    // 文字记账：打字一句话，走同一套解析（不依赖系统语音服务，任何手机都能用）
    if (showTextInput) {
        AlertDialog(
            onDismissRequest = { showTextInput = false; textInput = "" },
            title = { Text("文字记账") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "把记账说成一句话，例如：\n「今天中午吃了碗面花了15块」\n「昨天打车花了20块」\n「工资发了5000」",
                        color = Muted, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("如：中午吃了碗面花了15块") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = textInput.trim()
                    showTextInput = false
                    textInput = ""
                    if (t.isNotEmpty()) {
                        voicePreview = VoiceParse.parse(t)
                    } else {
                        Toast.makeText(context, "输入内容不能为空", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("解析") }
            },
            dismissButton = {
                TextButton(onClick = { showTextInput = false; textInput = "" }) { Text("取消") }
            },
        )
    }

    // 语音识别服务不可用时，给出可操作的引导（国产 ROM 常见：没有注册识别服务的应用）
    if (voiceUnavailable) {
        AlertDialog(
            onDismissRequest = { voiceUnavailable = false },
            title = { Text("语音识别不可用") },
            text = {
                Text(
                    "你的手机（Redmi/MIUI）没有安装提供语音识别服务的应用，暂时用不了语音记账。\n\n" +
                        "解决办法（任选）：\n" +
                        "1. 应用商店安装「讯飞输入法」（免费，自带语音识别）\n" +
                        "2. 安装后：设置 → 更多设置 → 语言与输入法，设为默认\n" +
                        "3. 返回这里重新点「语音记账」\n\n" +
                        "或者先改用手动记账，等装了输入法再试。",
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { voiceUnavailable = false }) { Text("我知道了") }
            },
        )
    }
}

private fun dateToMillis(date: String): Long {
    val p = date.split("-").map { it.toInt() }
    val c = java.util.Calendar.getInstance()
    c.set(p[0], p[1] - 1, p[2], 0, 0, 0)
    return c.timeInMillis
}

private fun millisToDate(millis: Long): String {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val p = { n: Int -> n.toString().padStart(2, '0') }
    return "${c.get(java.util.Calendar.YEAR)}-${p(c.get(java.util.Calendar.MONTH) + 1)}-${p(c.get(java.util.Calendar.DAY_OF_MONTH))}"
}

@Composable
private fun SumCell(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text("¥${fmtMoney(value)}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

internal fun fmtMoney(v: Double): String {
    val rounded = Math.round(v * 100) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else String.format("%.2f", rounded)
}

/** 金额回填输入框：15.0 → "15"，15.5 → "15.5" */
private fun fmtAmountStr(v: Double): String =
    if (v == Math.floor(v)) v.toLong().toString() else v.toString()

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(52.dp))
        Text(value, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
