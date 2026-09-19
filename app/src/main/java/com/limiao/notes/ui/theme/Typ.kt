package com.limiao.notes.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字型令牌。
 *
 * **不引入任何字体文件** —— 层级全靠字重 / 字号 / 字距做出来：
 *   · 小标签：11sp + SemiBold + 正字距 1.2sp（大写后像杂志页眉）
 *   · 大数字：粗体 + 负字距（收紧才显得「重」）
 *
 * ⚠️ 大数字三档（hero / big / mid）是全站唯一需要调"大小观感"的地方，
 *    想整体放大或缩小，只改下面这三个 fontSize 即可，不用动页面。
 */
object Typ {
    /** 超大数字：卡片主数值（当前体重等）—— 一屏最多出现 1 个 */
    val hero = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp)

    /** 大号数字：圆环内、深色横幅里的数值 */
    val big = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)

    /** 中号数字：卡片里与其它元素并排的次级数值 */
    val mid = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)

    /** 页面大标题 */
    val pageTitle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)

    /** 区块标题 */
    val section = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

    /** 正文 */
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp)

    /** 次要正文 */
    val bodySm = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)

    /** 辅助说明 */
    val caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)

    /** 大写小标签 */
    val label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}
