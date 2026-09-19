package com.limiao.notes.data

import java.util.Calendar

/** 一句话里拆出来的一个食物条目 */
data class ParsedFood(
    val raw: String,        // 原文片段，如「一碗米饭」
    val name: String,       // 展示名：匹配到库就用库里的名字，否则是原文
    val item: FoodItem?,    // null = 库里没找到，需要手动补热量
    val qty: Double,        // 数量，如 1；没写数量按 1 算
    val unit: String,       // 单位，如「碗」；空 = 没写
    val grams: Double,      // 估算克数（可编辑）
)

/** 「一句话记一顿」的解析结果 */
data class MealParsed(
    val text: String,
    val date: String,          // YYYY-MM-DD
    val mealType: String,      // MealTypes.ALL 之一
    val foods: List<ParsedFood>,
) {
    /** 匹配到本地食物库的条数 */
    val matchedCount: Int get() = foods.count { it.item != null }
    /** 按估算克数算出的总热量（未匹配的条目按 0 计入） */
    val totalKcal: Double get() = foods.sumOf { f ->
        f.item?.let { FoodLib.calc(it, f.grams).kcal } ?: 0.0
    }
}

/**
 * 饮食语句解析：把「今天中午吃了一碗米饭和一个水煮鸡腿」这类口语拆成结构化条目。
 *
 * 零依赖、纯本地正则 + 内置食物库，**不联网**。
 * 设计原则与记账的 [VoiceParse] 一致：**解析只是加速器，解析不出来交给用户手动补**，
 * 所以每条都允许改克数/热量，未匹配的条目也能直接填热量入库。
 *
 * 覆盖的句式：
 *   · 数量词：「一碗」「两个」「半盘」「1杯」「250克」（支持中文数字，含「两」「半」）
 *   · 多食物：「和 / 跟 / 与 / 还有 / 以及 / 、 / ，」等分隔
 *   · 餐次：「早餐 / 午饭 / 中午 / 晚上 / 夜宵」；没写就看句中的钟点，再没有就用当前时间
 *   · 日期：「今天 / 昨天 / 前天 / 8月25号 / 25号」（复用 [VoiceParse] 已验证的日期逻辑）
 */
object MealParse {

    // ---------------- 对外入口 ----------------

    fun parse(
        raw: String,
        today: String = DateFmt.today(),
        fallbackMealType: String? = null,
        nowHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        defaultDate: String? = null,
    ): MealParsed {
        val text = raw.trim()
        // 句子里没写日期时，用调用方给的那天（例如饮食页当前正在看的那天），否则就是今天。
        // 写了「昨天/今天/8月25号」这类词时，一律以真实今天为基准换算 —— 与记账页语义一致。
        val base = if (hasDateWord(text)) today else (defaultDate ?: today)
        val date = VoiceParse.parse(text, base).date     // 日期规则复用记账那套，已实测
        val mealType = parseMealType(text, fallbackMealType, nowHour)
        val foods = splitFoods(text).mapNotNull { parseOne(it) }
        return MealParsed(text, date, mealType, foods)
    }

    private val RELATIVE_DATE_WORDS = listOf("今天", "昨天", "前天", "刚才", "刚刚")

    private fun hasDateWord(raw: String): Boolean =
        RELATIVE_DATE_WORDS.any { raw.contains(it) } ||
                DATE_FULL_RE.containsMatchIn(raw) ||
                DATE_YEAR_RE.containsMatchIn(raw) ||
                DATE_PART_RE.containsMatchIn(raw)

    // ---------------- 餐次 ----------------

    private val MEAL_WORDS = listOf(
        "早餐" to "早餐", "早饭" to "早餐", "早晨" to "早餐",
        "午餐" to "午餐", "午饭" to "午餐", "中饭" to "午餐", "晌午" to "午餐",
        "晚餐" to "晚餐", "晚饭" to "晚餐",
        "夜宵" to "加餐", "宵夜" to "加餐", "加餐" to "加餐", "下午茶" to "加餐",
    )

    private val PERIOD_WORDS = listOf(
        "早上" to "早餐", "上午" to "早餐", "中午" to "午餐",
        "下午" to "加餐", "傍晚" to "晚餐", "晚上" to "晚餐", "夜里" to "加餐",
    )

    private fun parseMealType(raw: String, fallback: String?, nowHour: Int): String {
        MEAL_WORDS.forEach { (kw, m) -> if (raw.contains(kw)) return m }
        PERIOD_WORDS.forEach { (kw, m) -> if (raw.contains(kw)) return m }
        // 句中有钟点（"12点吃的"）—— 负向前瞻排除「12月」这类日期
        Regex("(\\d{1,2})\\s*[点时](?!\\s*[月号日])").find(raw)?.let { m ->
            val h = m.groupValues[1].toInt()
            if (h in 0..23) return MealTypes.suggestByHour(h)
        }
        return fallback ?: MealTypes.suggestByHour(nowHour)
    }

    // ---------------- 切分多食物 ----------------

