package com.limiao.notes.ui.theme

import com.limiao.notes.ui.theme.skins.PaperSkin

/**
 * 皮肤注册表。
 *
 * 加一套新皮肤只需要两步：
 *   1. 在 `ui/theme/skins/` 下新建文件，定义好一个 `AppSkin` 常量
 *   2. 把它加进下面的 `ALL`
 * 页面代码**一个字都不用改**，「我的」页的外观选择器会自动多出一项。
 */
object Skins {
    val ALL: List<AppSkin> = listOf(
        PaperSkin,
        // 后续在这里追加：黎喵粉 / 深色 …
    )

    val DEFAULT: AppSkin = ALL.first()

    /** 按 id 取皮肤；id 为空或找不到时回落到默认皮肤（老数据 / 数据损坏都不会崩） */
    fun byId(id: String?): AppSkin = ALL.firstOrNull { it.id == id } ?: DEFAULT
}
