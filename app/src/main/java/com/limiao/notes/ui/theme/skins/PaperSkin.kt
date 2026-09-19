package com.limiao.notes.ui.theme.skins

import androidx.compose.ui.graphics.Color
import com.limiao.notes.ui.theme.AppSkin

/**
 * 「纸白」—— 默认皮肤（v0.4 起）。
 *
 * 黑白极简 + 橙色强调 + 绿色语义。布局骨架参考 Keep：极浅底、白卡片、大数字、圆环进度。
 * 这是**唯一**写死色值的地方；页面里再出现硬编码颜色就是不支持换肤，属于 bug。
 */
val PaperSkin = AppSkin(
    id = "paper",
    name = "纸白",
    desc = "黑白极简 · 橙绿点缀",

    bg = Color(0xFFF4F4F6),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFECECEF),
    divider = Color(0xFFF0F0F2),
    darkBanner = Color(0xFF16181D),

    textPrimary = Color(0xFF16181D),
    textSecondary = Color(0xFF5C6068),
    textTertiary = Color(0xFF9BA0A8),

    accent = Color(0xFFFF6B35),
    accentPressed = Color(0xFFE8551F),
    accentSoft = Color(0xFFFFEDE6),
    onAccent = Color(0xFFFFFFFF),

    success = Color(0xFF16A34A),
    successSoft = Color(0xFFE8F7EE),
    danger = Color(0xFFE5484D),
    dangerSoft = Color(0xFFFDECEC),
    warning = Color(0xFFF59E0B),
    warningSoft = Color(0xFFFEF6E7),
    water = Color(0xFF4A9BFF),
    waterSoft = Color(0xFFE8F1FE),

    chartLine = Color(0xFFFF6B35),
    chartFill = Color(0x1FFF6B35), // 橙 12% 透明度，折线图面积填充用
    chartGrid = Color(0xFFF0F0F2),

    statusBar = Color(0xFFF4F4F6),
    navBar = Color(0xFFFFFFFF),
    splashBg = Color(0xFFF4F4F6),
    lightStatusBarIcons = true,
)
