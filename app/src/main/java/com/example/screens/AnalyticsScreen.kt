package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AnalyticsEngine
import com.example.engine.BrokerRateProfile
import com.example.engine.MatchingMethod
import com.example.model.Trade
import com.example.ui.components.EditorialCard

@Composable
fun AnalyticsScreen(
    trades: List<Trade>,
    brokerProfile: BrokerRateProfile,
    matchingMethod: MatchingMethod = MatchingMethod.FIFO
) {
    var timeframe by rememberSaveable { mutableStateOf("ALL") }
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    val filteredTrades = remember(trades, timeframe) {
        filterTradesByPeriod(trades, timeframe)
    }
    val chronological = remember(filteredTrades) {
        filteredTrades.reversed()
    }
    val report = remember(chronological, brokerProfile, matchingMethod) {
        AnalyticsEngine.buildReport(chronological, brokerProfile, matchingMethod)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isNarrow = maxWidth < 360.dp
        val isWide = maxWidth >= 600.dp
        val horizontalPad = when {
            isNarrow -> 16.dp
            isWide -> 24.dp
            else -> 20.dp
        }
        val bottomClearance = if (isWide) 32.dp else 96.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPad,
                end = horizontalPad,
                top = 12.dp,
                bottom = bottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${report.metrics.totalTrades} trade${if (report.metrics.totalTrades == 1) "" else "s"} · After ${brokerProfile.brokerName} fees",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1M", "6M", "1Y", "ALL").forEach { key ->
                        val selected = timeframe == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    timeframe = key
                                    scrubbedIndex = null
                                }
                                .testTag("analytics_period_$key"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                            border = if (selected) null else BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (report.metrics.totalTrades == 0) {
                item {
                    EditorialCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "No trades in this period",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Switch to ALL or log trades to view historical P&L, weekday stats, and tagged patterns.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                insightsOverviewItems(
                    report = report,
                    scrubbedIndex = scrubbedIndex,
                    onScrubIndex = { scrubbedIndex = it }
                )
            }
        }
    }
}
