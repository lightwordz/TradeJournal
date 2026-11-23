package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BucketMetric
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.LossRose
import com.example.ui.theme.ProfitGreen
import com.example.utils.Formatters
import java.math.BigDecimal

/**
 * Balanced vertical diverging Canvas bar chart for Weekday (Mon-Fri) Net P&L.
 */
@Composable
fun WeekdayDivergingChart(
    weekdays: List<BucketMetric>,
    modifier: Modifier = Modifier
) {
    if (weekdays.isEmpty()) return
    val maxAbs = weekdays.maxOfOrNull { it.netPnl.abs().toFloat() }?.coerceAtLeast(1f) ?: 1f
    val baselineColor = MaterialTheme.colorScheme.outline
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
        ) {
            val zeroY = size.height * 0.5f
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            // Subtle upper & lower reference hairlines for balanced square/vertical feel
            drawLine(
                color = guideColor,
                start = Offset(0f, size.height * 0.1f),
                end = Offset(size.width, size.height * 0.1f),
                strokeWidth = 0.75.dp.toPx(),
                pathEffect = dash
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, size.height * 0.9f),
                end = Offset(size.width, size.height * 0.9f),
                strokeWidth = 0.75.dp.toPx(),
                pathEffect = dash
            )

            // Zero baseline
            drawLine(
                color = baselineColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx()
            )

            val slotWidth = size.width / weekdays.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.44f).coerceIn(18.dp.toPx(), 36.dp.toPx())
            val maxHalfHeight = size.height * 0.41f

            weekdays.forEachIndexed { idx, day ->
                val raw = day.netPnl.toFloat()
                val frac = (kotlin.math.abs(raw) / maxAbs).coerceIn(0f, 1f)
                val barH = if (day.count == 0) 2.dp.toPx() else (frac * maxHalfHeight).coerceAtLeast(6.dp.toPx())
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f

                val (top, color) = when {
                    day.count == 0 -> (zeroY - 1.dp.toPx()) to baselineColor
                    raw >= 0f -> (zeroY - barH) to ProfitGreen
                    else -> zeroY to LossRose
                }

                drawRoundRect(
                    color = color.copy(alpha = if (day.count == 0) 0.4f else 0.9f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekdays.forEach { day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (day.count > 0) Formatters.compactInr(day.netPnl, showSign = true) else "—",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFont,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = when {
                            day.count == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            day.netPnl >= BigDecimal.ZERO -> ProfitGreen
                            else -> LossRose
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (day.count > 0) {
                        Text(
                            text = "${day.winRatePct}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
