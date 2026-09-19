package com.limiao.notes.ui.health

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.FoodItem
import com.limiao.notes.data.FoodLib
import com.limiao.notes.data.IdGen
import com.limiao.notes.data.MealEntry
import com.limiao.notes.data.MealParse
import com.limiao.notes.data.MealTypes
import com.limiao.notes.data.fmt0
import com.limiao.notes.data.prettyDate
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.AccentSoft
import com.limiao.notes.ui.CardBg
import com.limiao.notes.ui.OnAccent
import com.limiao.notes.ui.Success
import com.limiao.notes.ui.TextPrimary
import com.limiao.notes.ui.TextSecondary
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.VoiceRecorder
import com.limiao.notes.ui.Warning
import com.limiao.notes.ui.components.GhostButton
import com.limiao.notes.ui.components.PillButton
import com.limiao.notes.ui.components.SectionLabel
import com.limiao.notes.ui.components.SegRow
import com.limiao.notes.ui.components.SheetShell
import com.limiao.notes.ui.components.ThinDivider
import com.limiao.notes.ui.components.rememberSheetStateExpanded
import com.limiao.notes.ui.numFilter
import com.limiao.notes.ui.theme.Dim
import java.util.Calendar

private val EXAMPLES = listOf(
    "今天中午吃了一碗米饭和一个水煮鸡腿",
    "早上吃了两个鸡蛋和一杯牛奶",
    "昨天晚上吃了半盘红烧肉",
    "8月25号午饭吃了碗面条",
)

/** 预览里的一条，可改克数 / 补热量 / 删除 */
private class FoodDraft(
    val name: String,
    val item: FoodItem?,
    gramsInit: String,
    kcalInit: String,
) {
    var grams by mutableStateOf(gramsInit)
    var kcal by mutableStateOf(kcalInit)

    val gramsValue: Double get() = grams.trim().toDoubleOrNull() ?: 0.0

    /** 匹配到库的按克数实时算；库里没有的就用用户手填的热量 */
    val kcalValue: Double
        get() = item?.let { FoodLib.calc(it, gramsValue).kcal } ?: (kcal.trim().toDoubleOrNull() ?: 0.0)

    /** 数据齐了才算能入库 */
    val ok: Boolean get() = gramsValue > 0 && kcalValue > 0
}

private fun FoodDraft.toEntry(date: String, mealType: String): MealEntry {
    val g = gramsValue
    val c = item?.let { FoodLib.calc(it, g) }
    return MealEntry(
        id = IdGen.new(),
        date = date,
        mealType = mealType,
        foodName = name,
        grams = g,
        calories = kcalValue,
        carbs = c?.carbs ?: 0.0,
        protein = c?.protein ?: 0.0,
        fat = c?.fat ?: 0.0,
        inputMethod = "text",
        createdAt = DateFmt.nowTime(),
    )
}

