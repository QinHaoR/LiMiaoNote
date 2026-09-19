package com.limiao.notes.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 尺寸令牌。全站圆角 / 间距 / 高度只从这里取，避免出现「圆角 16 与默认 12 混用」这种漂移。
 */
object Dim {
    // —— 间距 ——
    val screen = 16.dp        // 页面水平留白
    val cardPad = 16.dp       // 卡片内边距
    val gapSection = 12.dp    // 卡片与卡片之间
    val gapItem = 8.dp        // 卡片内部元素之间

    // —— 圆角 ——
    val radiusCard = 18.dp    // 卡片
    val radiusInner = 12.dp   // 卡内小块
    val radiusTile = 14.dp    // 宫格图标块
    val radiusChip = 100.dp   // 胶囊（全圆）
    val radiusSheet = 24.dp   // 底部弹层顶部圆角

    // —— 高度 ——
    val buttonH = 50.dp       // 满宽药丸按钮
    val inputH = 48.dp        // 输入框 / 按钮行
    val topBar = 48.dp        // 二级页顶栏

    // —— 进度条 ——
    val barThin = 6.dp        // 常规进度条
    val barHair = 3.dp        // 极细进度条（营养素）

    // —— 圆环 ——
    val ring = 132.dp         // 圆环直径
    val ringStroke = 10.dp    // 圆环线宽

    val tile = 48.dp          // 宫格图标块尺寸
}
