package com.limiao.notes.data

import org.json.JSONObject

// ==================== 健康模块数据结构 ====================
//
// 设计要点：
// 1. 体重「一天一条」，date 即语义主键（有则覆盖，无则追加），天然去重
// 2. 饮食每条独立 id，一天可多条，按 mealType 分组展示
// 3. 饮水按天累计，一天一条
// 4. 当前体重不单独存，从 weights 最新一条推导（避免两处数据打架）
// 5. 全部纯本地，存 DataStore，与其它模块同一套序列化方式

/** 体重记录（date 为语义主键，一天一条） */
data class WeightRecord(
    val date: String,          // YYYY-MM-DD
    val weight: Double,        // kg
    val createdAt: String,
)

/** 单条饮食记录 */
data class MealEntry(
    val id: String,
    val date: String,          // YYYY-MM-DD
    val mealType: String,      // MealTypes.ALL 之一
    val foodName: String,
    val grams: Double,         // 0 表示未填
    val calories: Double,      // kcal
    val carbs: Double,         // g
    val protein: Double,       // g
    val fat: Double,           // g
    val inputMethod: String,   // "lib" 食物库 / "manual" 手动 / "text" 文字解析 / "photo" 拍照 / "ai" 助手
    val createdAt: String,
)

/** 每日饮水（date 为语义主键，一天一条） */
data class WaterLog(
    val date: String,          // YYYY-MM-DD
    val ml: Int,
    val updatedAt: String,
)

/**
 * 身体数据与目标。
 * 当前体重 / BMI / BMR / TDEE 都是从体重记录与这里**实时推算**的，不落库，避免数据不一致。
 */
data class HealthProfile(
    val heightCm: Int,
    val gender: Int,           // 1=男 2=女
    val birthYear: Int,
    val activityLevel: Int,    // 1=久坐 2=轻度 3=中度 4=重度
    val initialWeight: Double, // 起始体重，用于算"已减重"；0 = 未设，回退用最早一条体重
    val targetWeight: Double,  // 0 = 未设
    val targetDate: String,    // YYYY-MM-DD，"" = 未设
) {
    val hasTarget: Boolean get() = targetWeight > 0

    companion object {
        val DEFAULT = HealthProfile(
            heightCm = 165,
            gender = 2,
            birthYear = 2000,
            activityLevel = 2,
            initialWeight = 0.0,
            targetWeight = 0.0,
            targetDate = "",
        )

        val ACTIVITY_LABELS = listOf("久坐", "轻度", "中度", "重度")
    }
}

object MealTypes {
    val ALL = listOf("早餐", "午餐", "晚餐", "加餐")

    /** 生成时间落在哪个餐次（用于"记当前这顿"的默认值） */
    fun suggestByHour(hour: Int): String = when (hour) {
        in 4..9 -> "早餐"
        in 10..13 -> "午餐"
        in 14..17 -> "加餐"
        in 18..21 -> "晚餐"
        else -> "加餐"
    }
}

// ==================== 序列化 ====================

internal fun WeightRecord.toJson() = JSONObject().apply {
    put("date", date); put("weight", weight); put("createdAt", createdAt)
}

internal fun MealEntry.toJson() = JSONObject().apply {
    put("id", id); put("date", date); put("mealType", mealType)
    put("foodName", foodName); put("grams", grams); put("calories", calories)
    put("carbs", carbs); put("protein", protein); put("fat", fat)
    put("inputMethod", inputMethod); put("createdAt", createdAt)
}

internal fun WaterLog.toJson() = JSONObject().apply {
    put("date", date); put("ml", ml); put("updatedAt", updatedAt)
}

internal fun HealthProfile.toJson() = JSONObject().apply {
    put("heightCm", heightCm); put("gender", gender); put("birthYear", birthYear)
    put("activityLevel", activityLevel); put("initialWeight", initialWeight)
    put("targetWeight", targetWeight); put("targetDate", targetDate)
}

internal fun JSONObject.parseWeightRecord() = WeightRecord(
    date = optString("date", ""),
    weight = optDouble("weight", 0.0),
    createdAt = optString("createdAt", ""),
)

internal fun JSONObject.parseMealEntry() = MealEntry(
    id = optString("id", ""),
    date = optString("date", ""),
    mealType = optString("mealType", "加餐"),
    foodName = optString("foodName", ""),
    grams = optDouble("grams", 0.0),
    calories = optDouble("calories", 0.0),
    carbs = optDouble("carbs", 0.0),
    protein = optDouble("protein", 0.0),
    fat = optDouble("fat", 0.0),
    inputMethod = optString("inputMethod", "manual"),
    createdAt = optString("createdAt", ""),
)

internal fun JSONObject.parseWaterLog() = WaterLog(
    date = optString("date", ""),
    ml = optInt("ml", 0),
    updatedAt = optString("updatedAt", ""),
)

internal fun JSONObject.parseHealthProfile() = HealthProfile(
    heightCm = optInt("heightCm", HealthProfile.DEFAULT.heightCm),
    gender = optInt("gender", HealthProfile.DEFAULT.gender),
    birthYear = optInt("birthYear", HealthProfile.DEFAULT.birthYear),
    activityLevel = optInt("activityLevel", HealthProfile.DEFAULT.activityLevel),
    initialWeight = optDouble("initialWeight", 0.0),
    targetWeight = optDouble("targetWeight", 0.0),
    targetDate = optString("targetDate", ""),
)

