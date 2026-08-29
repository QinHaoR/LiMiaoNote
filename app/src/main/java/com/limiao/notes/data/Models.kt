package com.limiao.notes.data

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

// ==================== 数据模型 ====================

data class Note(
    val id: String,
    val content: String,
    val tag: String,
    val pinned: Boolean,
    val createdAt: String,
)

/** 日记（字段预留，暂未开放入口） */
data class Diary(
    val id: String,
    val date: String,          // YYYY-MM-DD
    val content: String,
    val mood: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class Transaction(
    val id: String,
    val amount: Double,
    val category: String,
    val note: String,
    val type: String,          // "expense" | "income"
    val date: String,          // YYYY-MM-DD
    val time: String?,         // HH:mm，可为 null
    val createdAt: String,
)

data class Period(
    val id: String,
    val startDate: String,     // YYYY-MM-DD
    val endDate: String?,      // null = 进行中
    val symptoms: List<String>,
    val note: String?,
    val createdAt: String,
)

data class DayNote(
    val date: String,          // YYYY-MM-DD，一天一条
    val flow: String?,         // 少 / 中 / 多
    val symptoms: List<String>,
    val note: String,
    val updatedAt: String,
)

data class CycleSettings(
    val avgCycleLength: Int,
    val avgPeriodLength: Int,
)

data class AppData(
    val version: Int,
    val notes: List<Note>,
    val diaries: List<Diary>,
    val transactions: List<Transaction>,
    val periods: List<Period>,
    val dayNotes: List<DayNote>,
    val settings: CycleSettings,
) {
    companion object {
        fun empty() = AppData(
            version = 1,
            notes = emptyList(),
            diaries = emptyList(),
            transactions = emptyList(),
            periods = emptyList(),
            dayNotes = emptyList(),
            settings = CycleSettings(avgCycleLength = 28, avgPeriodLength = 5),
        )
    }
}

// ==================== 常量 ====================

object Categories {
    val EXPENSE = listOf("餐饮", "出行", "购物", "居住", "娱乐", "医疗", "学习", "其他")
    val INCOME = listOf("工资", "兼职", "红包", "理财", "其他")
}

object QuickNotes {
    val ALL = listOf("外卖", "打车", "超市", "网购", "房租", "话费", "早餐", "午饭", "晚饭", "地铁")
}

object DayPeriods {
    val ALL = listOf(
        "上午" to "09:00",
        "中午" to "12:00",
        "下午" to "15:00",
        "晚上" to "20:00",
    )
}

object FlowLevels {
    val ALL = listOf("少", "中", "多")
}

object PeriodSymptoms {
    val ALL = listOf(
        "痛经", "腰酸", "头痛", "胸胀", "疲惫",
        "情绪波动", "食欲增加", "失眠", "长痘", "腹胀",
    )
}

object Moods {
    val ALL = listOf("😊开心", "😐一般", "😔低落", "😤烦躁", "🤒不舒服")
}

/** 分类配色（文字色 / 底色） */
object CategoryColors {
    data class C(val text: Color, val bg: Color)

    private fun parse(hex: String) = Color(android.graphics.Color.parseColor(hex))

    private val MAP = mapOf(
        "餐饮" to C(parse("#9A3412"), parse("#FFEDD5")),       // 橙
        "出行" to C(parse("#0369A1"), parse("#E0F2FE")),       // 天蓝
        "交通" to C(parse("#0369A1"), parse("#E0F2FE")),
        "购物" to C(parse("#BE185D"), parse("#FCE7F3")),       // 粉
        "居住" to C(parse("#6D28D9"), parse("#EDE9FE")),       // 紫
        "娱乐" to C(parse("#B45309"), parse("#FEF3C7")),       // 琥珀
        "医疗" to C(parse("#047857"), parse("#D1FAE5")),       // 绿
        "学习" to C(parse("#4338CA"), parse("#E0E7FF")),       // 靛蓝
        "其他" to C(parse("#475569"), parse("#F1F5F9")),       // 灰
        "工资" to C(parse("#047857"), parse("#D1FAE5")),
        "兼职" to C(parse("#0F766E"), parse("#CCFBF1")),       // 青
        "红包" to C(parse("#BE123C"), parse("#FFE4E6")),       // 玫红
        "理财" to C(parse("#0E7490"), parse("#CFFAFE")),       // 青蓝
    )

    fun of(category: String): C = MAP[category] ?: C(parse("#475569"), parse("#F1F5F9"))
}

// ==================== JSON 序列化（org.json，零额外依赖） ====================

private fun JSONObject.putOptNullable(key: String, value: String?) {
    if (value != null) put(key, value) else put(key, JSONObject.NULL)
}

fun AppData.toJson(): String {
    val o = JSONObject()
    o.put("version", version)
    o.put("notes", JSONArray().apply { notes.forEach { put(it.toJson()) } })
    o.put("diaries", JSONArray().apply { diaries.forEach { put(it.toJson()) } })
    o.put("transactions", JSONArray().apply { transactions.forEach { put(it.toJson()) } })
    o.put("periods", JSONArray().apply { periods.forEach { put(it.toJson()) } })
    o.put("dayNotes", JSONArray().apply { dayNotes.forEach { put(it.toJson()) } })
    o.put("settings", JSONObject().apply {
        put("avgCycleLength", settings.avgCycleLength)
        put("avgPeriodLength", settings.avgPeriodLength)
    })
    return o.toString()
}

private fun Note.toJson() = JSONObject().apply {
    put("id", id); put("content", content); put("tag", tag); put("pinned", pinned); put("createdAt", createdAt)
}

private fun Diary.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("content", content)
    putOptNullable("mood", mood); put("createdAt", createdAt); put("updatedAt", updatedAt)
}

