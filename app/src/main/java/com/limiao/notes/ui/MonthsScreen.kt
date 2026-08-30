package com.limiao.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limiao.notes.data.AppData

/** 月份总览：按月列出支出/收入/结余，点某月进月明细 */
@Composable
fun MonthsScreen(data: AppData, onOpenMonth: (String) -> Unit, onBack: () -> Unit) {
    val byMonth = data.transactions
        .groupBy { it.date.substring(0, 7) }
        .toSortedMap(compareByDescending { it })

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 顶部：返回首页 + 标题（与月明细页样式一致）
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "‹",
                fontSize = 20.sp,
                color = Muted,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Text(
                "月份总览",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(28.dp))
        }
        Spacer(Modifier.height(12.dp))

        if (byMonth.isEmpty()) {
            Text("还没有记账记录", color = Muted, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            byMonth.forEach { (ym, txns) ->
                val expense = txns.filter { it.type == "expense" }.sumOf { it.amount }
                val income = txns.filter { it.type == "income" }.sumOf { it.amount }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenMonth(ym) },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            monthLabel(ym),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text("支 ¥${fmtMoney(expense)}", color = Primary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        Text("收 ¥${fmtMoney(income)}", color = IncomeGreen, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        Text("结 ¥${fmtMoney(income - expense)}", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
