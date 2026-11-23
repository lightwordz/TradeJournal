package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AnalyticsReport
import com.example.engine.BucketMetric
import com.example.ui.components.EditorialCard
import com.example.ui.components.EquityCurveChart
import com.example.ui.components.WeekdayDivergingChart
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.math.BigDecimal

internal fun LazyListScope.insightsOverviewItems(
    report: AnalyticsReport,
    scrubbedIndex: Int?,
    onScrubIndex: (Int?) -> Unit
) {
    // 1. Performance Summary Card (Unchanged)
    item {
        val netPnl = report.metrics.totalNetPnl
        val netAccent = if (netPnl >= BigDecimal.ZERO) ProfitGreen else LossRose

        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Net Profit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.inr(netPnl, showSign = true),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = netAccent
                            )
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ProfitGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${report.metrics.winRate}% Wins",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.SemiBold,
                                color = ProfitGreen
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Visual Win / Loss ratio bar
                val total = report.metrics.totalTrades.coerceAtLeast(1)
                val winFrac = report.metrics.winningTrades.toFloat() / total.toFloat()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LossRose.copy(alpha = 0.22f))
                ) {
                    if (winFrac > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(winFrac)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ProfitGreen)
                        )
                    }
                }

                // 3 plain metrics side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimpleStatCol(
                        modifier = Modifier.weight(1f),
                        label = "Avg Win (${report.metrics.winningTrades})",
                        value = Formatters.compactInr(report.avgWin, showSign = true),
                        valueColor = ProfitGreen
                    )
                    SimpleStatCol(
                        modifier = Modifier.weight(1f),
                        label = "Avg Loss (${report.metrics.losingTrades})",
                        value = if (report.avgLoss > BigDecimal.ZERO) "-${Formatters.compactInr(report.avgLoss)}" else "₹0",
                        valueColor = LossRose,
                        alignment = Alignment.CenterHorizontally
                    )
                    SimpleStatCol(
                        modifier = Modifier.weight(1f),
                        label = "Win vs Loss",
                        value = "${report.payoffRatio}x",
                        valueColor = if (report.payoffRatio >= BigDecimal("1.2")) ProfitGreen else MaterialTheme.colorScheme.onSurface,
                        alignment = Alignment.End
                    )
                }

                PlainCalloutBanner(
                    icon = Icons.Outlined.Lightbulb,
                    text = report.edgeTakeaway,
                    accent = if (netPnl >= BigDecimal.ZERO) PrimaryCobalt else GoldCommodity
                )
            }
        }
    }

    // 2. Cumulative Equity Curve
    item {
        val points = report.equityDrawdownSeries.map { it.cumulativePnl.toFloat() }
        val activePoint = scrubbedIndex?.let { report.equityDrawdownSeries.getOrNull(it) }
        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Equity Curve",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activePoint != null) {
                                "${Formatters.friendlyDate(activePoint.date)} · ${activePoint.symbol} · ${Formatters.inr(activePoint.cumulativePnl, showSign = true)}"
                            } else {
                                "Touch curve to inspect any trade"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Biggest Dip",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (report.metrics.maxDrawdown > BigDecimal.ZERO) {
                                "-${Formatters.inr(report.metrics.maxDrawdown)}"
                            } else {
                                "₹0"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = if (report.metrics.maxDrawdown > BigDecimal.ZERO) LossRose else ProfitGreen
                            )
                        )
                    }
                }

                EquityCurveChart(
                    points = points,
                    selectedIndex = scrubbedIndex,
                    onPointSelected = onScrubIndex
                )
            }
        }
    }

    // 3. Weekday Breakdown (Taller square/vertical card with top-day badge)
    item {
        val bestDay = report.weekdayStats.filter { it.count > 0 }.maxByOrNull { it.netPnl }
        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Weekday Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Historical net P&L and win rate by weekday",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bestDay != null && bestDay.netPnl > BigDecimal.ZERO) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ProfitGreen.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Top · ${bestDay.label}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFont,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ProfitGreen
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                WeekdayDivergingChart(weekdays = report.weekdayStats)
            }
        }
    }

    // 4. Fees & Charges (Clean 3-number minimalist strip, no clutter)
    item {
        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fees & Taxes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${report.feeDragPct}% of gross",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFont,
                            fontWeight = FontWeight.Medium,
                            color = if (report.feeDragPct >= 25) LossRose else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimpleStatCol(
                        modifier = Modifier.weight(1f),
                        label = "Total Paid",
                        value = Formatters.inr(report.totalFees),
                        valueColor = LossRose
                    )
                    SimpleStatCol(
                        modifier = Modifier.weight(1f),
                        label = "Brokerage",
                        value = Formatters.inr(report.totalBrokerage),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        alignment = Alignment.CenterHorizontally
                    )
                    SimpleStatCol(
                        modifier = Modifier.weight(1f),
                        label = "STT & Taxes",
                        value = Formatters.inr(report.totalStatutoryTaxes),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        alignment = Alignment.End
                    )
                }
            }
        }
    }

    // 5. Logged Behavior Patterns (informational historical summary)
    item {
        val topMood = report.emotionStats.firstOrNull { it.count > 0 }
        val topLeak = report.mistakeStats.firstOrNull { it.netPnl < BigDecimal.ZERO }

        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Psychology",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Informational only",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = report.psychologyTakeaway,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactBucketTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        bucket = report.sequenceBehavior.afterWin
                    )
                    CompactBucketTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        bucket = report.sequenceBehavior.afterLoss
                    )
                }

                if (topMood != null || topLeak != null) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (topMood != null) {
                            MinimalInsightRow(
                                title = "Top tagged mood",
                                detail = "${topMood.label} (${topMood.winRatePct}% win)",
                                amount = topMood.netPnl
                            )
                        }
                        if (topLeak != null) {
                            MinimalInsightRow(
                                title = "Highest loss tag",
                                detail = "${topLeak.label} (${topLeak.count}×)",
                                amount = topLeak.netPnl
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. Category Breakdown (Tabbed Minimalist Card: Market | Setup | Side)
    item {
        var activeLens by remember { mutableIntStateOf(0) }
        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By Category",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("Market", "Setup", "Side").forEachIndexed { idx, label ->
                                val picked = activeLens == idx
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { activeLens = idx },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (picked) MaterialTheme.colorScheme.surface else Color.Transparent
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (picked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                when (activeLens) {
                    0 -> CleanBucketList(items = report.segmentStats.take(4))
                    1 -> CleanBucketList(items = report.setupStats.take(4))
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactBucketTile(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                bucket = report.buyMetric
                            )
                            CompactBucketTile(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                bucket = report.sellMetric
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlainCalloutBanner(icon: ImageVector, text: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.07f),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "HISTORICAL SUMMARY (INFORMATIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accent
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SimpleStatCol(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = valueColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactBucketTile(modifier: Modifier = Modifier, bucket: BucketMetric) {
    val accent = if (bucket.netPnl >= BigDecimal.ZERO) ProfitGreen else LossRose
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = bucket.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (bucket.count > 0) Formatters.inr(bucket.netPnl, showSign = true) else "—",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFont,
                    fontWeight = FontWeight.Bold,
                    color = if (bucket.count > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (bucket.count > 0) "${bucket.count} trades · ${bucket.winRatePct}% win" else "No trades",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MinimalInsightRow(title: String, detail: String, amount: BigDecimal) {
    val color = if (amount >= BigDecimal.ZERO) ProfitGreen else LossRose
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = Formatters.inr(amount, showSign = true),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = JetBrainsMonoFont,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        )
    }
}

@Composable
private fun CleanBucketList(items: List<BucketMetric>) {
    if (items.isEmpty()) {
        Text(
            text = "Not enough trades yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { idx, item ->
            val color = if (item.netPnl >= BigDecimal.ZERO) ProfitGreen else LossRose
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.count} trade${if (item.count == 1) "" else "s"} · ${item.winRatePct}% win",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = Formatters.inr(item.netPnl, showSign = true),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFont,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                )
            }
            if (idx < items.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
