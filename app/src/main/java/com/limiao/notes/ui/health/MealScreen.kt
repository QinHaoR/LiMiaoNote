package com.limiao.notes.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.FoodItem
import com.limiao.notes.data.FoodLib
import com.limiao.notes.data.HealthCalc
import com.limiao.notes.data.HealthData
import com.limiao.notes.data.IdGen
import com.limiao.notes.data.MealEntry
import com.limiao.notes.data.MealTypes
import com.limiao.notes.data.fmt0
import com.limiao.notes.data.fmt1
import com.limiao.notes.data.prettyDate
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.IncomeGreen
import com.limiao.notes.ui.Ink
import com.limiao.notes.ui.InkSoft
import com.limiao.notes.ui.Line
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.Primary

private val OverRed = Color(0xFFDC2626)

/** 每餐次配色（沿用全站分类配色风格） */
private fun mealPalette(mealType: String): Pair<Color, Color> = when (mealType) {
    "早餐" -> Color(0xFFB45309) to Color(0xFFFEF3C7)
    "午餐" -> Color(0xFF9A3412) to Color(0xFFFFEDD5)
    "晚餐" -> Color(0xFF6D28D9) to Color(0xFFEDE9FE)
    else -> Color(0xFF0369A1) to Color(0xFFE0F2FE)
}

