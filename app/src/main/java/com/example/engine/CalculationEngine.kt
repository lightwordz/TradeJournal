package com.example.engine

import com.example.model.ChargeBreakdown
import com.example.model.Exchange
import com.example.model.OrderFill
import com.example.model.Segment
import com.example.model.Trade
import com.example.model.TradeResult
import com.example.model.TradeSide
import java.math.BigDecimal
import java.math.RoundingMode

enum class MatchingMethod {
    FIFO,
    WEIGHTED_AVG
}

data class BrokerRateProfile(
    val brokerName: String,
    val effectiveDate: String = "2026-01-01",
    val eqDeliveryBrokeragePct: BigDecimal = BigDecimal.ZERO,
    val eqDeliveryFlatMax: BigDecimal = BigDecimal.ZERO,
    val eqIntradayBrokeragePct: BigDecimal = BigDecimal("0.0003"),
    val eqIntradayFlatPerOrder: BigDecimal = BigDecimal("20.00"),
    val fnoFuturesFlatPerOrder: BigDecimal = BigDecimal("20.00"),
    val fnoOptionsFlatPerOrder: BigDecimal = BigDecimal("20.00"),
    val commodityFlatPerOrder: BigDecimal = BigDecimal("20.00"),

    // STT / CTT
    val sttEqDeliveryBuy: BigDecimal = BigDecimal("0.001"),
    val sttEqDeliverySell: BigDecimal = BigDecimal("0.001"),
    val sttEqIntradaySell: BigDecimal = BigDecimal("0.00025"),
    val sttFnoFuturesSell: BigDecimal = BigDecimal("0.0002"),
    val sttFnoOptionsSell: BigDecimal = BigDecimal("0.001"),
    val sttCommoditySell: BigDecimal = BigDecimal("0.0001"),

    // Exchange transaction charges
    val exchangeTxnRateEq: BigDecimal = BigDecimal("0.0000325"),
    val exchangeTxnRateFno: BigDecimal = BigDecimal("0.000019"),
    val exchangeTxnRateCommodity: BigDecimal = BigDecimal("0.000002"),

    // SEBI turnover fee
    val sebiFeeRate: BigDecimal = BigDecimal("0.000001"),

    // Stamp duty (buy side)
    val stampDutyDelivery: BigDecimal = BigDecimal("0.00015"),
    val stampDutyIntraday: BigDecimal = BigDecimal("0.00003"),
    val stampDutyFutures: BigDecimal = BigDecimal("0.00002"),
    val stampDutyOptions: BigDecimal = BigDecimal("0.00003"),

    // DP charges on delivery sell
    val dpChargesPerSell: BigDecimal = BigDecimal("15.93")
) {
    companion object {
        fun forBroker(brokerName: String): BrokerRateProfile {
            return when (brokerName.lowercase()) {
                "upstox" -> BrokerRateProfile(
                    brokerName = "Upstox",
                    eqDeliveryBrokeragePct = BigDecimal("0.025"),
                    eqDeliveryFlatMax = BigDecimal("20.00"),
                    dpChargesPerSell = BigDecimal("18.50")
                )
                "groww" -> BrokerRateProfile(
                    brokerName = "Groww",
                    eqDeliveryBrokeragePct = BigDecimal("0.001"),
                    eqDeliveryFlatMax = BigDecimal("20.00"),
                    dpChargesPerSell = BigDecimal("16.52")
                )
                "angel one" -> BrokerRateProfile(
                    brokerName = "Angel One",
                    eqDeliveryBrokeragePct = BigDecimal.ZERO,
                    eqDeliveryFlatMax = BigDecimal.ZERO,
                    dpChargesPerSell = BigDecimal("20.00")
                )
                else -> BrokerRateProfile(brokerName = "Zerodha")
            }
        }
    }
}

object DefaultLotSizes {
    fun seeded(): Map<String, Int> = linkedMapOf(
        "NIFTY 50" to 165,
        "SENSEX" to 20,
        "BANKNIFTY" to 30,
        "FINNIFTY" to 65,
        "MIDCPNIFTY" to 120,
        "GOLDM" to 10,
        "CRUDEOILM" to 10
    )

    fun isCommodity(ticker: String): Boolean {
        val upper = ticker.uppercase()
        return upper.startsWith("GOLD") || upper.startsWith("SILVER") ||
            upper.startsWith("CRUDE") || upper.startsWith("NATURALGAS") ||
            upper.startsWith("COPPER") || upper.startsWith("ZINC")
    }

