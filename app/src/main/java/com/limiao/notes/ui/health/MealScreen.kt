package com.limiao.notes.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.AccentSoft
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.CardBg
import com.limiao.notes.ui.Danger
import com.limiao.notes.ui.Success
import com.limiao.notes.ui.SuccessSoft
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.Water
import com.limiao.notes.ui.WaterSoft
import com.limiao.notes.ui.numFilter
import com.limiao.notes.ui.components.BigNumber
import com.limiao.notes.ui.components.FoodRow
import com.limiao.notes.ui.components.GhostButton
import com.limiao.notes.ui.components.LTitleTopBar
import com.limiao.notes.ui.components.ListRow
import com.limiao.notes.ui.components.MacroRow
import com.limiao.notes.ui.components.MacroSpec
import com.limiao.notes.ui.components.PillButton
import com.limiao.notes.ui.components.RingProgress
import com.limiao.notes.ui.components.SectionLabel
import com.limiao.notes.ui.components.SegRow
import com.limiao.notes.ui.components.SheetActionRow
import com.limiao.notes.ui.components.SheetDivider
import com.limiao.notes.ui.components.SheetShell
import com.limiao.notes.ui.components.SkinCard
import com.limiao.notes.ui.components.StepButton
import com.limiao.notes.ui.components.ThinBar
import com.limiao.notes.ui.components.ThinDivider
import com.limiao.notes.ui.components.rememberSheetStateExpanded
import com.limiao.notes.ui.theme.Dim
import com.limiao.notes.ui.theme.Typ

/** 每餐次配色（全部取自皮肤令牌，不写死色值） */
@Composable
private fun mealPalette(mealType: String): Pair<Color, Color> = when (mealType) {
    "早餐" -> Accent to AccentSoft
    "午餐" -> Success to SuccessSoft
    "晚餐" -> Water to WaterSoft
    else -> TextSecondary to Bg
}

