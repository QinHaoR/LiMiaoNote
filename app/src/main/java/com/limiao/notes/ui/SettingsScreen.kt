package com.limiao.notes.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.parseAppData
import com.limiao.notes.data.toJson
import java.io.File

@Composable
fun SettingsScreen(data: AppData, onSave: (AppData) -> Unit) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val text = context.contentResolver.openInputStream(it)
                    ?.bufferedReader()?.use { r -> r.readText() } ?: ""
                val incoming = parseAppData(text)
                onSave(mergeData(data, incoming))
            } catch (e: Exception) {
                // 解析失败，忽略
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        // ===== 数据备份 =====
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("数据备份", fontWeight = FontWeight.Medium)
                Text("数据只存在本机，卸载或清数据会丢失，建议定期导出", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                ActionButton("导出数据（JSON 文件）") { exportToFile(context, data) }
                Spacer(Modifier.height(8.dp))
                ActionButton("导入 / 合并数据") { importLauncher.launch("application/json") }
                Spacer(Modifier.height(8.dp))
                ActionButton("清空所有数据", danger = true) { showClearConfirm = true }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 周期参数 =====
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("周期参数", fontWeight = FontWeight.Medium)
                Text("用于经期预测推算", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(8.dp))
                StepRow("平均周期长度（天）", data.settings.avgCycleLength) {
                    onSave(data.copy(settings = data.settings.copy(avgCycleLength = it)))
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                StepRow("平均经期长度（天）", data.settings.avgPeriodLength) {
                    onSave(data.copy(settings = data.settings.copy(avgPeriodLength = it)))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== 关于 =====
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("关于", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("应用名称", color = Muted); Text("黎喵记录")
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("软件版本", color = Muted); Text("v0.1")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "经期预测基于历史记录统计推算，仅供参考，不能作为医疗或避孕依据。",
            color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空所有数据？") },
            text = { Text("此操作不可恢复，请先导出备份。") },
            confirmButton = {
                TextButton(onClick = {
                    onSave(AppData.empty()); showClearConfirm = false
                }) { Text("清空", color = androidx.compose.ui.graphics.Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消", color = Muted) } },
        )
    }
}

@Composable
private fun ActionButton(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) androidx.compose.ui.graphics.Color(0xFFFFF1F2) else Bg,
            contentColor = if (danger) androidx.compose.ui.graphics.Color(0xFFDC2626) else Ink,
        ),
    ) { Text(text, fontSize = 14.sp) }
}

@Composable
private fun StepRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = InkSoft, modifier = Modifier.weight(1f))
        Text("－", fontSize = 18.sp, color = Muted,
            modifier = Modifier
                .clickable { if (value > 1) onChange(value - 1) }
                .background(Bg, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp))
        Text(" $value ", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("＋", fontSize = 18.sp, color = Muted,
            modifier = Modifier
                .clickable { if (value < 90) onChange(value + 1) }
                .background(Bg, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

/** 导出：写私有文件 + 系统分享（可发微信/QQ/存到手机） */
private fun exportToFile(context: Context, data: AppData) {
    try {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "黎喵记录数据-${DateFmt.today()}.json")
        file.writeText(data.toJson())
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出数据"))
    } catch (e: Exception) {
        // FileProvider 未配置等异常：静默
    }
}

/** 合并：按 id 去重（日记/每日笔记按 date），已有的保留，只追加新的 */
private fun mergeData(current: AppData, incoming: AppData): AppData {
    fun <T> mergeById(a: List<T>, b: List<T>, idOf: (T) -> String): List<T> {
        val ids = a.map(idOf).toHashSet()
        return a + b.filter { idOf(it) !in ids }
    }
    fun <T> mergeByDate(a: List<T>, b: List<T>, keyOf: (T) -> String): List<T> {
        val keys = a.map(keyOf).toHashSet()
        return a + b.filter { keyOf(it) !in keys }
    }
    return AppData(
        version = 1,
        notes = mergeById(current.notes, incoming.notes) { it.id },
        diaries = mergeById(current.diaries, incoming.diaries) { it.id },
        transactions = mergeById(current.transactions, incoming.transactions) { it.id },
        periods = mergeById(current.periods, incoming.periods) { it.id },
        dayNotes = mergeByDate(current.dayNotes, incoming.dayNotes) { it.date },
        settings = current.settings.copy(
            avgCycleLength = incoming.settings.avgCycleLength,
            avgPeriodLength = incoming.settings.avgPeriodLength,
        ),
    )
}
