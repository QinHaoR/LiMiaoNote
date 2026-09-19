package com.limiao.notes.data

import kotlin.math.max

/**
 * 热量相关计算 —— 全部是纯公式，零网络、零依赖。
 *
 * 公式来源：Mifflin-St Jeor（目前公认较准的 BMR 估算式）
 *   男：BMR = 10×体重kg + 6.25×身高cm − 5×年龄 + 5
 *   女：BMR = 10×体重kg + 6.25×身高cm − 5×年龄 − 161
 *
 * 每日热量预算 = BMR − 400（约每周减 0.4kg），并设 1200 kcal 为安全底线。
 */
object HealthCalc {

    fun age(birthYear: Int, now: java.util.Calendar = java.util.Calendar.getInstance()): Int {
        val a = now.get(java.util.Calendar.YEAR) - birthYear
        return if (a in 1..120) a else 22
    }

    /** 基础代谢率 */
    fun bmr(weightKg: Double, heightCm: Int, age: Int, gender: Int): Double {
        if (weightKg <= 0 || heightCm <= 0) return 0.0
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return if (gender == 2) base - 161.0 else base + 5.0
    }

    /** 活动系数 */
    fun activityMultiplier(level: Int): Double = when (level) {
        1 -> 1.2
        2 -> 1.375
        3 -> 1.55
        4 -> 1.725
        else -> 1.375
    }

    /** 每日总消耗 */
    fun tdee(bmr: Double, activityLevel: Int): Double = bmr * activityMultiplier(activityLevel)

    /** 每日热量预算（带安全底线） */
    fun dailyBudget(bmr: Double): Double =
        if (bmr <= 0) 0.0 else max(bmr - 400.0, 1200.0)

    fun bmi(weightKg: Double, heightCm: Int): Double {
        if (weightKg <= 0 || heightCm <= 0) return 0.0
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    /** BMI 分级（中国成人标准） */
    fun bmiLabel(bmi: Double): String = when {
        bmi <= 0 -> "—"
        bmi < 18.5 -> "偏瘦"
        bmi < 24.0 -> "正常"
        bmi < 28.0 -> "偏胖"
        else -> "肥胖"
    }

    data class Macros(val carbsG: Double, val proteinG: Double, val fatG: Double)

    /** 营养素目标：碳水 50% / 蛋白质 25% / 脂肪 25%（1g 碳水=4kcal，1g 蛋白=4kcal，1g 脂肪=9kcal） */
    fun macroTargets(budget: Double): Macros = Macros(
        carbsG = budget * 0.50 / 4.0,
        proteinG = budget * 0.25 / 4.0,
        fatG = budget * 0.25 / 9.0,
    )

    /** 距目标的进度（0..1）；未设目标体重返回 -1 */
    fun goalProgress(current: Double, target: Double, initial: Double): Float {
        if (target <= 0 || initial <= 0 || current <= 0) return -1f
        val total = initial - target
        if (total <= 0) return -1f
        val done = initial - current
        return (done / total).coerceIn(0.0, 1.0).toFloat()
    }

    /** 按目标日期估算每天需要多大的热量缺口 */
    fun requiredDeficitPerDay(current: Double, target: Double, targetDate: String): Double {
        if (current <= 0 || target <= 0 || targetDate.isBlank()) return 0.0
        val days = daysBetween(DateFmt.today(), targetDate)
        if (days <= 0) return 0.0
        val needKg = current - target
        if (needKg <= 0) return 0.0
        // 1kg 脂肪 ≈ 7700 kcal
        return needKg * 7700.0 / days
    }

    fun daysBetween(from: String, to: String): Int {
        return try {
            fun parse(s: String): Long {
                val p = s.split("-")
                val c = java.util.Calendar.getInstance()
                c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 0, 0, 0)
                c.set(java.util.Calendar.MILLISECOND, 0)
                return c.timeInMillis
            }
            val ms = parse(to) - parse(from)
            (ms / 86400000L).toInt()
        } catch (_: Exception) {
            0
        }
    }
}