    /** 时间/餐次/口语前缀 —— 切分食物前先剔掉，避免它们被当成食物名的一部分 */
    private val CONTEXT_WORDS = listOf(
        "今天", "昨天", "前天", "刚才", "刚刚", "现在",
        "早餐", "早饭", "午餐", "午饭", "中饭", "晚餐", "晚饭", "夜宵", "宵夜", "加餐", "下午茶",
        "早上", "早晨", "上午", "中午", "晌午", "下午", "傍晚", "晚上", "夜里", "凌晨",
    )

    private val TIME_RE = Regex("\\d{1,2}\\s*[点时:]\\s*\\d{0,2}\\s*分?")

    /** 日期表达式也要剔掉，否则「8月25号吃了碗面」会把「月25号」当成食物名 */
    private val DATE_FULL_RE = Regex("\\d{1,2}\\s*月\\s*\\d{1,2}\\s*[日号]")
    private val DATE_YEAR_RE = Regex("\\d{4}\\s*年")
    private val DATE_PART_RE = Regex("\\d{1,2}\\s*[月日号]")

    /** 句首的口语动词：「我今天中午吃了…」→ 剩下的才是食物 */
    private val VERB_RE = Regex("^[我咱]?(?:又|还|刚|先|才|大概|差不多|就)?(?:吃了?|喝了?|点了?|买了?|来了?|整了?|干了?|消灭了?|炫了?|造了?|用了?|下肚了?)了?")

    private val SEP_RE = Regex("(?:还有|以及|外加|再加上|再加|另加|和|跟|与|、|，|,|\\+|＋|；|;)")

    private fun splitFoods(raw: String): List<String> {
        var s = raw
        CONTEXT_WORDS.forEach { s = s.replace(it, "") }
        s = TIME_RE.replace(s, "")
        s = DATE_FULL_RE.replace(s, "")   // 8月25号
        s = DATE_YEAR_RE.replace(s, "")   // 2026年
        s = DATE_PART_RE.replace(s, "")   // 25号 / 8月
        // 句首动词可能叠加（"又吃了"），最多剥 3 层
        repeat(3) {
            val next = VERB_RE.replace(s, "")
            if (next == s) return@repeat
            s = next
        }
        return s.split(SEP_RE)
            .map { it.trim().trim('的', '了', '，', ',', '。', '.', '！', '？', '!', '?', ' ') }
            .filter { it.isNotEmpty() }
    }

    // ---------------- 单条：数量词 + 单位 + 食物名 ----------------

    /**
     * 「二两肉」这类要先于数量词识别 —— 「两」既是数量 2 又是单位 50g。
     * 这里把「数字 + 两」固定当单位解，避免被数量词分支抢走。
     */
    private val LIANG_RE = Regex("^\\s*(\\d+(?:\\.\\d+)?|[一二三四五六七八九十]{1,2})\\s*两\\s*(.*)$")

    /** 小写缩写见注释；「两」在数量词里是 2，在单位里是 50g —— 靠正则先后顺序区分 */
    private val QTY_UNIT_RE = Regex(
        "^\\s*(?:(\\d+(?:\\.\\d+)?|[一二两三四五六七八九十半]{1,3})\\s*)?" +
                "(千克|公斤|毫升|碗|盘|碟|份|个|只|块|片|根|条|杯|瓶|罐|盒|袋|勺|斤|两" +
                "|克|g|G|ml|串|颗|粒|把|听)?" +
                "\\s*(.*)$"
    )

    /** 通用单位 → 克（食物没有专属份量表时的兜底） */
    private val UNIT_G = mapOf(
        "碗" to 200.0, "盘" to 200.0, "碟" to 150.0, "份" to 150.0,
        "个" to 80.0, "只" to 80.0, "块" to 50.0, "片" to 25.0,
        "根" to 60.0, "条" to 80.0, "杯" to 250.0, "瓶" to 350.0,
        "罐" to 330.0, "盒" to 100.0, "袋" to 80.0, "勺" to 10.0,
        "斤" to 500.0, "两" to 50.0, "千克" to 1000.0, "公斤" to 1000.0,
        "克" to 1.0, "g" to 1.0, "G" to 1.0, "ml" to 1.0, "毫升" to 1.0,
        "串" to 60.0, "颗" to 15.0, "粒" to 10.0, "把" to 30.0, "听" to 330.0,
    )

    /** 常见食物的「一个/一碗」大约多重 —— 有专属值就用它，比通用表准得多 */
    private val SERVING: Map<String, Map<String, Double>> = mapOf(
        "米饭（熟）" to mapOf("碗" to 200.0, "份" to 150.0, "个" to 80.0),
        "白粥" to mapOf("碗" to 250.0),
        "面条（煮熟）" to mapOf("碗" to 250.0),
        "馒头" to mapOf("个" to 100.0),
        "包子（肉馅）" to mapOf("个" to 100.0),
        "饺子（猪肉）" to mapOf("个" to 20.0),
        "面包" to mapOf("片" to 35.0),
        "鸡蛋" to mapOf("个" to 50.0, "只" to 50.0),
        "鸡腿" to mapOf("个" to 100.0, "只" to 100.0),
        "鸡胸肉" to mapOf("块" to 100.0, "份" to 100.0),
        "排骨" to mapOf("块" to 50.0),
        "五花肉" to mapOf("块" to 50.0),
        "苹果" to mapOf("个" to 200.0),
        "梨" to mapOf("个" to 200.0),
        "橙子" to mapOf("个" to 150.0),
        "香蕉" to mapOf("根" to 120.0),
        "玉米（煮）" to mapOf("根" to 200.0),
        "红薯" to mapOf("个" to 200.0),
        "黄瓜" to mapOf("根" to 150.0),
        "番茄" to mapOf("个" to 150.0),
        "牛奶" to mapOf("杯" to 250.0, "盒" to 250.0, "袋" to 200.0),
        "酸奶" to mapOf("杯" to 150.0, "盒" to 150.0),
        "可乐" to mapOf("罐" to 330.0, "瓶" to 500.0, "杯" to 330.0),
    )

