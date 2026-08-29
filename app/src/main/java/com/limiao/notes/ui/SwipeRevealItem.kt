package com.limiao.notes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * 左滑出现「编辑 / 删除」按钮的记录项（iOS 风格）
 * - 向左滑动 → 露出右侧 编辑(蓝) + 删除(红) 按钮
 * - 超过一半自动展开，否则回弹
 * - 点按钮执行对应操作后自动复位
 */
@Composable
fun SwipeRevealItem(
    editColor: Color = Color(0xFF3B82F6),
    deleteColor: Color = Color(0xFFDC2626),
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { 160.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun settle() {
        val target = if (offset.value < -revealPx / 2) -revealPx else 0f
        scope.launch { offset.animateTo(target, tween(180)) }
    }

    Box(Modifier.fillMaxWidth()) {
        // 背景按钮（右侧露出）
        Row(
            Modifier
                .fillMaxSize()
                .align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(editColor)
                    .clickable {
                        scope.launch { offset.animateTo(0f, tween(150)) }
                        onEdit()
                    },
                contentAlignment = Alignment.Center,
            ) { Text("编辑", color = Color.White, fontSize = 13.sp) }
            Box(
                Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(deleteColor)
                    .clickable {
                        scope.launch { offset.animateTo(0f, tween(150)) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center,
            ) { Text("删除", color = Color.White, fontSize = 13.sp) }
        }
        // 前景内容（可左滑，带不透明背景遮住按钮；graphicsLayer 保证命中区域不随滑动移动）
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .graphicsLayer { translationX = offset.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { settle() },
                        onDragCancel = { settle() },
                    ) { change, dragAmount ->
                        change.consume()
                        val next = (offset.value + dragAmount).coerceIn(-revealPx, 0f)
                        scope.launch { offset.snapTo(next) }
                    }
                },
        ) { content() }
    }
}
