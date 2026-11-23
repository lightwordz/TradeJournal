package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BrokerRateProfile
import com.example.engine.CalculationEngine
import com.example.engine.MatchingMethod
import com.example.model.Trade
import com.example.ui.components.EditorialCard
import com.example.ui.components.LedgerTradeRow
import com.example.ui.theme.*
import com.example.utils.Formatters
import com.example.utils.IndianMarketCalendar
import com.example.utils.MonthKey
import java.math.BigDecimal

@Composable
fun CalendarScreen(
    trades: List<Trade>,
    brokerProfile: BrokerRateProfile,
    matchingMethod: MatchingMethod = MatchingMethod.FIFO,
    onTradeClick: (Trade) -> Unit = {}
) {
    val tradeResults = remember(trades, brokerProfile, matchingMethod) {
        trades.map { CalculationEngine.calculateTradeResult(it, brokerProfile, matchingMethod) }
    }
    val byDate = remember(trades) { trades.groupBy { it.date } }

    val todayInfo = remember { IndianMarketCalendar.todayIst() }
    val latestTradeDate = remember(trades) { trades.maxOfOrNull { it.date } }
    val initialTarget = remember(latestTradeDate, todayInfo) {
        latestTradeDate?.let { IndianMarketCalendar.parseDateParts(it) }
            ?: (todayInfo.first to todayInfo.second)
    }

    var currentMonth by remember { mutableStateOf(initialTarget.first) }
    var selectedDay by remember { mutableIntStateOf(initialTarget.second) }
    var userNavigatedCalendar by remember { mutableStateOf(false) }

    // If Room trades finish loading after the first frame and user hasn't navigated yet, snap to latest trade
    LaunchedEffect(latestTradeDate) {
        if (!userNavigatedCalendar && latestTradeDate != null) {
            IndianMarketCalendar.parseDateParts(latestTradeDate)?.let { (m, d) ->
                currentMonth = m
                selectedDay = d
            }
        }
    }

    val daysInMonth = remember(currentMonth) { IndianMarketCalendar.daysInMonth(currentMonth) }
    if (selectedDay > daysInMonth) selectedDay = daysInMonth

    val monthPrefix = currentMonth.prefix()
    val selectedDateStr = currentMonth.dateString(selectedDay)
    val isViewingToday = currentMonth == todayInfo.first && selectedDay == todayInfo.second

    val activeDaysCount = byDate.keys.count { it.startsWith(monthPrefix) }
    val greenDaysCount = byDate.entries.count { (date, list) ->
        date.startsWith(monthPrefix) && list.fold(BigDecimal.ZERO) { acc, t ->
            acc.add(tradeResults.firstOrNull { it.tradeId == t.id }?.netPnl ?: BigDecimal.ZERO)
        } >= BigDecimal.ZERO
    }
    val bestDayPnl = byDate.entries
        .filter { it.key.startsWith(monthPrefix) }
        .maxOfOrNull { (_, list) ->
            list.fold(BigDecimal.ZERO) { acc, t ->
                acc.add(tradeResults.firstOrNull { it.tradeId == t.id }?.netPnl ?: BigDecimal.ZERO)
            }
        } ?: BigDecimal.ZERO

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
        val cellHeight = when {
            isNarrow -> 48.dp
            isWide -> 60.dp
            else -> 54.dp
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Calendar",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = IndianMarketCalendar.monthLabel(currentMonth),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isViewingToday) {
                            TextButton(
                                onClick = {
                                    userNavigatedCalendar = true
                                    currentMonth = todayInfo.first
                                    selectedDay = todayInfo.second
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = PrimaryCobalt,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                userNavigatedCalendar = true
                                currentMonth = currentMonth.previous()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = {
                                userNavigatedCalendar = true
                                currentMonth = currentMonth.next()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

        // Month Summary Strip
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthStat(Modifier.weight(1f), "Active Days", "$activeDaysCount")
                    VerticalDivider(Modifier.height(28.dp), 0.75.dp, MaterialTheme.colorScheme.outline)
                    MonthStat(Modifier.weight(1f), "Green Days", "$greenDaysCount / $activeDaysCount")
                    VerticalDivider(Modifier.height(28.dp), 0.75.dp, MaterialTheme.colorScheme.outline)
                    MonthStat(Modifier.weight(1f), "Best Day", Formatters.compactInr(bestDayPnl, showSign = true))
                }
            }
        }

        // Monthly Heatmap Grid
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val blanks = IndianMarketCalendar.leadingBlanksMonFirst(currentMonth)
                    val cells: List<Int?> = List(blanks) { null } + (1..daysInMonth).toList()
                    val rows = cells.chunked(7)

                    rows.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until 7) {
                                val dayNum = week.getOrNull(i)
                                if (dayNum == null) {
                                    Spacer(modifier = Modifier.weight(1f).height(cellHeight))
                                } else {
                                    val dateKey = currentMonth.dateString(dayNum)
                                    val dayTrades = byDate[dateKey].orEmpty()
                                    val dayPnl = dayTrades.fold(BigDecimal.ZERO) { acc, t ->
                                        acc.add(tradeResults.firstOrNull { it.tradeId == t.id }?.netPnl ?: BigDecimal.ZERO)
                                    }
                                    val isHoliday = IndianMarketCalendar.holidays2026.containsKey(dateKey)
                                    val isWeekendDay = i >= 5
                                    val isTodayCell = dateKey == todayInfo.third
                                    val isSelected = selectedDay == dayNum

                                    val cellBg = when {
                                        dayTrades.isNotEmpty() && dayPnl >= BigDecimal.ZERO -> ProfitGreen.copy(alpha = 0.13f)
                                        dayTrades.isNotEmpty() && dayPnl < BigDecimal.ZERO -> LossRose.copy(alpha = 0.13f)
                                        isHoliday -> GoldCommodity.copy(alpha = 0.13f)
                                        isWeekendDay -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(cellHeight)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellBg)
                                            .then(
                                                when {
                                                    isSelected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(10.dp))
                                                    isTodayCell -> Modifier.border(1.dp, PrimaryCobalt.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                                    else -> Modifier
                                                }
                                            )
                                            .clickable {
                                                userNavigatedCalendar = true
                                                selectedDay = dayNum
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected || dayTrades.isNotEmpty() || isTodayCell) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isTodayCell && dayTrades.isEmpty() -> PrimaryCobalt
                                                        isWeekendDay && dayTrades.isEmpty() && !isHoliday -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                ),
                                                maxLines = 1
                                            )
                                            if (dayTrades.isNotEmpty()) {
                                                val compactLabel = Formatters.compactInr(dayPnl, showSign = true)
                                                    .let { if (!isWide) it.replace("₹", "") else it }
                                                Text(
                                                    text = compactLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontFamily = JetBrainsMonoFont,
                                                        color = if (dayPnl >= BigDecimal.ZERO) ProfitGreen else LossRose
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else if (isHoliday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(GoldCommodity)
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

        // Selected Day Activity Ledger
        val dayTrades = byDate[selectedDateStr].orEmpty()
        val holidayLabel = IndianMarketCalendar.holidays2026[selectedDateStr]
        val isSelectedWeekend = IndianMarketCalendar.isWeekend(currentMonth, selectedDay)
        val dayTotal = dayTrades.fold(BigDecimal.ZERO) { acc, t ->
            acc.add(tradeResults.firstOrNull { it.tradeId == t.id }?.netPnl ?: BigDecimal.ZERO)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${IndianMarketCalendar.shortMonthName(currentMonth.month)} $selectedDay, ${currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isViewingToday) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryCobalt.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryCobalt,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (dayTrades.isNotEmpty()) {
                    Text(
                        text = Formatters.inr(dayTotal, showSign = true),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFont,
                            fontWeight = FontWeight.Bold,
                            color = if (dayTotal >= BigDecimal.ZERO) ProfitGreen else LossRose
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        val marketStatusBanner = holidayLabel ?: if (isSelectedWeekend && dayTrades.isEmpty()) {
            "Weekend · NSE / BSE / MCX Closed"
        } else null

        if (marketStatusBanner != null) {
            item {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (holidayLabel != null) GoldCommodity else MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = marketStatusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            if (dayTrades.isEmpty()) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No trades logged on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    dayTrades.forEachIndexed { idx, trade ->
                        val res = tradeResults.first { it.tradeId == trade.id }
                        LedgerTradeRow(
                            trade = trade,
                            result = res,
                            onClick = { onTradeClick(trade) }
                        )
                        if (idx < dayTrades.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
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

@Composable
private fun MonthStat(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