private fun Transaction.toJson() = JSONObject().apply {
    put("id", id); put("amount", amount); put("category", category); put("note", note)
    put("type", type); put("date", date); putOptNullable("time", time); put("createdAt", createdAt)
}

private fun Period.toJson() = JSONObject().apply {
    put("id", id); put("startDate", startDate); putOptNullable("endDate", endDate)
    put("symptoms", JSONArray(symptoms)); putOptNullable("note", note); put("createdAt", createdAt)
}

private fun DayNote.toJson() = JSONObject().apply {
    put("date", date); putOptNullable("flow", flow)
    put("symptoms", JSONArray(symptoms)); put("note", note); put("updatedAt", updatedAt)
}

fun parseAppData(json: String): AppData {
    return try {
        val o = JSONObject(json)
        fun arr(name: String) = o.optJSONArray(name) ?: JSONArray()
        fun settings(s: JSONObject) = CycleSettings(
            avgCycleLength = s.optInt("avgCycleLength", 28),
            avgPeriodLength = s.optInt("avgPeriodLength", 5),
        )
        AppData(
            version = o.optInt("version", 1),
            notes = arr("notes").asList { it.parseNote() },
            diaries = arr("diaries").asList { it.parseDiary() },
            transactions = arr("transactions").asList { it.parseTransaction() },
            periods = arr("periods").asList { it.parsePeriod() },
            dayNotes = arr("dayNotes").asList { it.parseDayNote() },
            settings = o.optJSONObject("settings")?.let { settings(it) }
                ?: CycleSettings(28, 5),
        )
    } catch (e: Exception) {
        AppData.empty()
    }
}

private inline fun <T> JSONArray.asList(parse: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        out.add(parse(o))
    }
    return out
}

private fun JSONObject.optStr(key: String): String? =
    if (isNull(key)) null else optString(key)

private fun JSONObject.optStrArray(key: String): List<String> {
    val a = optJSONArray(key) ?: return emptyList()
    val out = ArrayList<String>(a.length())
    for (i in 0 until a.length()) out.add(a.optString(i))
    return out
}

private fun JSONObject.parseNote() = Note(
    id = optString("id", ""), content = optString("content", ""),
    tag = optString("tag", ""), pinned = optBoolean("pinned", false),
    createdAt = optString("createdAt", ""),
)

private fun JSONObject.parseDiary() = Diary(
    id = optString("id", ""), date = optString("date", ""), content = optString("content", ""),
    mood = optStr("mood"), createdAt = optString("createdAt", ""), updatedAt = optString("updatedAt", ""),
)

private fun JSONObject.parseTransaction() = Transaction(
    id = optString("id", ""), amount = optDouble("amount", 0.0),
    category = optString("category", ""), note = optString("note", ""),
    type = optString("type", "expense"), date = optString("date", ""),
    time = optStr("time"), createdAt = optString("createdAt", ""),
)

private fun JSONObject.parsePeriod() = Period(
    id = optString("id", ""), startDate = optString("startDate", ""),
    endDate = optStr("endDate"), symptoms = optStrArray("symptoms"),
    note = optStr("note"), createdAt = optString("createdAt", ""),
)

private fun JSONObject.parseDayNote() = DayNote(
    date = optString("date", ""), flow = optStr("flow"),
    symptoms = optStrArray("symptoms"), note = optString("note", ""),
    updatedAt = optString("updatedAt", ""),
)

// ==================== 工具 ====================

object IdGen {
    fun new(): String =
        System.currentTimeMillis().toString(36) +
                (100000..999999).random().toString(36)
}

object DateFmt {
    fun today(): String {
        val d = java.util.Calendar.getInstance()
        return ymd(d)
    }
    fun ymd(c: java.util.Calendar): String {
        val p = { n: Int -> n.toString().padStart(2, '0') }
        return "${c.get(java.util.Calendar.YEAR)}-${p(c.get(java.util.Calendar.MONTH) + 1)}-${p(c.get(java.util.Calendar.DAY_OF_MONTH))}"
    }
    fun nowTime(): String {
        val d = java.util.Calendar.getInstance()
        val p = { n: Int -> n.toString().padStart(2, '0') }
        return "${p(d.get(java.util.Calendar.HOUR_OF_DAY))}:${p(d.get(java.util.Calendar.MINUTE))}"
    }
}
