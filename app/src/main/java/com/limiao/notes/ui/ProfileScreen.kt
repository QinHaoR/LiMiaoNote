package com.limiao.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
@OptIn(ExperimentalLayoutApi::class)
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

    val miniApps = listOf(
        MiniApp("经期", Icons.Filled.CalendarMonth, Color(0xFFBE185D), Color(0xFFFCE7F3), onOpenPeriod),
        MiniApp("随手记", Icons.Filled.EditNote, Color(0xFFB45309), Color(0xFFFEF3C7), onOpenNotes),
        MiniApp("Markdown", Icons.Filled.Description, Color(0xFF0369A1), Color(0xFFE0F2FE), onOpenMdHome),
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // ===== 档案卡 =====
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像（点击可选）
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.12f))
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
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Edit, "修改昵称",
                                tint = Muted,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        draftNickname = data.profile.nickname
                                        editingNickname = true
                                    },
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("本地档案 · 无账号 · 数据只存本机", color = Muted, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)

                Row(Modifier.fillMaxWidth()) {
                    StatCell("${data.transactions.size}", "记账")
                    StatCell("${data.notes.size}", "笔记")
                    StatCell("${data.periods.size}", "经期")
                    StatCell("$activeDays", "记录天数")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 我的小程序 =====
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("我的小程序", fontWeight = FontWeight.Medium)
                Text(
                    "常用功能的快捷入口",
                    color = Muted, fontSize = 12.sp,
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
                            MiniAppCell(app, cellWidth)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 其它入口 =====
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column {
                MenuRow(Icons.Filled.Settings, "设置", "数据备份 · 周期参数 · 清空", onOpenSettings)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "黎喵记录 $APP_VERSION · 数据只存在本机",
            color = Muted, fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
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
                    placeholder = { Text("给自己起个名字", color = Muted) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = draftNickname.trim().ifBlank { Profile.DEFAULT.nickname }
                    onSave(data.copy(profile = data.profile.copy(nickname = name)))
                    editingNickname = false
                }) { Text("保存", color = Primary, fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(onClick = { editingNickname = false }) { Text("取消", color = Muted) }
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
                                .background(if (selected) Primary.copy(alpha = 0.18f) else Bg)
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
                TextButton(onClick = { pickingAvatar = false }) { Text("关闭", color = Muted) }
            },
        )
    }
}

/** 统计小格（四个等宽） */
@Composable
private fun RowScope.StatCell(value: String, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

/** 小程序图标（圆角方块 + 名称，微信小程序宫格样式） */
@Composable
private fun MiniAppCell(app: MiniApp, width: Dp) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = app.onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(app.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(app.icon, app.name, tint = app.tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(app.name, fontSize = 12.sp, color = InkSoft, maxLines = 1)
    }
}

/** 列表行（图标 + 标题 + 副标题 + 右箭头） */
@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, title, tint = InkSoft, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}
