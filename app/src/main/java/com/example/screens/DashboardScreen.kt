package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BrokerRateProfile
import com.example.engine.CalculationEngine
import com.example.engine.MatchingMethod
import com.example.model.Trade
import com.example.ui.components.EditorialCard
import com.example.ui.components.EquityCurveChart
import com.example.ui.components.LedgerTradeRow
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    trades: List<Trade>,
    brokerProfile: BrokerRateProfile,
    matchingMethod: MatchingMethod = MatchingMethod.FIFO,
    traderName: String,
    effectiveCapital: BigDecimal,
    dailyLossLimit: BigDecimal,
    onViewAllClick: () -> Unit,
    onAddTradeClick: () -> Unit,
    onTradeClick: (Trade) -> Unit
) {
    var timeframe by remember { mutableStateOf("1M") }
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    val filteredTrades = remember(trades, timeframe) {
        filterTradesByPeriod(trades, timeframe)
    }

    val tradeResults = remember(filteredTrades, brokerProfile, matchingMethod) {
        filteredTrades.map { CalculationEngine.calculateTradeResult(it, brokerProfile, matchingMethod) }
    }

    // Oldest -> newest chronological points for curve, scrubbing, streak, and max drawdown
    val chronological = remember(filteredTrades, tradeResults) {
        filteredTrades.reversed().map { trade ->
            val res = tradeResults.first { it.tradeId == trade.id }
            trade to res
        }
    }
    val metrics = remember(chronological) {
        CalculationEngine.computeMetrics(chronological.map { it.second })
    }

    val cumulativeSeries = remember(chronological) {
        var running = BigDecimal.ZERO
        chronological.map { (trade, res) ->
            running = running.add(res.netPnl)
            Triple(trade, running, res.netPnl)
        }
    }

    val equityPoints = remember(cumulativeSeries) {
        cumulativeSeries.map { it.second.toFloat() }
    }

    val activeScrub = scrubbedIndex?.let { cumulativeSeries.getOrNull(it) }
    val heroAmount = activeScrub?.second ?: metrics.totalNetPnl
    val isProfit = heroAmount >= BigDecimal.ZERO
    val accent = if (isProfit) ProfitGreen else LossRose
    val returnPct = if (effectiveCapital > BigDecimal.ZERO) {
        heroAmount
            .multiply(BigDecimal("100"))
            .divide(effectiveCapital, 2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        }.format(java.util.Date())
    }
    val todayGuardrail = remember(trades, brokerProfile, matchingMethod, dailyLossLimit, todayIso) {
        val todayResults = trades
            .filter { it.date == todayIso }
            .reversed()
            .map { CalculationEngine.calculateTradeResult(it, brokerProfile, matchingMethod) }
        CalculationEngine.evaluateDailyRiskGuardrail(todayResults, dailyLossLimit)
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        // Editorial Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val istGreeting = remember {
                    val hour = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata")).get(Calendar.HOUR_OF_DAY)
                    when {
                        hour < 12 -> "Good morning"
                        hour < 17 -> "Good afternoon"
                        else -> "Good evening"
                    }
                }
                val firstName = traderName.trim().substringBefore(" ")
                val greetingLine = if (firstName.isNotEmpty()) "$istGreeting, $firstName" else istGreeting
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = greetingLine,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Portfolio",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ProfitGreen)
                        )
                        Text(
                            text = "Capital ${Formatters.compactInr(effectiveCapital)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFont,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Canvas Hero P&L + Interactive Scrubbable Curve
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (activeScrub != null) {
                            "${activeScrub.first.date} · ${activeScrub.first.symbol}"
                        } else {
                            "Net Return"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = Formatters.inr(heroAmount, showSign = true),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = if (isNarrow) 30.sp else 34.sp,
                                fontFamily = JetBrainsMonoFont,
                                letterSpacing = (-1.1).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("hero_net_pnl")
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = accent.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${if (returnPct >= BigDecimal.ZERO) "+" else ""}$returnPct%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFont,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = if (activeScrub != null) {
                            "Trade net: ${Formatters.inr(activeScrub.third, showSign = true)}"
                        } else if (equityPoints.isEmpty()) {
                            "After ${brokerProfile.brokerName} fees · Ready for your first trade"
                        } else {
                            "Touch chart to inspect · After ${brokerProfile.brokerName} fees"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (equityPoints.isNotEmpty()) {
                    EquityCurveChart(
                        points = equityPoints,
                        selectedIndex = scrubbedIndex,
                        onPointSelected = { scrubbedIndex = it }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log a trade to plot your equity curve",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1W, 1M, 6M, 1Y, ALL timeframe tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1W", "1M", "6M", "1Y", "ALL").forEach { key ->
                        val selected = timeframe == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    timeframe = key
                                    scrubbedIndex = null
                                }
                                .testTag("timeframe_$key"),
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
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stage 8: Daily Risk Guardrail Card (only surfaces when active today with loss/streak)
        if (todayGuardrail.todayTradesCount > 0 &&
            (todayGuardrail.level != CalculationEngine.GuardrailLevel.CALM || todayGuardrail.todayNetPnl < BigDecimal.ZERO)
        ) {
            item(key = "daily_guardrail_card") {
                val warnColor = when (todayGuardrail.level) {
                    CalculationEngine.GuardrailLevel.BREACHED -> LossRose
                    CalculationEngine.GuardrailLevel.CAUTION -> GoldCommodity
                    CalculationEngine.GuardrailLevel.CALM -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val title = when (todayGuardrail.level) {
                    CalculationEngine.GuardrailLevel.BREACHED -> "Daily Loss Limit Reached"
                    CalculationEngine.GuardrailLevel.CAUTION -> "Daily Limit Notice"
                    CalculationEngine.GuardrailLevel.CALM -> "Daily Limit Tracker"
                }
                val message = when {
                    todayGuardrail.level == CalculationEngine.GuardrailLevel.BREACHED ->
                        "Today's logged net P&L is ${Formatters.inr(todayGuardrail.todayNetPnl)} (${todayGuardrail.limitUsedPct}% of configured daily limit)."
                    todayGuardrail.trailingLosses >= 2 ->
                        "${todayGuardrail.trailingLosses} consecutive losing entries logged today (${todayGuardrail.limitUsedPct}% of configured daily limit)."
                    else ->
                        "${Formatters.inr(todayGuardrail.lossAmount)} of ${Formatters.compactInr(dailyLossLimit)} configured daily limit logged (${todayGuardrail.limitUsedPct}%)."
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = warnColor.copy(alpha = 0.08f),
                    border = BorderStroke(0.75.dp, warnColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = warnColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFont,
                                    fontWeight = FontWeight.SemiBold,
                                    color = warnColor
                                )
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Unified 3-column Bento Metric Strip with Vertical Hairline Dividers
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RibbonStat(
                        modifier = Modifier.weight(1f),
                        label = "Win Rate",
                        value = "${metrics.winRate}%",
                        sub = "${metrics.winningTrades}W · ${metrics.losingTrades}L"
                    )
                    VerticalDivider(
                        modifier = Modifier.height(36.dp),
                        thickness = 0.75.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    RibbonStat(
                        modifier = Modifier.weight(1f),
                        label = "Gain / Loss",
                        value = "${metrics.profitFactor}x",
                        sub = "Profit ratio"
                    )
                    VerticalDivider(
                        modifier = Modifier.height(36.dp),
                        thickness = 0.75.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    RibbonStat(
                        modifier = Modifier.weight(1f),
                        label = "Avg / Trade",
                        value = Formatters.compactInr(metrics.expectancy, showSign = true),
                        sub = "${metrics.totalTrades} trades"
                    )
                }
            }
        }

        // Asymmetric Highlights Row (Equal Height)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorialCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Best Trade",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = Formatters.inr(metrics.bestTrade, showSign = true),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val streakText = when {
                            metrics.currentStreak > 0 -> "${metrics.currentStreak} wins in a row"
                            metrics.currentStreak < 0 -> "${kotlin.math.abs(metrics.currentStreak)} losses in a row"
                            else -> "No active run"
                        }
                        Text(
                            text = streakText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                EditorialCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Largest Drop",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = Formatters.inr(metrics.maxDrawdown),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.maxDrawdown > BigDecimal.ZERO) LossRose else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Daily cap ${Formatters.compactInr(dailyLossLimit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Recent Trades Unified Ledger Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Trades",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onViewAllClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryCobalt,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        if (filteredTrades.isEmpty()) {
            item {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoGraph,
                            contentDescription = null,
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "No trades logged yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap + below to record your first executed trade in your local diary.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onAddTradeClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Text("Log First Trade", color = MaterialTheme.colorScheme.background)
                        }
                    }
                }
            }
        } else {
            item {
                val recent = filteredTrades.take(5)
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    recent.forEachIndexed { index, trade ->
                        val res = tradeResults.first { it.tradeId == trade.id }
                        LedgerTradeRow(
                            trade = trade,
                            result = res,
                            onClick = { onTradeClick(trade) }
                        )
                        if (index < recent.lastIndex) {
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

internal fun filterTradesByPeriod(trades: List<Trade>, period: String): List<Trade> {
    if (period == "ALL" || trades.isEmpty()) return trades
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val anchorDate = trades.mapNotNull { runCatching { sdf.parse(it.date) }.getOrNull() }.maxOrNull()
        ?: return trades
    val cal = Calendar.getInstance().apply {
        time = anchorDate
        when (period) {
            "1W" -> add(Calendar.DAY_OF_YEAR, -7)
            "1M" -> add(Calendar.MONTH, -1)
            "6M" -> add(Calendar.MONTH, -6)
            "1Y" -> add(Calendar.YEAR, -1)
        }
    }
    val cutoff = cal.time
    return trades.filter { trade ->
        val d = runCatching { sdf.parse(trade.date) }.getOrNull()
        d == null || !d.before(cutoff)
    }
}

@Composable
private fun RibbonStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
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
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
