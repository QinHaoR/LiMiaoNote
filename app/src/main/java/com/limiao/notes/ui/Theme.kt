package com.limiao.notes.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==================== 配色（对应 Web 版设计规范） ====================

val Primary = Color(0xFFE8737F)      // 主色（粉）
val Bg = Color(0xFFFDF5F6)           // 页面背景（浅粉）
val Surface = Color(0xFFFFFFFF)      // 卡片背景
val Ink = Color(0xFF333333)          // 主文字
val InkSoft = Color(0xFF555555)      // 次要文字
val Muted = Color(0xFF999999)        // 弱文字
val Line = Color(0xFFEEEEEE)         // 分隔线 / 描边
val IncomeGreen = Color(0xFF4CAF50)  // 收入绿

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Line,
    onSurfaceVariant = InkSoft,
    outline = Line,
    outlineVariant = Line,
)

@Composable
fun LiMiaoTheme(content: @Composable () -> Unit) {
    // 本应用固定浅色主题（与产品设计一致）
    MaterialTheme(colorScheme = LightColors, content = content)
}