    fun computeQuantity(lots: Int, lotSize: Int): Int =
        lots.coerceAtLeast(1) * lotSize.coerceAtLeast(1)
}

object CalculationEngine {

    val defaultBrokerProfile = BrokerRateProfile(brokerName = "Zerodha")

    private val HUNDRED = BigDecimal("100")

    fun toPaise(rupees: BigDecimal): Long =
        rupees.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact()

    fun fromPaise(paise: Long): BigDecimal =
        BigDecimal.valueOf(paise).divide(HUNDRED, 2, RoundingMode.HALF_UP)

    fun calculateTradeResult(
        trade: Trade,
        profile: BrokerRateProfile = defaultBrokerProfile,
        matchingMethod: MatchingMethod = MatchingMethod.FIFO
    ): TradeResult {
        val orders = trade.orders
        val entryOrders: List<OrderFill>
        val exitOrders: List<OrderFill>

        if (orders.size >= 2) {
            val splitIndex = (orders.size + 1) / 2
            entryOrders = orders.take(splitIndex)
            exitOrders = orders.drop(splitIndex)
        } else {
            entryOrders = orders
            exitOrders = emptyList()
        }

        val entryQty = entryOrders.sumOf { it.qty }
        val exitQty = exitOrders.sumOf { it.qty }
        val matchedQty = if (exitQty > 0L) minOf(entryQty, exitQty) else 0L

        val avgEntryPrice = weightedAvgPrice(entryOrders)
        val avgExitPrice = if (exitOrders.isNotEmpty()) weightedAvgPrice(exitOrders) else avgEntryPrice

        val grossPnl = if (matchedQty == 0L) {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        } else if (matchingMethod == MatchingMethod.FIFO) {
            computeFifoGrossPnl(entryOrders, exitOrders, trade.multiplier, trade.side)
        } else {
            val sideSign = if (trade.side == TradeSide.BUY) BigDecimal.ONE else BigDecimal.ONE.negate()
            avgExitPrice.subtract(avgEntryPrice)
                .multiply(BigDecimal.valueOf(matchedQty))
                .multiply(trade.multiplier)
                .multiply(sideSign)
                .setScale(2, RoundingMode.HALF_UP)
        }

        val effectiveQty = BigDecimal.valueOf(
            if (matchedQty > 0L) matchedQty else (trade.lots.toLong() * trade.lotSize.toLong()).coerceAtLeast(1L)
        )

        val buyValue = if (trade.side == TradeSide.BUY) {
            avgEntryPrice.multiply(effectiveQty).multiply(trade.multiplier)
        } else {
            avgExitPrice.multiply(effectiveQty).multiply(trade.multiplier)
        }

        val sellValue = if (trade.side == TradeSide.BUY) {
            avgExitPrice.multiply(effectiveQty).multiply(trade.multiplier)
        } else {
            avgEntryPrice.multiply(effectiveQty).multiply(trade.multiplier)
        }

        val turnover = buyValue.add(sellValue)
        val charges = calculateCharges(
            trade = trade,
            buyValue = buyValue,
            sellValue = sellValue,
            turnover = turnover,
            ordersCount = orders.size.coerceAtLeast(1),
            profile = profile
        )

        val netPnl = grossPnl.subtract(charges.totalCharges).setScale(2, RoundingMode.HALF_UP)

        val rMultiple = if (trade.plannedRisk > BigDecimal.ZERO) {
            netPnl.divide(trade.plannedRisk, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val riskReward = if (trade.stopLoss > BigDecimal.ZERO && trade.target > BigDecimal.ZERO) {
            val risk = avgEntryPrice.subtract(trade.stopLoss).abs()
            val reward = trade.target.subtract(avgEntryPrice).abs()
            if (risk > BigDecimal.ZERO) reward.divide(risk, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }

        return TradeResult(
            tradeId = trade.id,
            grossPnl = grossPnl,
            charges = charges,
            netPnl = netPnl,
            rMultiple = rMultiple,
            riskReward = riskReward
        )
    }

    private fun weightedAvgPrice(fills: List<OrderFill>): BigDecimal {
        val totalQty = fills.sumOf { it.qty }
        if (totalQty <= 0L) return BigDecimal.ZERO
        val totalValue = fills.fold(BigDecimal.ZERO) { acc, fill ->
            acc.add(fill.price.multiply(BigDecimal.valueOf(fill.qty)))
        }
        return totalValue.divide(BigDecimal.valueOf(totalQty), 6, RoundingMode.HALF_UP)
    }

    private fun computeFifoGrossPnl(
        entryOrders: List<OrderFill>,
        exitOrders: List<OrderFill>,
        multiplier: BigDecimal,
        side: TradeSide
    ): BigDecimal {
        val remainingEntries = entryOrders.map { it.qty to it.price }.toMutableList()
        var entryIdx = 0
        var totalGross = BigDecimal.ZERO
        val sideSign = if (side == TradeSide.BUY) BigDecimal.ONE else BigDecimal.ONE.negate()

        for (exit in exitOrders) {
            var qtyToClose = exit.qty
            while (qtyToClose > 0L && entryIdx < remainingEntries.size) {
                val (openQty, openPrice) = remainingEntries[entryIdx]
                if (openQty <= 0L) {
                    entryIdx++
                    continue
                }
                val matched = minOf(qtyToClose, openQty)
                val legPnl = exit.price.subtract(openPrice)
                    .multiply(BigDecimal.valueOf(matched))
                    .multiply(multiplier)
                    .multiply(sideSign)
                totalGross = totalGross.add(legPnl)

                val leftover = openQty - matched
                remainingEntries[entryIdx] = leftover to openPrice
                if (leftover == 0L) entryIdx++
                qtyToClose -= matched
            }
        }
        return totalGross.setScale(2, RoundingMode.HALF_UP)
    }

    private fun calculateCharges(
        trade: Trade,
        buyValue: BigDecimal,
        sellValue: BigDecimal,
        turnover: BigDecimal,
        ordersCount: Int,
        profile: BrokerRateProfile
    ): ChargeBreakdown {
        val brokerage = when (trade.segment) {
            Segment.EQ_DELIVERY -> {
                if (profile.eqDeliveryBrokeragePct > BigDecimal.ZERO) {
                    val pctFee = turnover.multiply(profile.eqDeliveryBrokeragePct)
                    val cap = profile.eqDeliveryFlatMax.multiply(BigDecimal.valueOf(ordersCount.toLong()))
                    if (cap > BigDecimal.ZERO) pctFee.min(cap) else pctFee
                } else BigDecimal.ZERO
            }
            Segment.EQ_INTRADAY -> {
                val pctFee = turnover.multiply(profile.eqIntradayBrokeragePct)
                val cap = profile.eqIntradayFlatPerOrder.multiply(BigDecimal.valueOf(ordersCount.toLong()))
                pctFee.min(cap)
            }
            Segment.FUT -> profile.fnoFuturesFlatPerOrder.multiply(BigDecimal.valueOf(ordersCount.toLong()))
            Segment.OPT -> profile.fnoOptionsFlatPerOrder.multiply(BigDecimal.valueOf(ordersCount.toLong()))
            Segment.COMMODITY_FUT, Segment.COMMODITY_OPT ->
                profile.commodityFlatPerOrder.multiply(BigDecimal.valueOf(ordersCount.toLong()))
        }.setScale(2, RoundingMode.HALF_UP)

        val stt = when (trade.segment) {
            Segment.EQ_DELIVERY -> buyValue.multiply(profile.sttEqDeliveryBuy)
                .add(sellValue.multiply(profile.sttEqDeliverySell))
            Segment.EQ_INTRADAY -> sellValue.multiply(profile.sttEqIntradaySell)
            Segment.FUT -> sellValue.multiply(profile.sttFnoFuturesSell)
            Segment.OPT -> sellValue.multiply(profile.sttFnoOptionsSell)
            Segment.COMMODITY_FUT, Segment.COMMODITY_OPT -> sellValue.multiply(profile.sttCommoditySell)
        }.setScale(2, RoundingMode.HALF_UP)

        val exchangeRate = when (trade.exchange) {
            Exchange.MCX -> profile.exchangeTxnRateCommodity
            else -> if (trade.segment == Segment.EQ_INTRADAY || trade.segment == Segment.EQ_DELIVERY) {
                profile.exchangeTxnRateEq
            } else {
                profile.exchangeTxnRateFno
            }
        }
        val exchangeCharges = turnover.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP)
        val sebiFee = turnover.multiply(profile.sebiFeeRate).setScale(2, RoundingMode.HALF_UP)

        val stampDutyRate = when (trade.segment) {
            Segment.EQ_DELIVERY -> profile.stampDutyDelivery
            Segment.EQ_INTRADAY -> profile.stampDutyIntraday
            Segment.FUT -> profile.stampDutyFutures
            else -> profile.stampDutyOptions
        }
        val stampDuty = buyValue.multiply(stampDutyRate).setScale(2, RoundingMode.HALF_UP)

        val gst = brokerage.add(exchangeCharges).add(sebiFee)
            .multiply(BigDecimal("0.18"))
            .setScale(2, RoundingMode.HALF_UP)

        val dpCharges = if (trade.segment == Segment.EQ_DELIVERY && sellValue > BigDecimal.ZERO) {
            profile.dpChargesPerSell
        } else {
            BigDecimal.ZERO
        }.setScale(2, RoundingMode.HALF_UP)

        val totalCharges = brokerage.add(stt).add(exchangeCharges).add(sebiFee)
            .add(stampDuty).add(gst).add(dpCharges)
            .setScale(2, RoundingMode.HALF_UP)

        return ChargeBreakdown(
            brokerage = brokerage,
            stt = stt,
            exchangeCharges = exchangeCharges,
            sebiFee = sebiFee,
            stampDuty = stampDuty,
            gst = gst,
            dpCharges = dpCharges,
            totalCharges = totalCharges
        )
    }

    data class PortfolioMetrics(
        val totalNetPnl: BigDecimal,
        val winRate: BigDecimal,
        val profitFactor: BigDecimal,
        val expectancy: BigDecimal,
        val maxDrawdown: BigDecimal,
        val totalTrades: Int,
        val winningTrades: Int,
        val losingTrades: Int,
        val bestTrade: BigDecimal,
        val worstTrade: BigDecimal,
        val currentStreak: Int
    )

    fun computeMetrics(results: List<TradeResult>): PortfolioMetrics {
        if (results.isEmpty()) {
            return PortfolioMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0
            )
        }

        var wins = 0
        var losses = 0
        var grossWins = BigDecimal.ZERO
        var grossLosses = BigDecimal.ZERO
        var best = BigDecimal.ZERO
        var worst = BigDecimal.ZERO
        var streakCount = 0
        var lastWasWin: Boolean? = null

        val netPnls = results.map { it.netPnl }
        val totalNet = netPnls.fold(BigDecimal.ZERO) { acc, b -> acc.add(b) }

        for (res in results) {
            if (res.netPnl > BigDecimal.ZERO) {
                wins++
                grossWins = grossWins.add(res.netPnl)
                if (res.netPnl > best) best = res.netPnl
                if (lastWasWin == true) streakCount++ else {
                    streakCount = 1
                    lastWasWin = true
                }
            } else if (res.netPnl < BigDecimal.ZERO) {
                losses++
                grossLosses = grossLosses.add(res.netPnl.abs())
                if (res.netPnl < worst) worst = res.netPnl
                if (lastWasWin == false) streakCount++ else {
                    streakCount = 1
                    lastWasWin = false
                }
            }
        }

        val currentStreak = when (lastWasWin) {
            true -> streakCount
            false -> -streakCount
            null -> 0
        }

        val count = BigDecimal.valueOf(results.size.toLong())
        val winRate = BigDecimal.valueOf(wins.toLong())
            .divide(count, 4, RoundingMode.HALF_UP)
            .multiply(HUNDRED)
            .setScale(1, RoundingMode.HALF_UP)

        val profitFactor = if (grossLosses > BigDecimal.ZERO) {
            grossWins.divide(grossLosses, 2, RoundingMode.HALF_UP)
        } else if (grossWins > BigDecimal.ZERO) {
            BigDecimal("99.99")
        } else {
            BigDecimal.ZERO
        }

        val winProb = BigDecimal.valueOf(wins.toLong()).divide(count, 4, RoundingMode.HALF_UP)
        val lossProb = BigDecimal.valueOf(losses.toLong()).divide(count, 4, RoundingMode.HALF_UP)
        val avgWin = if (wins > 0) grossWins.divide(BigDecimal.valueOf(wins.toLong()), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val avgLoss = if (losses > 0) grossLosses.divide(BigDecimal.valueOf(losses.toLong()), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val expectancy = winProb.multiply(avgWin).subtract(lossProb.multiply(avgLoss)).setScale(2, RoundingMode.HALF_UP)

        var peak = BigDecimal.ZERO
        var maxDd = BigDecimal.ZERO
        var runningEquity = BigDecimal.ZERO
        for (pnl in netPnls) {
            runningEquity = runningEquity.add(pnl)
            if (runningEquity > peak) peak = runningEquity
            val dd = peak.subtract(runningEquity)
            if (dd > maxDd) maxDd = dd
        }

        return PortfolioMetrics(
            totalNetPnl = totalNet.setScale(2, RoundingMode.HALF_UP),
            winRate = winRate,
            profitFactor = profitFactor,
            expectancy = expectancy,
            maxDrawdown = maxDd.setScale(2, RoundingMode.HALF_UP),
            totalTrades = results.size,
            winningTrades = wins,
            losingTrades = losses,
            bestTrade = best.setScale(2, RoundingMode.HALF_UP),
            worstTrade = worst.setScale(2, RoundingMode.HALF_UP),
            currentStreak = currentStreak
        )
    }

    enum class GuardrailLevel {
        CALM,
        CAUTION,
        BREACHED
    }

    data class DailyRiskGuardrail(
        val todayNetPnl: BigDecimal,
        val todayTradesCount: Int,
        val lossAmount: BigDecimal,
        val limitUsedPct: Int,
        val trailingLosses: Int,
        val level: GuardrailLevel
    )

    fun evaluateDailyRiskGuardrail(
        todayResultsChronological: List<TradeResult>,
        dailyLossLimit: BigDecimal
    ): DailyRiskGuardrail {
        if (todayResultsChronological.isEmpty()) {
            return DailyRiskGuardrail(
                todayNetPnl = BigDecimal.ZERO,
                todayTradesCount = 0,
                lossAmount = BigDecimal.ZERO,
                limitUsedPct = 0,
                trailingLosses = 0,
                level = GuardrailLevel.CALM
            )
        }
        val todayNet = todayResultsChronological.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.netPnl) }
        val lossAmt = if (todayNet < BigDecimal.ZERO) todayNet.abs() else BigDecimal.ZERO
        val limitUsedPct = if (dailyLossLimit > BigDecimal.ZERO && lossAmt > BigDecimal.ZERO) {
            lossAmt.multiply(HUNDRED)
                .divide(dailyLossLimit, 0, RoundingMode.HALF_UP)
                .toInt()
                .coerceAtLeast(0)
        } else 0

        var trailingLosses = 0
        for (res in todayResultsChronological.asReversed()) {
            if (res.netPnl < BigDecimal.ZERO) trailingLosses++ else break
        }

        val level = when {
            dailyLossLimit > BigDecimal.ZERO && lossAmt >= dailyLossLimit -> GuardrailLevel.BREACHED
            limitUsedPct >= 60 || trailingLosses >= 2 -> GuardrailLevel.CAUTION
            else -> GuardrailLevel.CALM
        }

        return DailyRiskGuardrail(
            todayNetPnl = todayNet.setScale(2, RoundingMode.HALF_UP),
            todayTradesCount = todayResultsChronological.size,
            lossAmount = lossAmt.setScale(2, RoundingMode.HALF_UP),
            limitUsedPct = limitUsedPct,
            trailingLosses = trailingLosses,
            level = level
        )
    }

    data class RiskQuantityFormula(
        val maxRiskRupees: BigDecimal,
        val riskPerUnit: BigDecimal,
        val calculatedLots: Int,
        val calculatedQty: Int,
        val estimatedRiskRupees: BigDecimal
    )

    fun calculateRiskQuantityFormula(
        capital: BigDecimal,
        riskPercent: BigDecimal,
        entryPrice: BigDecimal,
        stopLossPrice: BigDecimal,
        lotSize: Int
    ): RiskQuantityFormula? {
        if (capital <= BigDecimal.ZERO || riskPercent <= BigDecimal.ZERO || lotSize <= 0) return null
        val riskPerUnit = entryPrice.subtract(stopLossPrice).abs()
        if (riskPerUnit <= BigDecimal.ZERO) return null

        val maxRisk = capital.multiply(riskPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
        val rawUnits = maxRisk.divide(riskPerUnit, 0, RoundingMode.DOWN).toInt()
        val lots = (rawUnits / lotSize).coerceAtLeast(0)
        val qty = lots * lotSize
        val actualRisk = riskPerUnit.multiply(BigDecimal.valueOf(qty.toLong())).setScale(2, RoundingMode.HALF_UP)

        return RiskQuantityFormula(
            maxRiskRupees = maxRisk,
            riskPerUnit = riskPerUnit.setScale(2, RoundingMode.HALF_UP),
            calculatedLots = lots,
            calculatedQty = qty,
            estimatedRiskRupees = actualRisk
        )
    }
}
