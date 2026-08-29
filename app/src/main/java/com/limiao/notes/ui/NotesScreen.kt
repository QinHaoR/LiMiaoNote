package com.limiao.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData
import com.limiao.notes.data.DateFmt
import com.limiao.notes.data.IdGen
import com.limiao.notes.data.Note

private val TAGS = listOf("便签", "灵感", "清单", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(data: AppData, onSave: (AppData) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<Note?>(null) }

    val sorted = data.notes.sortedWith(
        compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt }
    )

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("随手记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }
        if (sorted.isEmpty()) {
            item { Text("还没有笔记，点右下角 + 新建", color = Muted, modifier = Modifier.padding(vertical = 24.dp)) }
        }
        items(sorted, key = { it.id }) { n ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            n.content,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (n.pinned) "★" else "☆",
                            fontSize = 18.sp,
                            color = if (n.pinned) Primary else Muted,
                            modifier = Modifier
                                .clickable {
                                    onSave(data.copy(notes = data.notes.map {
                                        if (it.id == n.id) it.copy(pinned = !it.pinned) else it
                                    }))
                                }
                                .padding(4.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            n.tag,
                            modifier = Modifier.background(Bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                            color = InkSoft, fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(n.createdAt.take(10), color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editNote = n }) { Text("编辑", color = Muted, fontSize = 12.sp) }
                        TextButton(onClick = {
                            onSave(data.copy(notes = data.notes.filter { it.id != n.id }))
                        }) { Text("✕", color = Muted, fontSize = 14.sp) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    FloatingActionButton(
        onClick = { showSheet = true },
        containerColor = Primary,
        contentColor = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .align(Alignment.BottomEnd),
    ) { Text("＋", fontSize = 22.sp) }
    }

    if (showSheet || editNote != null) {
        val target = editNote
        var content by remember { mutableStateOf(target?.content ?: "") }
        var tag by remember { mutableStateOf(target?.tag ?: "便签") }
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; editNote = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    if (target == null) "新建笔记" else "编辑笔记",
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("写点什么…") },
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAGS.forEach { t ->
                        Text(
                            t,
                            modifier = Modifier
                                .clickable { tag = t }
                                .background(if (tag == t) Primary else Bg, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (tag == t) androidx.compose.ui.graphics.Color.White else InkSoft,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            if (target == null) {
                                val n = Note(IdGen.new(), content.trim(), tag, false, DateFmt.today())
                                onSave(data.copy(notes = data.notes + n))
                            } else {
                                onSave(data.copy(notes = data.notes.map {
                                    if (it.id == target.id) it.copy(content = content.trim(), tag = tag) else it
                                }))
                            }
                            showSheet = false; editNote = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary),
                ) { Text("保存") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
