package com.limiao.notes.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.limiao.notes.ui.theme.LocalAppSkin
import com.limiao.notes.ui.theme.Skins
import com.limiao.notes.ui.theme.toColorScheme

// ==================== 配色令牌（全部跟随皮肤） ====================
//
// 下面全是「读 CompositionLocal 的 Composable getter」，不是写死的颜色常量。
// 好处：换肤时全站自动生效，页面代码一个字都不用改，用法跟以前完全一样 —— 直接写 `Primary` / `Ink` / `Line`。
//
// ⚠️ 因为是 Composable getter，**只能**在 @Composable 作用域里读（函数体、Modifier 链、
//    @Composable 函数的默认参数值都可以）。**不能**写在顶层 val、object 初始化器、
//    非 Composable 的普通函数里 —— 那样编译不过。

// —— 兼容令牌：名字与旧版一致，老页面无需改动 ——
val Primary: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.accent
val Bg: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.bg
val Surface: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.card
val Ink: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.textPrimary
val InkSoft: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.textSecondary
val Muted: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.textTertiary
val Line: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.divider
val IncomeGreen: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.success

// —— 新语义令牌：新代码优先用这些，名字更贴近用途 ——
val Accent: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.accent
val AccentPressed: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.accentPressed
val AccentSoft: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.accentSoft
val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.onAccent

val CardBg: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.card
val CardBorder: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.cardBorder
val Divider: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.divider
val DarkBanner: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.darkBanner

val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.textSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.textTertiary

val Success: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.success
val SuccessSoft: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.successSoft
val Danger: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.danger
val DangerSoft: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.dangerSoft
val Warning: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.warning
val WarningSoft: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.warningSoft
val Water: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.water
val WaterSoft: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.waterSoft

val ChartLine: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.chartLine
val ChartFill: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.chartFill
val ChartGrid: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.chartGrid

val NavBar: Color @Composable @ReadOnlyComposable get() = LocalAppSkin.current.navBar

/**
 * 全站主题入口。
 *
 * [skinId] 来自 `AppData.skinId`（存在 DataStore 里），所以换肤能持久化、重启后仍然生效。
 * 找不到对应皮肤时自动回落到默认皮肤，不会崩。
 */
@Composable
fun LiMiaoTheme(
    skinId: String = Skins.DEFAULT.id,
    content: @Composable () -> Unit,
) {
    val skin = remember(skinId) { Skins.byId(skinId) }
    // 本应用固定浅色（皮肤机制天然支持深色，将来再加一套深色皮肤即可）
    CompositionLocalProvider(LocalAppSkin provides skin) {
        MaterialTheme(colorScheme = skin.toColorScheme(), content = content)
    }
}