    /** 口语别名 → 库里的规范名。只收「靠模糊搜索会选错」的那些 */
    private val ALIAS = mapOf(
        "饭" to "米饭（熟）",
        "面" to "面条（煮熟）",
        "西红柿" to "番茄",
        "鸡胸" to "鸡胸肉",
        "白菜" to "大白菜",
        "青菜" to "清炒时蔬",
        "水煮蛋" to "鸡蛋",
        "煮鸡蛋" to "鸡蛋",
        "煎蛋" to "鸡蛋",
        "荷包蛋" to "鸡蛋",
        "纯牛奶" to "牛奶",
        "白饭" to "米饭（熟）",
    )

    private fun parseOne(part: String): ParsedFood? {
        val liang = LIANG_RE.find(part)
        val qtyStr: String
        val unit: String
        var name: String
        if (liang != null) {
            qtyStr = liang.groupValues[1]
            unit = "两"
            name = liang.groupValues[2].trim().trim('的', '了', ' ')
        } else {
            val m = QTY_UNIT_RE.find(part) ?: return null
            qtyStr = m.groupValues[1]
            unit = m.groupValues[2]
            name = m.groupValues[3].trim().trim('的', '了', ' ')
        }

        // 只写了「一碗」没写食物 → 丢掉
        if (name.isEmpty()) return null

        // 把数量词从名字里清掉后剩下的可能还带「大/小」等修饰，交给匹配处理
        val item = matchFood(name, part)
        if (item != null) name = item.name

        val qty = cnNum(qtyStr) ?: 1.0
        val g = estimateGrams(item, unit, qty)

        return ParsedFood(
            raw = part,
            name = name,
            item = item,
            qty = qty,
            unit = unit,
            grams = g,
        )
    }

    /** 先精确、再别名、再把食物库名字当子串找（「水煮鸡腿」→「鸡腿」） */
    private fun matchFood(name: String, part: String): FoodItem? {
        FoodLib.find(name)?.let { return it }
        ALIAS[name]?.let { alias -> FoodLib.find(alias)?.let { return it } }
        // 库名包含关键词：search 已按「完全等于 > 开头 > 包含」+ 名称长度排过序
        FoodLib.search(name).firstOrNull()?.let { hit ->
            if (hit.name.contains(name) || name.contains(hit.name)) return hit
        }
        // 反向：库名是原文的一部分（取最长的那个，避免「鸡」匹配到「鸡腿」这种过度泛化）
        FoodLib.ALL.filter { part.contains(it.name) }
            .maxByOrNull { it.name.length }
            ?.let { return it }
        return FoodLib.ALL.filter { name.contains(it.name) }
            .maxByOrNull { it.name.length }
    }

    private fun estimateGrams(item: FoodItem?, unit: String, qty: Double): Double {
        val serving = item?.let { SERVING[it.name] }
        val perUnit = when {
            unit.isBlank() -> serving?.get("个") ?: serving?.values?.firstOrNull() ?: 100.0
            serving?.containsKey(unit) == true -> serving.getValue(unit)
            UNIT_G.containsKey(unit) -> UNIT_G.getValue(unit)
            else -> 100.0
        }
        return (qty * perUnit).coerceIn(1.0, 5000.0)
    }

    // ---------------- 中文数字 ----------------

    private val CN = mapOf(
        '一' to 1.0, '二' to 2.0, '两' to 2.0, '三' to 3.0, '四' to 4.0, '五' to 5.0,
        '六' to 6.0, '七' to 7.0, '八' to 8.0, '九' to 9.0, '十' to 10.0, '半' to 0.5,
    )

    private fun cnNum(s: String): Double? {
        val t = s.trim()
        if (t.isEmpty()) return null
        t.toDoubleOrNull()?.let { return it }
        if (t == "十") return 10.0
        if (t == "半") return 0.5
        if (t.length == 1) return CN[t[0]]
        if (t.startsWith("十")) return CN[t[1]]?.let { 10.0 + it }          // 十五
        if (t.length == 2 && t[1] == '十') return CN[t[0]]?.let { it * 10 } // 二十
        if (t.length == 3 && t[1] == '十') {                               // 二十五
            val a = CN[t[0]] ?: return null
            val b = CN[t[2]] ?: return null
            return a * 10 + b
        }
        return null
    }
}
