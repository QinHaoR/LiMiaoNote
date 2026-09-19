package com.limiao.notes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.ChartGrid
import com.limiao.notes.ui.Water
import com.limiao.notes.ui.theme.Dim

/**
 * 细进度条。
 *
 * 内建 700ms 缓动 + `coerceIn(0,1)`，所以调用方不必处理"进度越界"和"跳变生硬"两件事。
 */
@Composable
fun ThinBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    trackColor: Color = ChartGrid,
    height: Dp = Dim.barThin,
) {
    val p = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = p,
        animationSpec = tween(700),
        label = "thinBar",
    )
    val shape = RoundedCornerShape(height)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor),
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(color),
            )
        }
    }
}

/**
 * 圆环进度（本轮视觉重构的标志性元素）。
 *
 * 底环 + 圆头进度弧，中心可放任意内容（通常是一个大数字）。
 * 用 Canvas 手绘，没引任何图表库。
 */
@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = Dim.ring,
    stroke: Dp = Dim.ringStroke,
    color: Color = Accent,
    trackColor: Color = ChartGrid,
    center: @Composable () -> Unit = {},
) {
    val p = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = p,
        animationSpec = tween(800),
        label = "ring",
    )
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val d = diameter.toPx()
            val arcSize = Size(d - sw, d - sw)
            val topLeft = Offset(sw / 2f, sw / 2f)
            // 底环
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw),
            )
            // 进度弧（从 12 点方向开始，顺时针）
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }
        center()
    }
}

/**
 * 分段式进度（饮水那种「8 格」）。
 * 比一根连续进度条更容易一眼数出"喝了几杯"。
 */
@Composable
fun SegmentedBar(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    color: Color = com.limiao.notes.ui.Water,
    trackColor: Color = ChartGrid,
    height: Dp = 9.dp,
) {
    val n = total.coerceAtLeast(1)
    androidx.compose.foundation.layout.Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        repeat(n) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(if (i < filled) color else trackColor),
            )
        }
    }
}
