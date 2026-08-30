package com.limiao.notes.data

import java.util.Calendar

/** 语音识别文字的规则解析结果 */
data class VoiceParsed(
    val text: String,        // 识别原文
    val amount: Double,      // 0 = 没解析出金额
    val category: String,
    val note: String,
    val type: String,        // "expense" | "income"
    val date: String,        // YYYY-MM-DD
    val time: String?,       // HH:mm，可为 null
)

/**
 * 零依赖规则解析：把「语音转出来的文字」用关键词 + 正则拆出账目字段。
 * 免费测试版：只覆盖单笔 + 常见句式，识别不到就交给用户手动补。
 */
object VoiceParse {

    fun parse(raw: String, today: String = DateFmt.today()): VoiceParsed {
        val text = raw.trim()
        val amount = parseAmount(text)
        val type = if (isIncome(text)) "income" else "expense"
        val category = mapCategory(type, text)
        val date = parseDate(text, today)
        val time = parseTime(text)
        val note = buildNote(text)
        return VoiceParsed(text, amount, category, note, type, date, time)
    }

    // ---------------- 支出 / 收入 ----------------

    private fun isIncome(raw: String): Boolean {
        val keys = listOf(
            "工资", "发钱", "发工资", "兼职", "红包", "压岁钱",
            "理财", "利息", "收益", "股票", "基金", "中奖", "退款",
            // 注意避开「外卖」：只用带动作的"卖"
            "卖了", "卖出", "卖掉", "卖了个", "卖钱", "卖二手",
        )
        return keys.any { raw.contains(it) }
    }

    // ---------------- 金额 ----------------

    private fun parseAmount(raw: String): Double {
        // 数字 + 货币单位："15块" "15块钱" "15元" "15圆"
        Regex("(\\d+(?:\\.\\d+)?)\\s*(?:块钱|块|元|圆)").find(raw)?.let {
            return it.groupValues[1].toDouble()
        }
        // "花了 15" / "花15"
        Regex("花了?\\s*(\\d+(?:\\.\\d+)?)").find(raw)?.let {
            return it.groupValues[1].toDouble()
        }
        // 兜底：先剔除时间/日期上下文里的数字（"8点""12月""31日""2026年"），
        // 剩下的数字取唯一或最大值 —— 覆盖"夜宵30""房租1500"这类没带单位的说法
        var s = raw
        s = s.replace(Regex("\\d{1,2}\\s*[点时:]\\s*\\d{0,2}"), " ")
        s = s.replace(Regex("\\d{1,2}\\s*[月日号]"), " ")
        s = s.replace(Regex("\\d{4}\\s*年"), " ")
        val nums = Regex("\\d+(?:\\.\\d+)?").findAll(s).map { it.value.toDouble() }.toList()
        if (nums.isNotEmpty() && nums.all { it > 0 }) return nums.max()
        return 0.0
    }

    // ---------------- 日期 ----------------

    private fun parseDate(raw: String, today: String): String = when {
        raw.contains("前天") -> shiftDate(today, -2)
        raw.contains("昨天") -> shiftDate(today, -1)
        else -> today
    }

    private fun shiftDate(today: String, days: Int): String {
        val p = today.split("-").map { it.toInt() }
        val c = Calendar.getInstance()
        c.set(p[0], p[1] - 1, p[2] + days) // Calendar 自动处理跨月/跨年
        return DateFmt.ymd(c)
    }

    // ---------------- 时间（优先级：显式 X点 > 餐词 > 时段词 > null） ----------------

