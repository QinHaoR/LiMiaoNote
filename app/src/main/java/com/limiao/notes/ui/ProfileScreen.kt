package com.limiao.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.Profile
import com.limiao.notes.ui.components.CardTitleRow
import com.limiao.notes.ui.components.EntryTile
import com.limiao.notes.ui.components.ListRow
import com.limiao.notes.ui.components.SheetShell
import com.limiao.notes.ui.components.SkinCard
import com.limiao.notes.ui.components.StatCell
import com.limiao.notes.ui.components.ThinDivider
import com.limiao.notes.ui.components.rememberSheetStateExpanded
import com.limiao.notes.ui.theme.AppSkin
import com.limiao.notes.ui.theme.Dim
import com.limiao.notes.ui.theme.Skins

/** 一个小程序（「我的」页宫格里的快捷入口） */
private data class MiniApp(
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
    val onClick: () -> Unit,
)

/**
 * 「我的」tab
 *
 * 本应用无账号系统，所以这里是「本地档案」：昵称/头像只存本机，不上传任何服务器。
 * 小程序宫格 = 从底部 tab 与侧边栏收拢进来的常用入口（类似微信下拉的小程序）。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    data: AppData,
    onSave: (AppData) -> Unit,
    onOpenPeriod: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenMdHome: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var editingNickname by remember { mutableStateOf(false) }
    var pickingAvatar by remember { mutableStateOf(false) }
    var pickingSkin by remember { mutableStateOf(false) }
    var draftNickname by remember { mutableStateOf("") }

    // 记录天数：把记账 / 按天记录 / 笔记 / 经期起始日去重统计
    val activeDays = remember(data) {
        val days = HashSet<String>()
        data.transactions.forEach { if (it.date.isNotBlank()) days += it.date }
        data.dayNotes.forEach { if (it.date.isNotBlank()) days += it.date }
        data.notes.forEach { if (it.createdAt.length >= 10) days += it.createdAt.take(10) }
        data.periods.forEach { if (it.startDate.isNotBlank()) days += it.startDate }
        days.size
    }

    val currentSkin = remember(data.skinId) { Skins.byId(data.skinId) }

    val miniApps = listOf(
        MiniApp("经期", Icons.Filled.CalendarMonth, Accent, AccentSoft, onOpenPeriod),
        MiniApp("随手记", Icons.Filled.EditNote, Success, SuccessSoft, onOpenNotes),
        MiniApp("Markdown", Icons.Filled.Description, Water, WaterSoft, onOpenMdHome),
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dim.screen, vertical = 12.dp),
    ) {
        // ===== 档案卡 =====
        SkinCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 头像（点击可选）
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccentSoft)
                        .clickable {
                            draftNickname = data.profile.nickname
                            pickingAvatar = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(data.profile.avatar, fontSize = 28.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            data.profile.nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Edit, "修改昵称",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable {
                                    draftNickname = data.profile.nickname
                                    editingNickname = true
                                },
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("本地档案 · 无账号 · 数据只存本机", color = TextTertiary, fontSize = 12.sp)
                }
            }

            ThinDivider(Modifier.padding(vertical = 14.dp))

            Row(Modifier.fillMaxWidth()) {
                StatCell("${data.transactions.size}", "记账")
                StatCell("${data.notes.size}", "笔记")
                StatCell("${data.periods.size}", "经期")
                StatCell("$activeDays", "记录天数")
            }
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 外观（皮肤切换入口）=====
        SkinCard(padding = 0.dp) {
            ListRow(
                icon = Icons.Filled.Palette,
                title = "外观",
                subtitle = "当前：${currentSkin.name} · ${currentSkin.desc}",
                iconTint = Accent,
                onClick = { pickingSkin = true },
            )
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 我的小程序 =====
        SkinCard {
            CardTitleRow("我的小程序")
            Text(
                "常用功能的快捷入口",
                color = TextTertiary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cellWidth: Dp = maxWidth / 4
                FlowRow(
                    maxItemsInEachRow = 4,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    miniApps.forEach { app ->
                        EntryTile(
                            name = app.name,
                            icon = app.icon,
                            tint = app.tint,
                            bg = app.bg,
                            width = cellWidth,
                            onClick = app.onClick,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dim.gapSection))

        // ===== 其它入口 =====
        SkinCard(padding = 0.dp) {
            ListRow(
                icon = Icons.Filled.Settings,
                title = "设置",
                subtitle = "数据备份 · 周期参数 · 清空",
                onClick = onOpenSettings,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "黎喵记录 $APP_VERSION · 数据只存在本机",
            color = TextTertiary, fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }

    // ===== 选择皮肤 =====
    if (pickingSkin) {
        ModalBottomSheet(
            onDismissRequest = { pickingSkin = false },
            sheetState = rememberSheetStateExpanded(),
            containerColor = CardBg,
            dragHandle = null,
        ) {
            SheetShell(title = "外观", onClose = { pickingSkin = false }) {
                Text(
                    "选一套配色。切换后立刻全站生效，并记住你的选择。",
                    fontSize = 12.sp, color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(10.dp))
                Skins.ALL.forEach { skin ->
                    SkinOptionRow(
                        skin = skin,
                        selected = skin.id == data.skinId,
                        onClick = {
                            onSave(data.copy(skinId = skin.id))
                            pickingSkin = false
                        },
                    )
                }
                Text(
                    "更多皮肤开发中，敬请期待",
                    fontSize = 12.sp, color = TextTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }

    // ===== 改昵称 =====
    if (editingNickname) {
        AlertDialog(
            onDismissRequest = { editingNickname = false },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = draftNickname,
                    onValueChange = { if (it.length <= 12) draftNickname = it },
                    singleLine = true,
                    placeholder = { Text("给自己起个名字", color = TextTertiary) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = draftNickname.trim().ifBlank { Profile.DEFAULT.nickname }
                    onSave(data.copy(profile = data.profile.copy(nickname = name)))
                    editingNickname = false
                }) { Text("保存", color = Accent, fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(onClick = { editingNickname = false }) { Text("取消", color = TextTertiary) }
            },
        )
    }

    // ===== 换头像 =====
    if (pickingAvatar) {
        AlertDialog(
            onDismissRequest = { pickingAvatar = false },
            title = { Text("选择头像") },
            text = {
                FlowRow(
                    maxItemsInEachRow = 5,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Profile.AVATARS.forEach { emoji ->
                        val selected = emoji == data.profile.avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (selected) AccentSoft else Bg)
                                .clickable {
                                    onSave(data.copy(profile = data.profile.copy(avatar = emoji)))
                                    pickingAvatar = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingAvatar = false }) { Text("关闭", color = TextTertiary) }
            },
        )
    }
}

/** 皮肤选项一行：左边色卡预览（底色/卡片色/强调色/语义色），右边名称与描述，选中打勾 */
@Composable
private fun SkinOptionRow(
    skin: AppSkin,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 4 个色点组成的预览
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            SkinDot(skin.bg, bordered = true)
            SkinDot(skin.card, bordered = true)
            SkinDot(skin.accent, bordered = false)
            SkinDot(skin.success, bordered = false)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                skin.name,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = TextPrimary,
            )
            Text(skin.desc, fontSize = 12.sp, color = TextTertiary, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) {
            Icon(Icons.Filled.Check, "已选中", tint = Accent, modifier = Modifier.size(20.dp))
        }
    }
}

/** 皮肤预览里的一个小色点（浅色点加描边才看得见） */
@Composable
private fun SkinDot(color: Color, bordered: Boolean) {
    Box(
        Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (bordered) Modifier.border(1.dp, CardBorder, CircleShape) else Modifier),
    )
}
