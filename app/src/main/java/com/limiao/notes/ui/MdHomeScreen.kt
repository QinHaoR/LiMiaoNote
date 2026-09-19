package com.limiao.notes.ui

import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.MdHistory
import java.util.Calendar

/**
 * Markdown 阅读首页（侧边栏进入）：
 * 选择系统 .md 文件打开 + 最近打开历史（可重开 / 删除）。
 */
@Composable
fun MdHomeScreen(
    history: List<MdHistory>,
    onOpenUri: (Uri) -> Unit,
    onRemove: (MdHistory) -> Unit,
    onBack: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onOpenUri(uri)
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp, vertical = 4.dp), color = Primary, fontSize = 20.sp)
            Text("Markdown 阅读", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 打开按钮
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth().clickable { picker.launch(arrayOf("*/*")) },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(40.dp).background(Primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Description, null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("选择 Markdown 文件", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text("从系统里挑一个 .md 打开阅读", color = Muted, fontSize = 12.sp)
                        }
                        Text("›", color = Muted, fontSize = 18.sp)
                    }
                }
            }

            // 历史标题
            item {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("最近打开", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "共 ${history.size} 个",
                        color = Muted, fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        "还没有打开记录，点上方按钮选一个 .md 文件吧",
                        color = Muted, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                items(history, key = { it.uri }) { h ->
                    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenUri(Uri.parse(h.uri)) }.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Description, null, tint = InkSoft, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(openedLabel(h.openedAt), color = Muted, fontSize = 11.sp)
                            }
                            Icon(
                                Icons.Filled.Close, "删除记录",
                                tint = Muted,
                                modifier = Modifier.padding(8.dp).size(18.dp).clickable { onRemove(h) },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

private fun openedLabel(epoch: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = epoch }
    val p = { n: Int -> n.toString().padStart(2, '0') }
    val now = Calendar.getInstance()
    val same = { a: Calendar, b: Calendar ->
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
            a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
    }
    val hm = "${p(c.get(Calendar.HOUR_OF_DAY))}:${p(c.get(Calendar.MINUTE))}"
    return when {
        same(c, now) -> "今天 $hm"
        same(c, (Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) })) -> "昨天 $hm"
        else -> "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日 $hm"
    }
}
