package com.example.engine

import com.example.model.Trade
import com.example.model.TradeResult
import com.example.model.TradeSide
import com.example.utils.Formatters
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class EquityDrawdownPoint(
    val date: String,
    val symbol: String,
    val cumulativePnl: BigDecimal,
    val drawdown: BigDecimal,
    val tradeNetPnl: BigDecimal
)

data class BucketMetric(
    val label: String,
    val count: Int,
    val wins: Int,
    val winRatePct: Int,
    val netPnl: BigDecimal,
    val avgPnl: BigDecimal
)

data class DistributionBucket(
    val label: String,
    val count: Int,
    val totalPnl: BigDecimal,
    val isPositive: Boolean,
    val isNeutral: Boolean = false
)

data class SequenceBehavior(
    val afterWin: BucketMetric,
    val afterLoss: BucketMetric,
    val afterTwoLosses: BucketMetric
)

data class ProcessSplit(
    val highProcess: BucketMetric,
    val lowProcess: BucketMetric
)

data class AnalyticsReport(
    val metrics: CalculationEngine.PortfolioMetrics,
    val totalGross: BigDecimal,
    val totalBrokerage: BigDecimal,
    val totalStatutoryTaxes: BigDecimal,
    val totalFees: BigDecimal,
    val feeDragPct: Int,
    val avgWin: BigDecimal,
    val avgLoss: BigDecimal,
    val payoffRatio: BigDecimal,
    val avgRMultiple: BigDecimal,
    val equityDrawdownSeries: List<EquityDrawdownPoint>,
    val distribution: List<DistributionBucket>,
    val emotionStats: List<BucketMetric>,
    val sequenceBehavior: SequenceBehavior,
    val processSplit: ProcessSplit,
    val mistakeStats: List<BucketMetric>,
    val weekdayStats: List<BucketMetric>,
    val segmentStats: List<BucketMetric>,
    val setupStats: List<BucketMetric>,
    val buyMetric: BucketMetric,
    val sellMetric: BucketMetric,
    val edgeTakeaway: String,
    val psychologyTakeaway: String
)

object AnalyticsEngine {

    private val HUNDRED = BigDecimal("100")