/**
 * ③ 饮食
 *
 * 录入有三条路，前两条永远可用（不依赖网络）：
 *   1. 本地食物库检索（内置热量表）
 *   2. 手动填克数与热量
 *   （DeepSeek 接入后会再补「文字 → AI 换算」与「拍照 → AI 识别」，失败自动退回前两条）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(
    data: AppData,
    onSave: (AppData) -> Unit,
    onBack: () -> Unit,
) {
    val today = DateFmt.today()
    var date by remember { mutableStateOf(today) }
    var addingTo by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<MealEntry?>(null) }

    val h = data.health
    val cur = HealthData.currentWeight(data)
    val budget = HealthCalc.dailyBudget(
        HealthCalc.bmr(cur, h.heightCm, HealthCalc.age(h.birthYear), h.gender)
    )

    val dayMeals = remember(data.meals, date) { HealthData.mealsOf(data, date) }
    val intake = dayMeals.sumOf { it.calories }
    val macros = HealthData.macrosOf(data, date)
    val targets = HealthCalc.macroTargets(budget)
    val left = budget - intake
    val over = budget > 0 && left < 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        HealthTopBar("饮食", onBack)

        Column(Modifier.padding(horizontal = 16.dp)) {

            // ===== 日期切换 =====
            HCard(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArrowBox("‹") { date = shiftDate(date, -1) }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(prettyDate(date), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        if (date != today) {
                            Text(
                                "点这里回到今天",
                                fontSize = 11.sp, color = Primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { date = today },
                            )
                        }
                    }
                    ArrowBox("›", enabled = date < today) {
                        if (date < today) date = shiftDate(date, 1)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 每日汇总 =====
            HCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HLabel("摄入预算")
                    Spacer(Modifier.weight(1f))
                    if (budget > 0) {
                        val badge = if (over) "已超标" else "进行中"
                        val c = if (over) OverRed else IncomeGreen
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(c.copy(alpha = 0.12f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                        ) { Text(badge, fontSize = 11.sp, color = c) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HNumber(fmt0(intake), 32, unit = "kcal")
                Spacer(Modifier.height(4.dp))
                Text(
                    if (budget > 0) {
                        "预算 ${fmt0(budget)} kcal · " +
                                (if (over) "已超 ${fmt0(-left)}" else "剩余 ${fmt0(left)}") + " kcal"
                    } else {
                        "先在「身体数据」里填身高体重，才能算出预算"
                    },
                    fontSize = 12.sp,
                    color = if (over) OverRed else Muted,
                )
                if (budget > 0) {
                    Spacer(Modifier.height(10.dp))
                    HBar((intake / budget).toFloat(), if (over) OverRed else Primary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 营养素 =====
            HCard {
                HLabel("营养素占比")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    MacroCell("碳水", macros.first, targets.carbsG)
                    MacroCell("蛋白质", macros.second, targets.proteinG)
                    MacroCell("脂肪", macros.third, targets.fatG)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 四餐次 =====
            MealTypes.ALL.forEach { mt ->
                MealSection(
                    mealType = mt,
                    entries = dayMeals.filter { it.mealType == mt }.sortedBy { it.createdAt },
                    budgetInMeal = budget,
                    onAdd = { addingTo = mt },
                    onDelete = { deleting = it },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ===== 添加食物 =====
    addingTo?.let { mt ->
        AddFoodSheet(
            mealType = mt,
            date = date,
            onDismiss = { addingTo = null },
            onConfirm = { entry ->
                onSave(data.copy(meals = data.meals + entry))
                addingTo = null
            },
        )
    }

    // ===== 删除单条 =====
    deleting?.let { e ->
        ModalBottomSheet(
            onDismissRequest = { deleting = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Text(
                "${e.foodName}　${fmt0(e.calories)} kcal",
                fontSize = 13.sp, color = Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            ListItem(
                headlineContent = { Text("删除这条记录", color = OverRed) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSave(data.copy(meals = data.meals.filter { it.id != e.id }))
                        deleting = null
                    },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== 局部组件 ====================

@Composable
private fun ArrowBox(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 18.sp, color = if (enabled) InkSoft else Line)
    }
}

@Composable
private fun RowScope.MacroCell(label: String, actual: Double, target: Double) {
    Column(Modifier.weight(1f)) {
        Text(label, fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(4.dp))
        Text(
            if (target > 0) "${fmt0(actual)}/${fmt0(target)} g" else "${fmt0(actual)} g",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink,
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.padding(end = 10.dp)) {
            HBar(if (target > 0) (actual / target).toFloat() else 0f, Primary, 5.dp)
        }
    }
}

@Composable
private fun MealSection(
    mealType: String,
    entries: List<MealEntry>,
    budgetInMeal: Double,
    onAdd: () -> Unit,
    onDelete: (MealEntry) -> Unit,
) {
    val (tint, chipBg) = mealPalette(mealType)
    val total = entries.sumOf { it.calories }

    HCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipBg)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) { Text(mealType, fontSize = 12.sp, color = tint, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.width(10.dp))
            Text(
                if (entries.isEmpty()) "—" else "${fmt0(total)} kcal",
                fontSize = 12.sp, color = Muted,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.10f))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 11.dp, vertical = 4.dp),
            ) { Text("＋ 添加", fontSize = 12.sp, color = Primary) }
        }

        if (entries.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("还没有记录", fontSize = 12.sp, color = Muted)
        } else {
            Spacer(Modifier.height(4.dp))
            entries.forEach { e ->
                HorizontalDivider(color = Line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDelete(e) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.foodName, fontSize = 13.sp, color = Ink)
                        val sub = buildString {
                            if (e.grams > 0) append("${fmt0(e.grams)} g")
                            if (e.carbs > 0 || e.protein > 0 || e.fat > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("碳${fmt0(e.carbs)} 蛋${fmt0(e.protein)} 脂${fmt0(e.fat)}")
                            }
                        }
                        if (sub.isNotEmpty()) {
                            Text(
                                sub, fontSize = 11.sp, color = Muted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    Text(
                        "${fmt0(e.calories)} kcal",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkSoft,
                    )
                }
            }
            if (budgetInMeal > 0 && total > budgetInMeal) {
                Spacer(Modifier.height(6.dp))
                Text("这一顿已超过全天预算", fontSize = 11.sp, color = OverRed)
            }
        }
    }
}

/**
 * 添加食物弹层（底部弹出，不用对话框 —— 里面要放可滚动列表，弹层不存在嵌套滚动问题）。
 * 「食物库」走内置热量表（离线可用）；「手动填写」是最终兜底，任何情况都能记上。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    mealType: String,
    date: String,
    onDismiss: () -> Unit,
    onConfirm: (MealEntry) -> Unit,
) {
    var mode by remember { mutableStateOf(0) }          // 0 = 食物库，1 = 手动

    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<FoodItem?>(null) }
    var grams by remember { mutableStateOf("100") }

    var mName by remember { mutableStateOf("") }
    var mKcal by remember { mutableStateOf("") }
    var mCarb by remember { mutableStateOf("") }
    var mPro by remember { mutableStateOf("") }
    var mFat by remember { mutableStateOf("") }

    val results = remember(query) { FoodLib.search(query).take(120) }
    val g = grams.trim().toDoubleOrNull() ?: 0.0
    val preview = picked?.let { FoodLib.calc(it, g) }

    val canSave = if (mode == 0) {
        picked != null && g > 0
    } else {
        mName.isNotBlank() && (mKcal.trim().toDoubleOrNull() ?: 0.0) > 0
    }

    fun save() {
        val entry = if (mode == 0) {
            val item = picked ?: return
            val c = FoodLib.calc(item, g)
            MealEntry(
                id = IdGen.new(), date = date, mealType = mealType,
                foodName = item.name, grams = g,
                calories = c.kcal, carbs = c.carbs, protein = c.protein, fat = c.fat,
                inputMethod = "lib", createdAt = DateFmt.nowTime(),
            )
        } else {
            MealEntry(
                id = IdGen.new(), date = date, mealType = mealType,
                foodName = mName.trim(), grams = grams.trim().toDoubleOrNull() ?: 0.0,
                calories = mKcal.trim().toDoubleOrNull() ?: 0.0,
                carbs = mCarb.trim().toDoubleOrNull() ?: 0.0,
                protein = mPro.trim().toDoubleOrNull() ?: 0.0,
                fat = mFat.trim().toDoubleOrNull() ?: 0.0,
                inputMethod = "manual", createdAt = DateFmt.nowTime(),
            )
        }
        onConfirm(entry)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("记录$mealType", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HChip("食物库", mode == 0) { mode = 0 }
                HChip("手动填写", mode == 1) { mode = 1 }
            }
            Spacer(Modifier.height(14.dp))

            if (mode == 0) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("搜食物，如「米饭」", color = Muted) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(results, key = { it.name }) { item ->
                        val sel = picked?.name == item.name
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { picked = item }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontSize = 13.sp, color = Ink)
                                Text(
                                    "${item.cat} · ${fmt0(item.kcal)} kcal / 100g",
                                    fontSize = 11.sp, color = Muted,
                                )
                            }
                            if (sel) Text("✓ 已选", fontSize = 11.sp, color = Primary)
                        }
                        HorizontalDivider(color = Line)
                    }
                }

                if (picked != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            picked!!.name,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = Ink, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = grams,
                            onValueChange = { grams = numFilter(it) },
                            singleLine = true,
                            suffix = { Text("g", color = Muted) },
                            modifier = Modifier.width(108.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FoodLib.PORTIONS.forEach { p ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Bg)
                                    .clickable { grams = p.toString() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) { Text("${p}g", fontSize = 11.sp, color = InkSoft) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (preview != null && g > 0) {
                        Text(
                            "≈ ${fmt0(preview.kcal)} kcal · 碳 ${fmt1(preview.carbs)}g" +
                                    " · 蛋 ${fmt1(preview.protein)}g · 脂 ${fmt1(preview.fat)}g",
                            fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium,
                        )
                    } else {
                        Text("请填写克数", fontSize = 12.sp, color = Muted)
                    }
                }
            } else {
                OutlinedTextField(
                    value = mName,
                    onValueChange = { mName = it },
                    singleLine = true,
                    label = { Text("食物名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mKcal, onValueChange = { mKcal = numFilter(it) },
                        singleLine = true, label = { Text("热量") },
                        suffix = { Text("kcal", color = Muted) },
                        modifier = Modifier.weight(1.2f),
                    )
                    OutlinedTextField(
                        value = grams, onValueChange = { grams = numFilter(it) },
                        singleLine = true, label = { Text("份量") },
                        suffix = { Text("g", color = Muted) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mCarb, onValueChange = { mCarb = numFilter(it) },
                        singleLine = true, label = { Text("碳水") },
                        suffix = { Text("g", color = Muted) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = mPro, onValueChange = { mPro = numFilter(it) },
                        singleLine = true, label = { Text("蛋白") },
                        suffix = { Text("g", color = Muted) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = mFat, onValueChange = { mFat = numFilter(it) },
                        singleLine = true, label = { Text("脂肪") },
                        suffix = { Text("g", color = Muted) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("三大营养素可以不填，只记热量也能用", fontSize = 11.sp, color = Muted)
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("取消", fontSize = 14.sp, color = Muted) }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) Primary else Line)
                        .clickable(enabled = canSave) { save() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "保存到$mealType",
                        fontSize = 14.sp,
                        color = if (canSave) Color.White else Muted,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** 数字输入过滤：只允许数字和小数点，且长度受限 */
private fun numFilter(s: String): String =
    if (s.length <= 7 && s.all { it.isDigit() || it == '.' }) s else s.dropLast(1)

/** 日期加减（YYYY-MM-DD） */
private fun shiftDate(date: String, delta: Int): String = try {
    val p = date.split("-")
    val c = java.util.Calendar.getInstance()
    c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
    c.add(java.util.Calendar.DAY_OF_MONTH, delta)
    DateFmt.ymd(c)
} catch (_: Exception) {
    date
}
