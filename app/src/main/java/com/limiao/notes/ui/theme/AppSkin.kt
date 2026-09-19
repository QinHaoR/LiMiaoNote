package com.limiao.notes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.limiao.notes.ui.theme.skins.PaperSkin

/**
 * 一套皮肤 = 一组颜色令牌。
 *
 * 想加新皮肤？在 `ui/theme/skins/` 下新建一个文件写好 `AppSkin` 常量，再去 `Skins.kt` 的 `ALL` 里注册即可。
 * **不需要改任何页面代码** —— 页面读的都是 `ui/Theme.kt` 里的令牌，令牌读的就是这里的值。
 */
@Immutable
data class AppSkin(
    val id: String,
    val name: String,
    val desc: String,

    // —— 容器 ——
    val bg: Color,             // 页面底色
    val card: Color,           // 卡片底
    val cardBorder: Color,     // 卡片描边
    val divider: Color,        // 分隔线
    val darkBanner: Color,     // 深色横幅（周报卡等）

    // —— 文字 ——
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // —— 强调 ——
    val accent: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    val onAccent: Color,

    // —— 语义 ——
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val water: Color,
    val waterSoft: Color,

    // —— 图表 ——
    val chartLine: Color,
    val chartFill: Color,
    val chartGrid: Color,

    // —— 系统 ——
    val statusBar: Color,
    val navBar: Color,
    val splashBg: Color,
    val lightStatusBarIcons: Boolean,
)

/**
 * 当前皮肤。换肤 = 换这个 CompositionLocal 的值。
 *
 * 用 staticCompositionLocalOf：皮肤切换是低频操作，静态版读取时不做依赖追踪、开销更低；
 * 代价只是换肤那一瞬间全树重组一次，可以接受。
 */
val LocalAppSkin = staticCompositionLocalOf { PaperSkin }

/**
 * 把皮肤映射进 Material3，这样 AlertDialog / OutlinedTextField / Button / NavigationBar /
 * ModalBottomSheet 这些 M3 组件的默认配色也跟着皮肤走，不用逐个手写颜色。
 */
fun AppSkin.toColorScheme(): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    secondary = success,
    onSecondary = Color.White,
    tertiary = water,
    onTertiary = Color.White,
    background = bg,
    onBackground = textPrimary,
    surface = card,
    onSurface = textPrimary,
    surfaceVariant = bg,
    onSurfaceVariant = textSecondary,
    outline = cardBorder,
    outlineVariant = divider,
    error = danger,
    onError = Color.White,
    errorContainer = dangerSoft,
    onErrorContainer = danger,
)
