package com.limiao.notes.data

/**
 * 「一句话记一顿」解析器冒烟测试（纯 JVM，不依赖 Android 框架）
 *
 * 跑法见 `MarkdownParserSmokeTest` 的说明：
 *   env -u CODEBUDDY_SESSION_ID -u CLAUDE_SESSION_ID \
 *     "C:/Users/Public/jdk21/bin/java.exe" -classpath "gradle/wrapper/gradle-wrapper.jar" \
 *     org.gradle.wrapper.GradleWrapperMain :app:compileDebugUnitTestKotlin
 * 然后直接 java 跑本文件的 main（见文件末尾打印的用法）。
 */
fun main() {
    var failed = 0
    fun assert(name: String, cond: Boolean, detail: String = "") {
        val tag = if (cond) "PASS" else "FAIL"
        if (!cond) failed++
        println("[$tag] $name${if (detail.isNotEmpty()) "  ($detail)" else ""}")
    }

    val today = "2026-09-20"

    // ---------- 0) 用户给的原句 ----------
    run {
        val r = MealParse.parse("今天中午吃了一碗米饭和一个水煮鸡腿", today = today, nowHour = 15)
        println("   → date=${r.date} meal=${r.mealType} foods=${r.foods.map { "${it.name}:${it.grams}g" }}")
        assert("原句·日期=今天", r.date == today, r.date)
        assert("原句·餐次=午餐", r.mealType == "午餐", r.mealType)
        assert("原句·拆成 2 条", r.foods.size == 2, "${r.foods.size}")
        assert("原句·第1条=米饭（熟）200g",
            r.foods.getOrNull(0)?.name == "米饭（熟）" && r.foods[0].grams == 200.0,
            "${r.foods.getOrNull(0)?.name}:${r.foods.getOrNull(0)?.grams}")
        assert("原句·第2条=鸡腿 100g",
            r.foods.getOrNull(1)?.name == "鸡腿" && r.foods[1].grams == 100.0,
            "${r.foods.getOrNull(1)?.name}:${r.foods.getOrNull(1)?.grams}")
        assert("原句·热量>0", r.totalKcal > 0, "${r.totalKcal}")
    }

    // ---------- 1) 中文数量词 ----------
    run {
        val r = MealParse.parse("早上吃了两个鸡蛋和一杯牛奶", today = today, nowHour = 15)
        println("   → meal=${r.mealType} foods=${r.foods.map { "${it.name}:${it.grams}g" }}")
        assert("两词·餐次=早餐", r.mealType == "早餐", r.mealType)
        assert("两词·鸡蛋 2×50=100g",
            r.foods.getOrNull(0)?.name == "鸡蛋" && r.foods[0].grams == 100.0,
            "${r.foods.getOrNull(0)?.name}:${r.foods.getOrNull(0)?.grams}")
        assert("两词·牛奶 250g",
            r.foods.getOrNull(1)?.name == "牛奶" && r.foods[1].grams == 250.0,
            "${r.foods.getOrNull(1)?.name}:${r.foods.getOrNull(1)?.grams}")
    }

    // ---------- 2) 半 + 单位 ----------
    run {
        val r = MealParse.parse("昨天晚上吃了半盘红烧肉", today = today, nowHour = 15)
        println("   → date=${r.date} meal=${r.mealType} foods=${r.foods.map { "${it.name}:${it.grams}g" }}")
        assert("半盘·日期=昨天", r.date == "2026-09-19", r.date)
        assert("半盘·餐次=晚餐", r.mealType == "晚餐", r.mealType)
        assert("半盘·红烧肉 0.5×200=100g",
            r.foods.getOrNull(0)?.name == "红烧肉" && r.foods[0].grams == 100.0,
            "${r.foods.getOrNull(0)?.name}:${r.foods.getOrNull(0)?.grams}")
    }

    // ---------- 3) 具体日期 + 省略数量 ----------
    run {
        val r = MealParse.parse("8月25号午饭吃了碗面条", today = today, nowHour = 15)
        println("   → date=${r.date} meal=${r.mealType} foods=${r.foods.map { "${it.name}:${it.grams}g" }}")
        assert("日期·8月25号", r.date == "2026-08-25", r.date)
        assert("日期·餐次=午餐", r.mealType == "午餐", r.mealType)
        assert("日期·面条（煮熟）250g",
            r.foods.getOrNull(0)?.name == "面条（煮熟）" && r.foods[0].grams == 250.0,
            "${r.foods.getOrNull(0)?.name}:${r.foods.getOrNull(0)?.grams}")
    }

    // ---------- 4) 直接给克数 ----------
    run {
        val r = MealParse.parse("刚才吃了250克鸡胸肉", today = today, nowHour = 15)
        println("   → foods=${r.foods.map { "${it.name}:${it.grams}g" }}")
        assert("克数·鸡胸肉 250g",
            r.foods.getOrNull(0)?.name == "鸡胸肉" && r.foods[0].grams == 250.0,
            "${r.foods.getOrNull(0)?.name}:${r.foods.getOrNull(0)?.grams}")
    }

    // ---------- 5) 库里没有的食物 → 标记为待手动填热量 ----------
    run {
        val r = MealParse.parse("夜宵吃了两串烤肉", today = today, nowHour = 15)
        println("   → meal=${r.mealType} foods=${r.foods.map { "${it.name}:${it.grams}g item=${it.item != null}" }}")
        assert("未匹配·餐次=加餐", r.mealType == "加餐", r.mealType)
        assert("未匹配·解析出 1 条", r.foods.size == 1, "${r.foods.size}")
        assert("未匹配·item 为 null", r.foods.getOrNull(0)?.item == null)
        assert("未匹配·名字=烤肉", r.foods.getOrNull(0)?.name == "烤肉", "${r.foods.getOrNull(0)?.name}")
        assert("未匹配·2×60=120g", r.foods.getOrNull(0)?.grams == 120.0, "${r.foods.getOrNull(0)?.grams}")
        assert("未匹配·热量按 0 计", r.totalKcal == 0.0, "${r.totalKcal}")
    }

    // ---------- 6) 省略数量 → 按 1 算 ----------
    run {
        val r = MealParse.parse("吃了个苹果", today = today, nowHour = 15)
        println("   → foods=${r.foods.map { "${it.name}:${it.grams}g" }}")
        assert("省量·苹果 200g",
            r.foods.getOrNull(0)?.name == "苹果" && r.foods[0].grams == 200.0,
            "${r.foods.getOrNull(0)?.name}:${r.foods.getOrNull(0)?.grams}")
    }

    // ---------- 7) 「两」既可能是数量 2，也可能是单位 50g ----------
    run {
        val drink = MealParse.parse("喝了一杯可乐", today = today, nowHour = 15)
        println("   → 可乐=${drink.foods.map { "${it.name}:${it.grams}g" }}")
        assert("单位·可乐 330g",
            drink.foods.getOrNull(0)?.name == "可乐" && drink.foods[0].grams == 330.0,
            "${drink.foods.getOrNull(0)?.grams}")

        val liang = MealParse.parse("二两米饭", today = today, nowHour = 15)
        println("   → 二两米饭=${liang.foods.map { "${it.name}:${it.grams}g unit=${it.unit}" }}")
        assert("单位·二两=2×50=100g",
            liang.foods.getOrNull(0)?.grams == 100.0 && liang.foods[0].unit == "两",
            "${liang.foods.getOrNull(0)?.grams}/${liang.foods.getOrNull(0)?.unit}")
    }

    // ---------- 8) 餐次兜底：句里没有餐次词时按当前钟点 ----------
    run {
        val r = MealParse.parse("吃了一碗米饭", today = today, nowHour = 8)
        println("   → meal=${r.mealType}")
        assert("兜底·8点→早餐", r.mealType == "早餐", r.mealType)

        val r2 = MealParse.parse("吃了一碗米饭", today = today, nowHour = 20)
        assert("兜底·20点→晚餐", r2.mealType == "晚餐", r2.mealType)

        val r3 = MealParse.parse("吃了一碗米饭", today = today, fallbackMealType = "加餐", nowHour = 20)
        assert("兜底·显式指定优先", r3.mealType == "加餐", r3.mealType)
    }

    // ---------- 9) 空输入不炸 ----------
    run {
        val r = MealParse.parse("   ", today = today, nowHour = 15)
        assert("空输入·foods 为空", r.foods.isEmpty(), "${r.foods.size}")
        val r2 = MealParse.parse("今天心情不错", today = today, nowHour = 15)
        println("   → 无食物句 foods=${r2.foods.map { it.name }}")
        assert("无食物句·不抛异常", true)
    }

    // ---------- 10) 句子里没写日期 → 用调用方给的那天 ----------
    run {
        val r = MealParse.parse("吃了一碗米饭", today = today, nowHour = 15, defaultDate = "2026-09-15")
        println("   → date=${r.date}")
        assert("默认日期·无日期词用 defaultDate", r.date == "2026-09-15", r.date)

        val r2 = MealParse.parse("昨天吃了一碗米饭", today = today, nowHour = 15, defaultDate = "2026-09-15")
        println("   → date=${r2.date}")
        assert("默认日期·有日期词则以真实今天为基准", r2.date == "2026-09-19", r2.date)
    }

    println()
    println(if (failed == 0) "==== 全部通过 ====" else "==== 有 $failed 项失败 ====")
}