// ==================== 健康数据的小工具 ====================

object HealthData {

    /** 最新一条体重记录（按日期倒序找） */
    fun latestWeight(data: AppData): WeightRecord? =
        data.weights.filter { it.weight > 0 }.maxByOrNull { it.date }

    /** 当前体重：优先最新体重记录；没有则回退到身体数据里的初始体重 */
    fun currentWeight(data: AppData): Double =
        latestWeight(data)?.weight ?: data.health.initialWeight

    /** 起始体重：优先身体数据里填的；没填就用历史上最早一条体重记录 */
    fun initialWeight(data: AppData): Double {
        if (data.health.initialWeight > 0) return data.health.initialWeight
        return data.weights.filter { it.weight > 0 }.minByOrNull { it.date }?.weight ?: 0.0
    }

    /** 已减重（正数表示减了） */
    fun lostWeight(data: AppData): Double {
        val init = initialWeight(data)
        val now = currentWeight(data)
        if (init <= 0 || now <= 0) return 0.0
        return init - now
    }

    /** 某天的饮食记录 */
    fun mealsOf(data: AppData, date: String): List<MealEntry> =
        data.meals.filter { it.date == date }

    /** 某天的总摄入 */
    fun caloriesOf(data: AppData, date: String): Double =
        mealsOf(data, date).sumOf { it.calories }

    /** 某天的三大营养素合计 */
    fun macrosOf(data: AppData, date: String): Triple<Double, Double, Double> {
        val list = mealsOf(data, date)
        return Triple(
            list.sumOf { it.carbs },
            list.sumOf { it.protein },
            list.sumOf { it.fat },
        )
    }

    /** 某天饮水（ml） */
    fun waterOf(data: AppData, date: String): Int =
        data.waterLogs.firstOrNull { it.date == date }?.ml ?: 0

    fun setWater(data: AppData, date: String, ml: Int): AppData {
        val clamped = ml.coerceIn(0, 20000)
        val rest = data.waterLogs.filter { it.date != date }
        return data.copy(
            waterLogs = rest + WaterLog(date, clamped, DateFmt.today())
        )
    }

    /** 写体重：同一天已有则覆盖 */
    fun setWeight(data: AppData, date: String, weight: Double): AppData {
        val rest = data.weights.filter { it.date != date }
        return data.copy(weights = rest + WeightRecord(date, weight, DateFmt.today()))
    }

    fun removeWeight(data: AppData, date: String): AppData =
        data.copy(weights = data.weights.filter { it.date != date })

    /** 本周（周一至今天）的达标天数 / 日均摄入 / 减重 */
    data class WeekSummary(
        val onTargetDays: Int,
        val avgCalories: Double,
        val weightChange: Double,
        val dayCount: Int,
    )

    fun weekSummary(data: AppData, budget: Double, weekDates: List<String>): WeekSummary {
        var onTarget = 0
        var sum = 0.0
        var counted = 0
        weekDates.forEach { d ->
            val kcal = caloriesOf(data, d)
            if (kcal > 0) {
                sum += kcal
                counted++
                if (budget > 0 && kcal <= budget * 1.05) onTarget++
            }
        }
        val first = weekDates.firstOrNull()?.let { d ->
            data.weights.filter { it.date <= d }.maxByOrNull { it.date }
        }
        val last = weekDates.lastOrNull()?.let { d ->
            data.weights.filter { it.date <= d }.maxByOrNull { it.date }
        }
        val change = if (first != null && last != null) first.weight - last.weight else 0.0
        return WeekSummary(
            onTargetDays = onTarget,
            avgCalories = if (counted > 0) sum / counted else 0.0,
            weightChange = change,
            dayCount = weekDates.size,
        )
    }

    /** 最近 N 天的日期（含今天），升序 */
    fun recentDates(days: Int): List<String> {
        val out = ArrayList<String>(days)
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.DAY_OF_MONTH, -(days - 1))
        repeat(days) {
            out.add(DateFmt.ymd(c))
            c.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return out
    }

    /** 本周（周一 00:00 起）到今天的日期列表，升序 */
    fun thisWeekDates(): List<String> {
        val c = java.util.Calendar.getInstance()
        val dow = c.get(java.util.Calendar.DAY_OF_WEEK) // 1=周日
        val offset = if (dow == java.util.Calendar.SUNDAY) 6 else dow - 2
        c.add(java.util.Calendar.DAY_OF_MONTH, -offset)
        val out = ArrayList<String>(7)
        repeat(offset + 1) {
            out.add(DateFmt.ymd(c))
            c.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return out
    }
}

/** 日期显示："2026-09-20" → "9月20日 周六" */
internal fun prettyDate(date: String): String {
    val p = date.split("-")
    if (p.size != 3) return date
    val week = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    return try {
        val c = java.util.Calendar.getInstance()
        c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
        "${p[1].toInt()}月${p[2].toInt()}日 ${week[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]}"
    } catch (_: Exception) {
        date
    }
}

/** 数字格式化：去掉多余小数（72.0 → 72，72.35 → 72.4） */
internal fun fmt1(v: Double): String {
    if (v == 0.0) return "0"
    val r = Math.round(v * 10) / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

internal fun fmt0(v: Double): String = Math.round(v).toString()