/**
 * ③ 饮食
 *
 * 录入方式（前三条永远可用，完全不依赖网络）：
 *   1. **一句话记一顿** —— 打字或说话描述这一顿，本地规则解析成多条记录（见 `data/MealParse.kt`）
 *   2. 本地食物库检索（内置热量表，离线可用）
 *   3. 手动填克数与热量（最终兜底，任何情况都能记上）
 *   （DeepSeek 接入后会再加「拍照 → AI 识别」，失败自动退回前三条）
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
    var textRecording by remember { mutableStateOf(false) }

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
        LTitleTopBar("饮食", onBack)

        Column(Modifier.padding(horizontal = Dim.screen)) {

            // ===== 日期切换 =====
            SkinCard(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("‹") { date = shiftDate(date, -1) }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (date == today) "今天" else prettyDate(date),
                            fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                        )
                        if (date != today) {
                            Text(
                                "点这里回到今天",
                                fontSize = 11.sp, color = Accent,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { date = today },
                            )
                        }
                    }
                    StepButton(
                        "›",
                    ) { if (date < today) date = shiftDate(date, 1) }
                }
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 一句话记一顿（打字 / 语音，解析后自动入库）=====
            SkinCard(padding = 0.dp) {
                ListRow(
                    icon = Icons.Filled.EditNote,
                    title = "一句话记一顿",
                    subtitle = "打字或说话，自动拆成记录并算好热量",
                    iconTint = Accent,
                    onClick = { textRecording = true },
                )
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 每日汇总（圆环）=====
            SkinCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("摄入预算")
                    Spacer(Modifier.weight(1f))
                    if (budget > 0) {
                        val badge = if (over) "已超标" else "进行中"
                        val c = if (over) Danger else Success
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(Dim.radiusChip))
                                .background(c.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) { Text(badge, fontSize = 11.sp, color = c) }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (budget <= 0) {
                    Text(
                        "先在「身体数据」里填身高体重，才能算出预算",
                        fontSize = 13.sp, color = TextTertiary,
                    )
                    Spacer(Modifier.height(12.dp))
                    BigNumber(fmt0(intake), unit = "kcal", style = Typ.big)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("饮食摄入")
                            Spacer(Modifier.height(6.dp))
                            BigNumber(fmt0(intake), style = Typ.mid)
                            Spacer(Modifier.height(2.dp))
                            Text("千卡", fontSize = 11.sp, color = TextTertiary)
                        }
                        RingProgress(
                            progress = (intake / budget).toFloat(),
                            diameter = 122.dp,
                            stroke = 10.dp,
                            color = if (over) Danger else Accent,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (over) "已超出" else "还可摄入",
                                    fontSize = 11.sp, color = TextTertiary,
                                )
                                Spacer(Modifier.height(2.dp))
                                BigNumber(
                                    fmt0(Math.abs(left)),
                                    style = Typ.big,
                                    color = if (over) Danger else TextPrimary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text("推荐 ${fmt0(budget)}", fontSize = 11.sp, color = TextTertiary)
                            }
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            SectionLabel(if (over) "超出" else "剩余")
                            Spacer(Modifier.height(6.dp))
                            BigNumber(
                                fmt0(Math.abs(left)),
                                style = Typ.mid,
                                color = if (over) Danger else Success,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("千卡", fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }

                // 营养素
                Spacer(Modifier.height(16.dp))
                ThinDivider()
                Spacer(Modifier.height(14.dp))
                MacroRow(
                    specs = listOf(
                        MacroSpec("碳水", macros.first, targets.carbsG),
                        MacroSpec("蛋白", macros.second, targets.proteinG),
                        MacroSpec("脂肪", macros.third, targets.fatG),
                    ),
                )
            }

            Spacer(Modifier.height(Dim.gapSection))

            // ===== 四餐次 =====
            MealTypes.ALL.forEach { mt ->
                MealSection(
                    mealType = mt,
                    entries = dayMeals.filter { it.mealType == mt }.sortedBy { it.createdAt },
                    budgetInMeal = budget,
                    onAdd = { addingTo = mt },
                    onDelete = { deleting = it },
                )
                Spacer(Modifier.height(Dim.gapSection))
            }
        }
    }

    // ===== 一句话记一顿（打字 / 语音）=====
    if (textRecording) {
        MealTextSheet(
            viewedDate = date,
            onDismiss = { textRecording = false },
            onSave = { entries ->
                onSave(data.copy(meals = data.meals + entries))
                // 记完跳到这次记录所在的那天，方便马上核对结果
                entries.firstOrNull()?.let { date = it.date }
                textRecording = false
            },
        )
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
            sheetState = rememberSheetStateExpanded(),
            containerColor = CardBg,
            dragHandle = null,
        ) {
            SheetShell(
                title = e.foodName,
                onClose = { deleting = null },
            ) {
                Text(
                    "${fmt0(e.calories)} kcal" +
                            (if (e.grams > 0) " · ${fmt0(e.grams)} g" else ""),
                    fontSize = 13.sp, color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(10.dp))
                SheetDivider()
                SheetActionRow("删除这条记录", danger = true) {
                    onSave(data.copy(meals = data.meals.filter { it.id != e.id }))
                    deleting = null
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ==================== 局部组件 ====================


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

    SkinCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(Dim.radiusInner))
                    .background(chipBg)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) { Text(mealType, fontSize = 12.sp, color = tint, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.width(10.dp))
            Text(
                if (entries.isEmpty()) "还没记录" else "${fmt0(total)} kcal",
                fontSize = 12.sp, color = TextTertiary,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(Dim.radiusChip))
                    .background(AccentSoft)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) { Text("＋ 添加", fontSize = 12.sp, color = Accent) }
        }

        if (entries.isNotEmpty()) {
            entries.forEach { e ->
                Spacer(Modifier.height(10.dp))
                ThinDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDelete(e) }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.foodName, fontSize = 14.sp, color = TextPrimary)
                        val sub = buildString {
                            if (e.grams > 0) append("${fmt0(e.grams)} g")
                            if (e.carbs > 0 || e.protein > 0 || e.fat > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("碳${fmt0(e.carbs)} 蛋${fmt0(e.protein)} 脂${fmt0(e.fat)}")
                            }
                        }
                        if (sub.isNotEmpty()) {
                            Text(
                                sub, fontSize = 11.sp, color = TextTertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    Text(
                        "${fmt0(e.calories)} kcal",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary,
                    )
                }
            }
            if (budgetInMeal > 0 && total > budgetInMeal) {
                Spacer(Modifier.height(8.dp))
                Text("这一顿已超过全天预算", fontSize = 11.sp, color = Danger)
            }
        }
    }
}

/**
 * 添加食物弹层。
 *
 * 用底部弹层而不是对话框 —— 里面要放可滚动列表，AlertDialog 的内容区自带
 * verticalScroll，再嵌 LazyColumn 会命中「infinity maximum height」崩溃。
 *
 * **两条录入路径都必须保留**：食物库（离线可用）+ 手动填写（任何情况都能记上）。
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
        sheetState = rememberSheetStateExpanded(),
        containerColor = CardBg,
        dragHandle = null,
    ) {
        // 三段式布局：顶部（切换 + 搜索）/ 中间（列表，自己滚动）/ 底部（操作区，常驻）
        // 弹层固定 0.9 屏高，中间列表用 weight(1f) 吃掉剩余空间 ——
        // 于是「点完食物还得往下滑才能保存」的问题就不存在了。
        SheetShell(
            title = "记录$mealType",
            onClose = onDismiss,
            modifier = Modifier.fillMaxHeight(0.9f),
        ) {
            // 两档录入方式
            SegRow(
                options = listOf("食物库", "手动填写"),
                selectedIndex = mode,
                modifier = Modifier.padding(horizontal = 20.dp),
                onSelect = { mode = it },
            )
            Spacer(Modifier.height(12.dp))

            if (mode == 0) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("搜食物，如「米饭」", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search, null,
                            tint = TextTertiary, modifier = Modifier.size(18.dp),
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Close, "清空",
                                tint = TextTertiary,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { query = "" }
                                    .padding(6.dp),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (results.isEmpty()) "没有匹配的食物" else "${results.size} 个结果",
                    fontSize = 11.sp, color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                // ===== 中间：结果列表（占满剩余高度，自己滚动）=====
                if (results.isEmpty()) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "没找到「$query」\n换个词试试，或切到「手动填写」",
                            fontSize = 13.sp, color = TextTertiary,
                            textAlign = TextAlign.Center, lineHeight = 21.sp,
                        )
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(results, key = { it.name }) { item ->
                            FoodRow(
                                name = item.name,
                                detail = "${item.cat} · ${fmt0(item.kcal)} 千卡 / 100 克",
                                selected = picked?.name == item.name,
                                showDivider = item != results.last(),
                                onClick = { picked = item },
                            )
                        }
                    }
                }

            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
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
                            suffix = { Text("kcal", color = TextTertiary) },
                            modifier = Modifier.weight(1.2f),
                        )
                        OutlinedTextField(
                            value = grams, onValueChange = { grams = numFilter(it) },
                            singleLine = true, label = { Text("份量") },
                            suffix = { Text("g", color = TextTertiary) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mCarb, onValueChange = { mCarb = numFilter(it) },
                            singleLine = true, label = { Text("碳水") },
                            suffix = { Text("g", color = TextTertiary) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = mPro, onValueChange = { mPro = numFilter(it) },
                            singleLine = true, label = { Text("蛋白") },
                            suffix = { Text("g", color = TextTertiary) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = mFat, onValueChange = { mFat = numFilter(it) },
                            singleLine = true, label = { Text("脂肪") },
                            suffix = { Text("g", color = TextTertiary) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("三大营养素可以不填，只记热量也能用", fontSize = 11.sp, color = TextTertiary)
                }
            }

            // ===== 底部常驻操作区（永远在屏幕里，不用往下滑）=====
            Spacer(Modifier.height(10.dp))
            ThinDivider()
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {

                if (mode == 0) {
                    val p = picked
                    if (p == null) {
                        Text("从上面选一个食物", fontSize = 12.sp, color = TextTertiary)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    p.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (preview != null && g > 0) {
                                        "≈ ${fmt0(preview.kcal)} 千卡 · 碳 ${fmt1(preview.carbs)} · " +
                                                "蛋 ${fmt1(preview.protein)} · 脂 ${fmt1(preview.fat)} g"
                                    } else {
                                        "请填写克数"
                                    },
                                    fontSize = 11.sp,
                                    color = if (preview != null && g > 0) Accent else TextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedTextField(
                                value = grams,
                                onValueChange = { grams = numFilter(it) },
                                singleLine = true,
                                suffix = { Text("g", color = TextTertiary) },
                                modifier = Modifier.width(104.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FoodLib.PORTIONS.forEach { portion ->
                                val sel = grams == portion.toString()
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(Dim.radiusChip))
                                        .background(if (sel) AccentSoft else Bg)
                                        .clickable { grams = portion.toString() }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${portion}g",
                                        fontSize = 11.sp,
                                        color = if (sel) Accent else TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        GhostButton("取消", onClick = onDismiss)
                    }
                    Box(Modifier.weight(1.5f)) {
                        PillButton(
                            text = "保存到$mealType",
                            enabled = canSave,
                            onClick = { save() },
                        )
                    }
                }
            }
        }
    }
}

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