    fun buildReport(
        chronologicalTrades: List<Trade>,
        profile: BrokerRateProfile,
        matchingMethod: MatchingMethod
    ): AnalyticsReport {
        val pairs = chronologicalTrades.map { trade ->
            trade to CalculationEngine.calculateTradeResult(trade, profile, matchingMethod)
        }
        val results = pairs.map { it.second }
        val metrics = CalculationEngine.computeMetrics(results)

        val totalGross = results.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.grossPnl) }
        val totalBrokerage = results.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.charges.brokerage) }
        val totalFees = results.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.charges.totalCharges) }
        val totalStatutoryTaxes = totalFees.subtract(totalBrokerage).coerceAtLeast(BigDecimal.ZERO)

        val feeDragPct = if (totalGross > BigDecimal.ZERO) {
            totalFees.multiply(HUNDRED).divide(totalGross, 0, RoundingMode.HALF_UP).toInt().coerceIn(0, 999)
        } else if (totalFees > BigDecimal.ZERO && totalGross <= BigDecimal.ZERO) {
            100
        } else 0

        val winResults = results.filter { it.netPnl > BigDecimal.ZERO }
        val lossResults = results.filter { it.netPnl < BigDecimal.ZERO }

        val avgWin = if (winResults.isEmpty()) BigDecimal.ZERO else {
            winResults.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.netPnl) }
                .divide(BigDecimal.valueOf(winResults.size.toLong()), 0, RoundingMode.HALF_UP)
        }
        val avgLoss = if (lossResults.isEmpty()) BigDecimal.ZERO else {
            lossResults.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.netPnl.abs()) }
                .divide(BigDecimal.valueOf(lossResults.size.toLong()), 0, RoundingMode.HALF_UP)
        }
        val payoffRatio = when {
            avgLoss > BigDecimal.ZERO -> avgWin.divide(avgLoss, 2, RoundingMode.HALF_UP)
            avgWin > BigDecimal.ZERO -> BigDecimal("9.99")
            else -> BigDecimal.ZERO
        }

        val rMultiples = results.map { it.rMultiple }.filter { it.compareTo(BigDecimal.ZERO) != 0 }
        val avgRMultiple = if (rMultiples.isEmpty()) BigDecimal.ZERO else {
            rMultiples.fold(BigDecimal.ZERO) { acc, r -> acc.add(r) }
                .divide(BigDecimal.valueOf(rMultiples.size.toLong()), 2, RoundingMode.HALF_UP)
        }

        // Cumulative Equity & Underwater Drawdown series
        var running = BigDecimal.ZERO
        var peak = BigDecimal.ZERO
        val series = pairs.map { (trade, res) ->
            running = running.add(res.netPnl)
            if (running > peak) peak = running
            val dd = running.subtract(peak) // 0 or negative rupee drawdown from peak
            EquityDrawdownPoint(
                date = trade.date,
                symbol = trade.symbol,
                cumulativePnl = running.setScale(2, RoundingMode.HALF_UP),
                drawdown = dd.setScale(2, RoundingMode.HALF_UP),
                tradeNetPnl = res.netPnl
            )
        }

        val distribution = buildDistribution(results, avgWin, avgLoss)

        val emotionStats = pairs
            .groupBy { it.first.emotionBefore.ifBlank { "Untagged" } }
            .map { (label, items) -> toBucketMetric(label, items) }
            .sortedByDescending { it.netPnl }

        val sequenceBehavior = computeSequenceBehavior(pairs)

        val highProcessItems = pairs.filter { it.first.rating >= 4 }
        val lowProcessItems = pairs.filter { it.first.rating in 1..3 }
        val processSplit = ProcessSplit(
            highProcess = toBucketMetric("Disciplined (4-5★)", highProcessItems),
            lowProcess = toBucketMetric("Impulsive (1-3★)", lowProcessItems)
        )

        val mistakesMap = mutableMapOf<String, MutableList<Pair<Trade, TradeResult>>>()
        for (pair in pairs) {
            pair.first.mistakes.forEach { m ->
                mistakesMap.getOrPut(m) { mutableListOf() }.add(pair)
            }
        }
        val mistakeStats = mistakesMap
            .map { (label, items) -> toBucketMetric(label, items) }
            .sortedBy { it.netPnl }

        val weekdayStats = computeWeekdayStats(pairs)

        val segmentStats = pairs
            .groupBy { it.first.segment.displayName }
            .map { (label, items) -> toBucketMetric(label, items) }
            .sortedByDescending { it.netPnl }

        val setupStats = pairs
            .groupBy { it.first.setup.ifBlank { "Discretionary" } }
            .map { (label, items) -> toBucketMetric(label, items) }
            .sortedByDescending { it.netPnl }

        val buyMetric = toBucketMetric("Buy First", pairs.filter { it.first.side == TradeSide.BUY })
        val sellMetric = toBucketMetric("Sell First", pairs.filter { it.first.side == TradeSide.SELL })

        val edgeTakeaway = buildEdgeTakeaway(metrics, payoffRatio, feeDragPct)
        val psychologyTakeaway = buildPsychologyTakeaway(emotionStats, sequenceBehavior, processSplit, mistakeStats)

        return AnalyticsReport(
            metrics = metrics,
            totalGross = totalGross.setScale(2, RoundingMode.HALF_UP),
            totalBrokerage = totalBrokerage.setScale(2, RoundingMode.HALF_UP),
            totalStatutoryTaxes = totalStatutoryTaxes.setScale(2, RoundingMode.HALF_UP),
            totalFees = totalFees.setScale(2, RoundingMode.HALF_UP),
            feeDragPct = feeDragPct,
            avgWin = avgWin,
            avgLoss = avgLoss,
            payoffRatio = payoffRatio,
            avgRMultiple = avgRMultiple,
            equityDrawdownSeries = series,
            distribution = distribution,
            emotionStats = emotionStats,
            sequenceBehavior = sequenceBehavior,
            processSplit = processSplit,
            mistakeStats = mistakeStats,
            weekdayStats = weekdayStats,
            segmentStats = segmentStats,
            setupStats = setupStats,
            buyMetric = buyMetric,
            sellMetric = sellMetric,
            edgeTakeaway = edgeTakeaway,
            psychologyTakeaway = psychologyTakeaway
        )
    }

    private fun toBucketMetric(label: String, items: List<Pair<Trade, TradeResult>>): BucketMetric {
        if (items.isEmpty()) {
            return BucketMetric(label, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val wins = items.count { it.second.netPnl > BigDecimal.ZERO }
        val net = items.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.second.netPnl) }
            .setScale(2, RoundingMode.HALF_UP)
        val avg = net.divide(BigDecimal.valueOf(items.size.toLong()), 0, RoundingMode.HALF_UP)
        val winRatePct = (wins * 100) / items.size
        return BucketMetric(
            label = label,
            count = items.size,
            wins = wins,
            winRatePct = winRatePct,
            netPnl = net,
            avgPnl = avg
        )
    }

    private fun buildDistribution(
        results: List<TradeResult>,
        avgWin: BigDecimal,
        avgLoss: BigDecimal
    ): List<DistributionBucket> {
        val winThreshold = if (avgWin > BigDecimal.ZERO) avgWin.multiply(BigDecimal("1.25")) else BigDecimal("3000")
        val lossThreshold = if (avgLoss > BigDecimal.ZERO) avgLoss.multiply(BigDecimal("1.25")) else BigDecimal("3000")
        val flatBand = BigDecimal("250")

        var bigLoss = 0
        var bigLossSum = BigDecimal.ZERO
        var smallLoss = 0
        var smallLossSum = BigDecimal.ZERO
        var flat = 0
        var flatSum = BigDecimal.ZERO
        var smallWin = 0
        var smallWinSum = BigDecimal.ZERO
        var bigWin = 0
        var bigWinSum = BigDecimal.ZERO

        for (r in results) {
            val n = r.netPnl
            when {
                n.abs() <= flatBand -> {
                    flat++
                    flatSum = flatSum.add(n)
                }
                n < BigDecimal.ZERO && n.abs() >= lossThreshold -> {
                    bigLoss++
                    bigLossSum = bigLossSum.add(n)
                }
                n < BigDecimal.ZERO -> {
                    smallLoss++
                    smallLossSum = smallLossSum.add(n)
                }
                n >= winThreshold -> {
                    bigWin++
                    bigWinSum = bigWinSum.add(n)
                }
                else -> {
                    smallWin++
                    smallWinSum = smallWinSum.add(n)
                }
            }
        }

        return listOf(
            DistributionBucket("Big\nLoss", bigLoss, bigLossSum, isPositive = false),
            DistributionBucket("Small\nLoss", smallLoss, smallLossSum, isPositive = false),
            DistributionBucket("Flat", flat, flatSum, isPositive = true, isNeutral = true),
            DistributionBucket("Small\nWin", smallWin, smallWinSum, isPositive = true),
            DistributionBucket("Big\nWin", bigWin, bigWinSum, isPositive = true)
        )
    }

    private fun computeSequenceBehavior(pairs: List<Pair<Trade, TradeResult>>): SequenceBehavior {
        val afterWin = mutableListOf<Pair<Trade, TradeResult>>()
        val afterLoss = mutableListOf<Pair<Trade, TradeResult>>()
        val afterTwoLosses = mutableListOf<Pair<Trade, TradeResult>>()

        var consecutiveLosses = 0
        var lastWasWin: Boolean? = null

        for (pair in pairs) {
            if (lastWasWin == true) {
                afterWin.add(pair)
            } else if (lastWasWin == false) {
                afterLoss.add(pair)
                if (consecutiveLosses >= 2) {
                    afterTwoLosses.add(pair)
                }
            }

            val net = pair.second.netPnl
            if (net > BigDecimal.ZERO) {
                lastWasWin = true
                consecutiveLosses = 0
            } else if (net < BigDecimal.ZERO) {
                lastWasWin = false
                consecutiveLosses++
            }
        }

        return SequenceBehavior(
            afterWin = toBucketMetric("After a Win", afterWin),
            afterLoss = toBucketMetric("After a Loss", afterLoss),
            afterTwoLosses = toBucketMetric("After 2+ Losses", afterTwoLosses)
        )
    }

    private fun computeWeekdayStats(pairs: List<Pair<Trade, TradeResult>>): List<BucketMetric> {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ist }
        val cal = Calendar.getInstance(ist)

        val buckets = linkedMapOf(
            Calendar.MONDAY to mutableListOf<Pair<Trade, TradeResult>>(),
            Calendar.TUESDAY to mutableListOf(),
            Calendar.WEDNESDAY to mutableListOf(),
            Calendar.THURSDAY to mutableListOf(),
            Calendar.FRIDAY to mutableListOf(),
            Calendar.SATURDAY to mutableListOf(),
            Calendar.SUNDAY to mutableListOf()
        )

        for (pair in pairs) {
            val d = runCatching { sdf.parse(pair.first.date) }.getOrNull() ?: continue
            cal.time = d
            buckets[cal.get(Calendar.DAY_OF_WEEK)]?.add(pair)
        }

        val labels = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )

        return listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        ).plus(
            listOf(Calendar.SATURDAY, Calendar.SUNDAY).filter { (buckets[it]?.size ?: 0) > 0 }
        ).map { day ->
            toBucketMetric(labels[day].orEmpty(), buckets[day].orEmpty())
        }
    }

    private fun buildEdgeTakeaway(
        metrics: CalculationEngine.PortfolioMetrics,
        payoffRatio: BigDecimal,
        feeDragPct: Int
    ): String {
        if (metrics.totalTrades == 0) {
            return "Log a few trades to see your win rate, profit curve, and fee impact."
        }
        return when {
            metrics.totalNetPnl > BigDecimal.ZERO && feeDragPct >= 25 ->
                "Estimated brokerage and taxes equal $feeDragPct% of gross P&L across these entries."
            metrics.expectancy > BigDecimal.ZERO && payoffRatio >= BigDecimal("1.50") ->
                "Recorded payoff ratio is ${payoffRatio}x (average win vs. average loss across ${metrics.totalTrades} trades)."
            metrics.expectancy > BigDecimal.ZERO ->
                "Recorded win rate is ${metrics.winRate}% with positive net expectancy across ${metrics.totalTrades} trades."
            metrics.losingTrades > 0 && payoffRatio < BigDecimal("0.85") ->
                "Recorded average loss exceeds average win across ${metrics.totalTrades} logged entries."
            else ->
                "Recorded net P&L is negative across ${metrics.totalTrades} trades in this period."
        }
    }

    private fun buildPsychologyTakeaway(
        emotions: List<BucketMetric>,
        sequence: SequenceBehavior,
        process: ProcessSplit,
        mistakes: List<BucketMetric>
    ): String {
        val worstMistake = mistakes.firstOrNull { it.netPnl < BigDecimal.ZERO }
        val worstEmotion = emotions.lastOrNull { it.netPnl < BigDecimal.ZERO }
        val bestEmotion = emotions.firstOrNull { it.netPnl > BigDecimal.ZERO }

        return when {
            sequence.afterTwoLosses.count >= 2 && sequence.afterTwoLosses.netPnl < BigDecimal.ZERO ->
                "In your log, win rate after 2 consecutive losses is ${sequence.afterTwoLosses.winRatePct}% (${sequence.afterTwoLosses.count} trades)."
            worstEmotion != null && bestEmotion != null ->
                "Recorded win rate is ${bestEmotion.winRatePct}% when tagged '${bestEmotion.label}', versus ${worstEmotion.winRatePct}% when '${worstEmotion.label}'."
            worstMistake != null ->
                "Tag '${worstMistake.label}' appears ${worstMistake.count} times in your log (${Formatters.inr(worstMistake.netPnl)} net)."
            process.highProcess.count > 0 && process.highProcess.netPnl > process.lowProcess.netPnl ->
                "Entries rated 4–5★ recorded higher cumulative net P&L than 1–3★ entries in your log."
            else ->
                "Tag mood and discipline rating on entries to summarize historical patterns in your journal."
        }
    }
}