/**
 * 「一句话记一顿」—— 打字或说话描述这一顿，自动拆成多条饮食记录。
 *
 * 与记账页的「文字记账」同一套思路：**解析只是加速器**，解析结果先摆在预览里，
 * 每条都能改克数、补热量、删除，确认后才入库。库里没有的食物不会丢，改成手填热量即可。
 *
 * 关于语音：走系统 SpeechRecognizer。国产 ROM 常见没有注册识别服务的应用 →
 * 此时按钮会提示改用键盘自带的麦克风键（那是输入法能力，任何手机都能用）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTextSheet(
    viewedDate: String,
    onDismiss: () -> Unit,
    onSave: (List<MealEntry>) -> Unit,
) {
    val context = LocalContext.current
    val nowHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    var raw by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(viewedDate) }
    var mealType by remember { mutableStateOf(MealTypes.suggestByHour(nowHour)) }
    val drafts = remember { mutableStateListOf<FoodDraft>() }
    var listening by remember { mutableStateOf(false) }

    // 边打边解析：文字一变就重算（解析是纯正则，很快）
    LaunchedEffect(raw) {
        if (raw.isBlank()) {
            drafts.clear()
            return@LaunchedEffect
        }
        val r = MealParse.parse(
            raw = raw,
            today = DateFmt.today(),
            nowHour = nowHour,
            defaultDate = viewedDate,
        )
        date = r.date
        mealType = r.mealType
        drafts.clear()
        drafts.addAll(
            r.foods.map { f ->
                FoodDraft(
                    name = f.name,
                    item = f.item,
                    gramsInit = fmt0(f.grams),
                    kcalInit = f.item?.let { fmt0(FoodLib.calc(it, f.grams).kcal) } ?: "",
                )
            }
        )
    }

    // ---- 语音 ----
    val voiceRecorder = remember(context) {
        VoiceRecorder(
            context = context.applicationContext,
            onText = { text ->
                listening = false
                raw = text
            },
            onError = { msg ->
                listening = false
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
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
            Toast.makeText(context, "需要录音权限才能语音输入", Toast.LENGTH_SHORT).show()
        }
    }
    fun startVoice() {
        if (!voiceRecorder.available()) {
            // 国产 ROM 常见：系统里没有注册识别服务的应用。这时最靠谱的是用输入法自带的语音键
            Toast.makeText(
                context,
                "系统没有语音识别服务。点下面的输入框，用键盘上的麦克风键也能语音输入",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            listening = true
            voiceRecorder.start()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val ready = drafts.filter { it.ok }
    val unmatched = drafts.count { it.item == null }
    val totalKcal = ready.sumOf { it.kcalValue }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberSheetStateExpanded(),
        containerColor = CardBg,
        dragHandle = null,
    ) {
        SheetShell(
            title = "一句话记一顿",
            onClose = onDismiss,
            modifier = Modifier.fillMaxHeight(0.9f),
        ) {
            // ===== 顶部：输入 + 语音 =====
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "打字或说话，把这一顿描述出来，自动拆成记录。",
                    fontSize = 12.sp, color = TextTertiary,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = {
                            Text("如：今天中午吃了一碗米饭和一个水煮鸡腿", color = TextTertiary)
                        },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    MicButton(listening = listening) {
                        if (listening) {
                            voiceRecorder.cancel()
                            listening = false
                        } else {
                            startVoice()
                        }
                    }
                }
                if (listening) {
                    Spacer(Modifier.height(6.dp))
                    Text("正在听…请说「今天中午吃了一碗米饭和一个鸡腿」", fontSize = 12.sp, color = Accent)
                }
            }

            // ===== 中间 =====
            if (drafts.isEmpty()) {
                // 还没输入：示例从顶部正常排下来
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(14.dp))
                    Text("试试这么说：", fontSize = 12.sp, color = TextTertiary)
                    Spacer(Modifier.height(8.dp))
                    EXAMPLES.forEach { ex ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(Dim.radiusInner))
                                .background(AccentSoft)
                                .clickable { raw = ex }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(ex, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "点一下示例就会自动填进去。\n日期和餐次会从话里自动识别；没说日期就记到当前这天。",
                        fontSize = 11.sp, color = TextTertiary, lineHeight = 18.sp,
                    )
                }
            } else {
                // 已解析出结果：可改克数 / 补热量 / 删条目，自己滚动
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(14.dp))

                    // 记到哪天 + 哪个餐次
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("记到")
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${prettyDate(date)} · $mealType",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    SegRow(
                        options = MealTypes.ALL,
                        selectedIndex = MealTypes.ALL.indexOf(mealType).coerceAtLeast(0),
                        onSelect = { mealType = MealTypes.ALL[it] },
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionLabel("解析出 ${drafts.size} 条")
                    Spacer(Modifier.height(6.dp))

                    drafts.forEachIndexed { i, d ->
                        if (i > 0) ThinDivider()
                        DraftRow(
                            draft = d,
                            onRemove = { drafts.remove(d) },
                        )
                    }

                    if (unmatched > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "有 $unmatched 条在本地食物库里没找到（名字是橙色），请手动填一下热量。",
                            fontSize = 11.sp, color = Warning, lineHeight = 18.sp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "份量是按常见份量估的（一碗米饭≈200g、一个鸡蛋≈50g），不对就直接改。",
                        fontSize = 11.sp, color = TextTertiary, lineHeight = 18.sp,
                    )
                }
            }

            // ===== 底部常驻操作区 =====
            Spacer(Modifier.height(10.dp))
            ThinDivider()
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                if (drafts.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "共 ${ready.size} 条",
                            fontSize = 12.sp, color = TextTertiary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "约 ${fmt0(totalKcal)} 千卡",
                            fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Accent,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        GhostButton("取消", onClick = onDismiss)
                    }
                    Box(Modifier.weight(1.6f)) {
                        PillButton(
                            text = if (ready.isEmpty()) "记上这一顿" else "记上 ${ready.size} 条",
                            enabled = ready.isNotEmpty(),
                            onClick = {
                                if (ready.isNotEmpty()) {
                                    onSave(ready.map { it.toEntry(date, mealType) })
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 圆形麦克风按钮：待机浅橙底橙图标，聆听中橙底白图标 */
@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (listening) Accent else AccentSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Mic,
            if (listening) "停止" else "语音输入",
            tint = if (listening) OnAccent else Accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 预览里的一条：名称 + 克数（+ 未匹配时的热量输入） */
@Composable
private fun DraftRow(draft: FoodDraft, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                draft.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (draft.item != null) TextPrimary else Warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Close,
                "删掉这条",
                tint = TextTertiary,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft.grams,
                onValueChange = { draft.grams = numFilter(it) },
                singleLine = true,
                suffix = { Text("g", color = TextTertiary) },
                modifier = Modifier.width(112.dp),
            )
            if (draft.item == null) {
                OutlinedTextField(
                    value = draft.kcal,
                    onValueChange = { draft.kcal = numFilter(it) },
                    singleLine = true,
                    placeholder = { Text("填热量", color = TextTertiary) },
                    suffix = { Text("kcal", color = TextTertiary) },
                    modifier = Modifier.width(132.dp),
                )
            } else {
                Text(
                    "≈ ${fmt0(draft.kcalValue)} 千卡",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (draft.kcalValue > 0) Success else TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
