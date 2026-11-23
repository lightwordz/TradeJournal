package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BrokerRateProfile
import com.example.engine.CalculationEngine
import com.example.engine.MatchingMethod
import com.example.model.Segment
import com.example.model.Trade
import com.example.ui.components.EditorialCard
import com.example.ui.components.LedgerTradeRow
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.math.BigDecimal

@Composable
fun JournalScreen(
    trades: List<Trade>,
    brokerProfile: BrokerRateProfile,
    matchingMethod: MatchingMethod = MatchingMethod.FIFO,
    onTradeClick: (Trade) -> Unit,
    onAddTradeClick: () -> Unit,
    onImportCsvClick: () -> Unit = {},
    onScanScreenshotClick: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf("All") }

    val tradeResults = remember(trades, brokerProfile, matchingMethod) {
        trades.associate { it.id to CalculationEngine.calculateTradeResult(it, brokerProfile, matchingMethod) }
    }

    val filteredTrades = remember(trades, searchQuery, activeFilter, tradeResults) {
        trades.filter { trade ->
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                trade.symbol.contains(q, ignoreCase = true) ||
                trade.setup.contains(q, ignoreCase = true) ||
                trade.notes.contains(q, ignoreCase = true) ||
                trade.segment.displayName.contains(q, ignoreCase = true) ||
                trade.tags.any { it.contains(q, ignoreCase = true) }

            val net = tradeResults[trade.id]?.netPnl ?: BigDecimal.ZERO
            val matchesFilter = when (activeFilter) {
                "Winners" -> net > BigDecimal.ZERO
                "Losses" -> net < BigDecimal.ZERO
                "Options" -> trade.segment == Segment.OPT
                "Futures" -> trade.segment == Segment.FUT
                "Stocks" -> trade.segment == Segment.EQ_INTRADAY || trade.segment == Segment.EQ_DELIVERY
                "Gold & Oil" -> trade.segment == Segment.COMMODITY_FUT || trade.segment == Segment.COMMODITY_OPT
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val filteredSummary = remember(filteredTrades, tradeResults) {
        val net = filteredTrades.fold(BigDecimal.ZERO) { acc, t ->
            acc.add(tradeResults[t.id]?.netPnl ?: BigDecimal.ZERO)
        }
        val wins = filteredTrades.count { (tradeResults[it.id]?.netPnl ?: BigDecimal.ZERO) > BigDecimal.ZERO }
        val winRate = if (filteredTrades.isNotEmpty()) (wins * 100) / filteredTrades.size else 0
        Triple(filteredTrades.size, winRate, net)
    }

    val groupedByDate = remember(filteredTrades) {
        filteredTrades.groupBy { it.date }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val horizontalPad = when {
            maxWidth >= 840.dp -> 28.dp
            maxWidth >= 600.dp -> 24.dp
            maxWidth < 360.dp -> 16.dp
            else -> 20.dp
        }
        val bottomClearance = if (maxWidth >= 600.dp) 32.dp else 96.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPad,
                end = horizontalPad,
                top = 12.dp,
                bottom = bottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Journal",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subLabel = if (filteredTrades.size == trades.size) {
                            "${trades.size} logged trade${if (trades.size == 1) "" else "s"}"
                        } else {
                            "Showing ${filteredTrades.size} of ${trades.size} trades"
                        }
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onScanScreenshotClick,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("journal_scan_screenshot_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = "Scan trade screenshot",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Scan",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = onImportCsvClick,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("journal_import_csv_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.UploadFile,
                                contentDescription = "Import CSV",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "CSV",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                placeholder = {
                    Text(
                        text = "Search ticker, strategy, or notes",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search trades",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Winners", "Losses", "Options", "Futures", "Stocks", "Gold & Oil").forEach { chip ->
                    val selected = activeFilter == chip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { activeFilter = chip },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                        border = if (selected) null else BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.5.sp,
                                color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Compact Live Filtered Summary Strip
        if (filteredTrades.isNotEmpty()) {
            item(key = "journal_summary_strip") {
                val (count, winRate, net) = filteredSummary
                val netAccent = if (net >= BigDecimal.ZERO) ProfitGreen else LossRose
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Trades",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontFamily = JetBrainsMonoFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(22.dp),
                            thickness = 0.75.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Win Rate",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$winRate%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontFamily = JetBrainsMonoFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(22.dp),
                            thickness = 0.75.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Column(
                            modifier = Modifier
                                .weight(1.25f)
                                .padding(start = 6.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Net Return",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Formatters.inr(net, showSign = true),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontFamily = JetBrainsMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    color = netAccent
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (groupedByDate.isEmpty()) {
            item {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                        if (trades.isEmpty()) {
                            Text(
                                text = "Your journal is empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap + below to log your first trade, or import a broker CSV.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onAddTradeClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                            ) {
                                Text("Log First Trade", color = MaterialTheme.colorScheme.background)
                            }
                        } else {
                            Text(
                                text = "No matching trades",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Nothing matched your search or active filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    activeFilter = "All"
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Clear Filters", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        } else {
            groupedByDate.forEach { (date, dayTrades) ->
                val dayNet = dayTrades.fold(BigDecimal.ZERO) { acc, t ->
                    acc.add(tradeResults[t.id]?.netPnl ?: BigDecimal.ZERO)
                }
                item(key = "date_group_$date") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (dayTrades.size > 1) {
                                    "${Formatters.friendlyDate(date)} · ${dayTrades.size} trades"
                                } else {
                                    Formatters.friendlyDate(date)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            // Avoid repeating identical single-trade rupee P&L right above its own row
                            if (dayTrades.size > 1) {
                                Text(
                                    text = "Day ${Formatters.inr(dayNet, showSign = true)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.5.sp,
                                        fontFamily = JetBrainsMonoFont,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (dayNet >= BigDecimal.ZERO) ProfitGreen else LossRose
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        EditorialCard(modifier = Modifier.fillMaxWidth()) {
                            dayTrades.forEachIndexed { idx, trade ->
                                val res = tradeResults[trade.id] ?: CalculationEngine.calculateTradeResult(trade, brokerProfile, matchingMethod)
                                LedgerTradeRow(
                                    trade = trade,
                                    result = res,
                                    onClick = { onTradeClick(trade) }
                                )
                                if (idx < dayTrades.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        thickness = 0.75.dp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
