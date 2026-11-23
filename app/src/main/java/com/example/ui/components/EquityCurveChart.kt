package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LossRose
import com.example.ui.theme.ProfitGreen
import kotlin.math.roundToInt

@Composable
fun EquityCurveChart(
    points: List<Float>,
    selectedIndex: Int? = null,
    onPointSelected: (Int?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lastVal = points.lastOrNull() ?: 0f
    val activeVal = if (selectedIndex != null && selectedIndex in points.indices) points[selectedIndex] else lastVal
    val lineColor = if (activeVal >= 0f) ProfitGreen else LossRose
    val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val crosshairColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
    val dotHalo = lineColor.copy(alpha = 0.20f)

    fun indexFromX(x: Float, width: Float): Int? {
        if (points.isEmpty() || width <= 0f) return null
        val totalSeries = points.size + 1
        val step = width / (totalSeries - 1).coerceAtLeast(1)
        val rawIdx = (x / step).roundToInt() - 1
        return rawIdx.coerceIn(0, points.lastIndex)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(114.dp)
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        onPointSelected(indexFromX(offset.x, size.width.toFloat()))
                        tryAwaitRelease()
                        onPointSelected(null)
                    }
                )
            }
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onPointSelected(indexFromX(offset.x, size.width.toFloat()))
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onPointSelected(indexFromX(change.position.x, size.width.toFloat()))
                    },
                    onDragEnd = { onPointSelected(null) },
                    onDragCancel = { onPointSelected(null) }
                )
            }
    ) {
        if (points.isEmpty()) return@Canvas

        val series = listOf(0f) + points
        val minVal = series.minOrNull() ?: 0f
        val maxVal = series.maxOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val paddingY = size.height * 0.14f
        val usableHeight = size.height - (paddingY * 2f)

        fun yFor(value: Float): Float {
            val norm = (value - minVal) / range
            return size.height - paddingY - (norm * usableHeight)
        }

        val zeroY = yFor(0f)
        drawLine(
            color = baselineColor,
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        val stepX = if (series.size > 1) size.width / (series.size - 1) else size.width
        val coords = series.mapIndexed { idx, v ->
            Offset(x = idx * stepX, y = yFor(v))
        }

        val strokePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(coords.last().x, size.height)
            lineTo(coords.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.18f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = 2.25.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        coords.drop(1).dropLast(1).forEach { pt ->
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = pt)
            drawCircle(color = lineColor, radius = 2.dp.toPx(), center = pt)
        }

        val focusCoord = if (selectedIndex != null && selectedIndex + 1 in coords.indices) {
            val c = coords[selectedIndex + 1]
            drawLine(
                color = crosshairColor,
                start = Offset(c.x, 0f),
                end = Offset(c.x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            c
        } else {
            coords.last()
        }

        drawCircle(color = dotHalo, radius = 8.dp.toPx(), center = focusCoord)
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = focusCoord)
    }
}