    private fun parseTime(raw: String): String? {
        // 显式时间："8点" "8:30" "晚上8点" "晚上8点半"（负向前瞻排除"12月""3号"这类日期数字）
        Regex("(\\d{1,2})\\s*[点时:]\\s*(\\d{1,2})?(\\s*半)?(?!\\s*[月号日])").find(raw)?.let { m ->
            val h = m.groupValues[1].toInt()
            var mi = m.groupValues[2].ifBlank { "0" }.toInt()
            if (m.groupValues[3].isNotBlank() && m.groupValues[2].isBlank()) mi = 30
            if (h <= 24 && mi < 60) {
                var hour = if ((raw.contains("下午") || raw.contains("晚上") ||
                            raw.contains("傍晚") || raw.contains("夜里")) && h <= 12) h + 12 else h
                if (hour == 24) hour = 0
                if (hour in 0..23) {
                    return "${hour.toString().padStart(2, '0')}:${mi.toString().padStart(2, '0')}"
                }
            }
        }
        // 餐词（比时段词更具体）
        for ((kw, t) in listOf(
            "早餐" to "08:00", "早饭" to "08:00",
            "午餐" to "12:00", "午饭" to "12:00", "中饭" to "12:00",
            "晚餐" to "19:00", "晚饭" to "19:00", "夜宵" to "22:00",
        )) if (raw.contains(kw)) return t
        // 时段词
        for ((kw, t) in listOf(
            "凌晨" to "02:00", "早上" to "08:00", "上午" to "09:00",
            "中午" to "12:00", "下午" to "15:00", "晚上" to "20:00",
        )) if (raw.contains(kw)) return t
        return null
    }

    // ---------------- 类别 ----------------

    private fun mapCategory(type: String, raw: String): String {
        if (type == "income") {
            return when {
                raw.contains("兼职") -> "兼职"
                raw.contains("红包") || raw.contains("压岁钱") -> "红包"
                raw.contains("理财") || raw.contains("利息") || raw.contains("收益") ||
                    raw.contains("股票") || raw.contains("基金") -> "理财"
                raw.contains("工资") || raw.contains("发钱") ||
                    raw.contains("卖了") || raw.contains("卖出") || raw.contains("卖掉") || raw.contains("二手") -> "工资"
                else -> "其他"
            }
        }
        // 越具体的词放越前面；「买」这类泛词放最后，避免"买了奶茶"被误判成购物
        val map = listOf(
            "餐饮" to listOf(
                "吃", "早餐", "早饭", "午餐", "午饭", "晚餐", "晚饭", "夜宵", "外卖",
                "面", "饭", "粉", "奶茶", "咖啡", "饮料", "火锅", "烧烤", "食堂",
                "零食", "水果", "包子", "菜", "餐馆", "饭店", "蛋糕", "面包", "煎饼", "馄饨", "豆腐脑",
            ),
            "出行" to listOf(
                "地铁", "公交", "打车", "滴滴", "出租", "高铁", "火车",
                "机票", "飞机", "加油", "停车", "车票", "骑车", "单车", "共享单车",
            ),
            "居住" to listOf("房租", "电费", "水费", "燃气", "物业", "宽带"),
            "医疗" to listOf("医院", "药", "看病", "挂号", "体检", "诊所"),
            "娱乐" to listOf("电影", "游戏", "充值", "会员", "KTV", "门票", "旅游", "演唱会", "视频会员"),
            "学习" to listOf("书", "课程", "培训", "报名", "网课", "文具"),
            "购物" to listOf("买", "淘宝", "京东", "网购", "超市", "便利店", "衣服", "鞋", "手机壳", "日用品", "商场"),
        )
        for ((cat, kws) in map) {
            if (kws.any { raw.contains(it) }) return cat
        }
        return "其他"
    }

    // ---------------- 备注（去掉金额、日期词后剩下的原文） ----------------

    private fun buildNote(raw: String): String {
        var s = raw
        s = s.replace(Regex("花了?\\s*\\d+(?:\\.\\d+)?\\s*(?:块钱|块|元|圆)"), "")
        s = s.replace(Regex("\\d+(?:\\.\\d+)?\\s*(?:块钱|块|元|圆)"), "")
        s = s.replace(Regex("今天|昨天|前天|刚才|现在"), "")
        s = s.trim(' ', '，', ',', '。', '.', '！', '?', '？')
        return s
    }
}
