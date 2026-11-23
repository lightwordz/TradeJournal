package com.example.engine

import com.example.model.Exchange
import com.example.model.OrderFill
import com.example.model.Segment
import com.example.model.Trade
import com.example.model.TradeSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculationEngineTest {

    private fun baseTrade(
        segment: Segment = Segment.OPT,
        exchange: Exchange = Exchange.NSE,
        side: TradeSide = TradeSide.BUY,
        orders: List<OrderFill>,
        lots: Int = 1,
        lotSize: Int = 25,
        multiplier: BigDecimal = BigDecimal.ONE,
        plannedRisk: BigDecimal = BigDecimal("1000.00"),
        stopLoss: BigDecimal = BigDecimal.ZERO,
        target: BigDecimal = BigDecimal.ZERO
    ) = Trade(
        segment = segment,
        exchange = exchange,
        symbol = "TEST",
        side = side,
        orders = orders,
        lots = lots,
        lotSize = lotSize,
        multiplier = multiplier,
        stopLoss = stopLoss,
        target = target,
        plannedRisk = plannedRisk,
        setup = "Breakout",
        tags = emptyList(),
        emotionBefore = "Calm",
        emotionDuring = "Calm",
        emotionAfter = "Calm",
        mistakes = emptyList(),
        rating = 4,
        notes = "",
        date = "2026-03-06"
    )

    @Test
    fun testPaiseConversionRoundHalfUp() {
        assertEquals(12550L, CalculationEngine.toPaise(BigDecimal("125.504")))
        assertEquals(12551L, CalculationEngine.toPaise(BigDecimal("125.505")))
        assertEquals(0, BigDecimal("125.51").compareTo(CalculationEngine.fromPaise(12551L)))
    }

    @Test
    fun testNiftyCallOptionLongProfitAndCharges() {
        val trade = baseTrade(
            segment = Segment.OPT,
            orders = listOf(
                OrderFill(datetime = "2026-03-01T09:30:00", qty = 50, price = BigDecimal("120.00")),
                OrderFill(datetime = "2026-03-01T14:15:00", qty = 50, price = BigDecimal("160.00"))
            ),
            lots = 2,
            lotSize = 25
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal("2000.00").compareTo(res.grossPnl))
        assertEquals(0, BigDecimal("40.00").compareTo(res.charges.brokerage))
        assertTrue(res.netPnl < BigDecimal("2000.00"))
        assertEquals(0, res.grossPnl.subtract(res.charges.totalCharges).compareTo(res.netPnl))
    }

    @Test
    fun testBankNiftyFuturesShortProfit() {
        val trade = baseTrade(
            segment = Segment.FUT,
            side = TradeSide.SELL,
            orders = listOf(
                OrderFill(datetime = "2026-03-02T10:00:00", qty = 15, price = BigDecimal("51200.00")),
                OrderFill(datetime = "2026-03-02T11:30:00", qty = 15, price = BigDecimal("50900.00"))
            ),
            lots = 1,
            lotSize = 15
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal("4500.00").compareTo(res.grossPnl))
        assertTrue(res.charges.stt > BigDecimal.ZERO)
    }

    @Test
    fun testExpiredWorthlessOptionExitZero() {
        val trade = baseTrade(
            segment = Segment.OPT,
            side = TradeSide.BUY,
            orders = listOf(
                OrderFill(datetime = "2026-03-05T10:00:00", qty = 50, price = BigDecimal("45.00")),
                OrderFill(datetime = "2026-03-05T15:30:00", qty = 50, price = BigDecimal.ZERO)
            ),
            lots = 2,
            lotSize = 25
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal("-2250.00").compareTo(res.grossPnl))
        assertEquals(0, BigDecimal.ZERO.compareTo(res.charges.stt)) // 0 sell premium -> 0 STT
        assertTrue(res.netPnl < BigDecimal("-2250.00"))
    }

    @Test
    fun testPartialExitFifoScaleInScaleOut() {
        // Scale in 25 @ 100, 25 @ 120; Partial exit 30 @ 140 (matches 25@100 + 5@120)
        // FIFO gross = 25*(140-100) + 5*(140-120) = 1000 + 100 = 1100
        val trade = baseTrade(
            segment = Segment.OPT,
            orders = listOf(
                OrderFill(datetime = "2026-03-06T09:30:00", qty = 25, price = BigDecimal("100.00")),
                OrderFill(datetime = "2026-03-06T09:45:00", qty = 25, price = BigDecimal("120.00")),
                OrderFill(datetime = "2026-03-06T10:30:00", qty = 30, price = BigDecimal("140.00"))
            ),
            lots = 2,
            lotSize = 25
        )
        val fifo = CalculationEngine.calculateTradeResult(trade, matchingMethod = MatchingMethod.FIFO)
        val wAvg = CalculationEngine.calculateTradeResult(trade, matchingMethod = MatchingMethod.WEIGHTED_AVG)
        assertEquals(0, BigDecimal("1100.00").compareTo(fifo.grossPnl))
        // Weighted avg entry = 110 -> 30*(140-110) = 900
        assertEquals(0, BigDecimal("900.00").compareTo(wAvg.grossPnl))
    }

    @Test
    fun testSingleOpenOrderProducesZeroGrossPnl() {
        val trade = baseTrade(
            segment = Segment.EQ_INTRADAY,
            orders = listOf(OrderFill(datetime = "2026-03-06T09:30:00", qty = 10, price = BigDecimal("1000.00"))),
            lots = 1,
            lotSize = 10
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal.ZERO.compareTo(res.grossPnl))
    }

    @Test
    fun testEquityDeliveryDpChargesAndZeroZerodhaBrokerage() {
        val trade = baseTrade(
            segment = Segment.EQ_DELIVERY,
            orders = listOf(
                OrderFill(datetime = "2026-03-01T10:00:00", qty = 10, price = BigDecimal("1000.00")),
                OrderFill(datetime = "2026-03-04T14:00:00", qty = 10, price = BigDecimal("1200.00"))
            ),
            lots = 1,
            lotSize = 10
        )
        val zerodha = CalculationEngine.calculateTradeResult(trade, BrokerRateProfile.forBroker("Zerodha"))
        val upstox = CalculationEngine.calculateTradeResult(trade, BrokerRateProfile.forBroker("Upstox"))
        assertEquals(0, BigDecimal.ZERO.compareTo(zerodha.charges.brokerage))
        assertEquals(0, BigDecimal("15.93").compareTo(zerodha.charges.dpCharges))
        assertTrue(upstox.charges.brokerage > BigDecimal.ZERO)
    }

    @Test
    fun testEquityIntradayPctBrokerageBelowCap() {
        // Turnover = 10 * 100 + 10 * 105 = 2050; 0.03% = 0.62 (< 40 cap)
        val trade = baseTrade(
            segment = Segment.EQ_INTRADAY,
            orders = listOf(
                OrderFill(datetime = "2026-03-06T09:30:00", qty = 10, price = BigDecimal("100.00")),
                OrderFill(datetime = "2026-03-06T11:30:00", qty = 10, price = BigDecimal("105.00"))
            ),
            lots = 1,
            lotSize = 10
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal("0.62").compareTo(res.charges.brokerage))
    }

    @Test
    fun testMcxCommodityWithMultiplier() {
        val trade = baseTrade(
            segment = Segment.COMMODITY_FUT,
            exchange = Exchange.MCX,
            orders = listOf(
                OrderFill(datetime = "2026-03-06T19:00:00", qty = 1, price = BigDecimal("5900.00")),
                OrderFill(datetime = "2026-03-06T23:30:00", qty = 1, price = BigDecimal("5950.00"))
            ),
            lots = 1,
            lotSize = 1,
            multiplier = BigDecimal("10")
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal("500.00").compareTo(res.grossPnl))
    }

    @Test
    fun testRMultipleAndRiskRewardRatio() {
        val trade = baseTrade(
            segment = Segment.OPT,
            orders = listOf(
                OrderFill(datetime = "2026-03-06T09:30:00", qty = 25, price = BigDecimal("100.00")),
                OrderFill(datetime = "2026-03-06T11:00:00", qty = 25, price = BigDecimal("140.00"))
            ),
            lots = 1,
            lotSize = 25,
            plannedRisk = BigDecimal("500.00"),
            stopLoss = BigDecimal("80.00"),
            target = BigDecimal("160.00")
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal("3.00").compareTo(res.riskReward)) // (160-100)/(100-80) = 3.00
        assertTrue(res.rMultiple > BigDecimal("1.50"))
    }

    @Test
    fun testMissingStopLossAndZeroPlannedRiskNoCrash() {
        val trade = baseTrade(
            orders = listOf(
                OrderFill(datetime = "2026-03-06T09:30:00", qty = 25, price = BigDecimal("100.00")),
                OrderFill(datetime = "2026-03-06T11:00:00", qty = 25, price = BigDecimal("120.00"))
            ),
            plannedRisk = BigDecimal.ZERO,
            stopLoss = BigDecimal.ZERO,
            target = BigDecimal.ZERO
        )
        val res = CalculationEngine.calculateTradeResult(trade)
        assertEquals(0, BigDecimal.ZERO.compareTo(res.rMultiple))
        assertEquals(0, BigDecimal.ZERO.compareTo(res.riskReward))
    }

    @Test
    fun testEmptyDatasetMetricsZeroSafe() {
        val metrics = CalculationEngine.computeMetrics(emptyList())
        assertEquals(0, metrics.totalTrades)
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.totalNetPnl))
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.winRate))
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.profitFactor))
    }

    @Test
    fun testZeroLossDatasetMetricsNoDivideByZero() {
        val dummy = baseTrade(orders = listOf(OrderFill(datetime = "2026-03-01", qty = 1, price = BigDecimal.ONE)))
        val base = CalculationEngine.calculateTradeResult(dummy)
        val metrics = CalculationEngine.computeMetrics(
            listOf(base.copy(netPnl = BigDecimal("1000.00")), base.copy(netPnl = BigDecimal("500.00")))
        )
        assertEquals(2, metrics.winningTrades)
        assertEquals(0, metrics.losingTrades)
        assertEquals(0, BigDecimal("100.0").compareTo(metrics.winRate))
        assertEquals(0, BigDecimal("99.99").compareTo(metrics.profitFactor))
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.maxDrawdown))
    }

    @Test
    fun testZeroWinDatasetMetrics() {
        val dummy = baseTrade(orders = listOf(OrderFill(datetime = "2026-03-01", qty = 1, price = BigDecimal.ONE)))
        val base = CalculationEngine.calculateTradeResult(dummy)
        val metrics = CalculationEngine.computeMetrics(
            listOf(base.copy(netPnl = BigDecimal("-400.00")), base.copy(netPnl = BigDecimal("-600.00")))
        )
        assertEquals(0, metrics.winningTrades)
        assertEquals(2, metrics.losingTrades)
        assertEquals(0, BigDecimal("0.0").compareTo(metrics.winRate))
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.profitFactor))
        assertEquals(-2, metrics.currentStreak)
        assertEquals(0, BigDecimal("1000.00").compareTo(metrics.maxDrawdown))
    }

    @Test
    fun testMaxDrawdownAndExpectancyHandVerified() {
        val dummy = baseTrade(orders = listOf(OrderFill(datetime = "2026-03-01", qty = 1, price = BigDecimal.ONE)))
        val base = CalculationEngine.calculateTradeResult(dummy)
        // Sequence: +1000 (peak 1000), -300 (700, dd 300), -400 (300, dd 700), +1200 (1500)
        val metrics = CalculationEngine.computeMetrics(
            listOf(
                base.copy(netPnl = BigDecimal("1000.00")),
                base.copy(netPnl = BigDecimal("-300.00")),
                base.copy(netPnl = BigDecimal("-400.00")),
                base.copy(netPnl = BigDecimal("1200.00"))
            )
        )
        assertEquals(0, BigDecimal("1500.00").compareTo(metrics.totalNetPnl))
        assertEquals(0, BigDecimal("700.00").compareTo(metrics.maxDrawdown))
        // Gross wins 2200, gross losses 700 -> PF = 3.14
        assertEquals(0, BigDecimal("3.14").compareTo(metrics.profitFactor))
        assertEquals(1, metrics.currentStreak)
    }

    @Test
    fun testDefaultLotSizesAndNifty50Real165QtyCalculation() {
        val seeded = DefaultLotSizes.seeded()
        assertEquals(165, seeded["NIFTY 50"])
        assertEquals(20, seeded["SENSEX"])
        assertEquals(30, seeded["BANKNIFTY"])
        assertEquals(330, DefaultLotSizes.computeQuantity(lots = 2, lotSize = seeded["NIFTY 50"]!!))

        val niftyTrade = baseTrade(
            segment = Segment.OPT,
            orders = listOf(
                OrderFill(datetime = "2026-03-02T09:30:00", qty = 165, price = BigDecimal("100.00")),
                OrderFill(datetime = "2026-03-02T11:30:00", qty = 165, price = BigDecimal("120.00"))
            ),
            lots = 1,
            lotSize = 165
        )
        val res = CalculationEngine.calculateTradeResult(niftyTrade)
        assertEquals(0, BigDecimal("3300.00").compareTo(res.grossPnl))
        assertTrue(res.netPnl > BigDecimal("3200.00"))
    }

    @Test
    fun testDailyRiskGuardrailAndRiskFormula() {
        val dummy = baseTrade(orders = listOf(OrderFill(datetime = "2026-03-01", qty = 1, price = BigDecimal.ONE)))
        val base = CalculationEngine.calculateTradeResult(dummy)

        val calm = CalculationEngine.evaluateDailyRiskGuardrail(
            listOf(base.copy(netPnl = BigDecimal("2500.00"))),
            BigDecimal("10000")
        )
        assertEquals(CalculationEngine.GuardrailLevel.CALM, calm.level)

        val cautionStreak = CalculationEngine.evaluateDailyRiskGuardrail(
            listOf(
                base.copy(netPnl = BigDecimal("-1200.00")),
                base.copy(netPnl = BigDecimal("-1500.00"))
            ),
            BigDecimal("10000")
        )
        assertEquals(CalculationEngine.GuardrailLevel.CAUTION, cautionStreak.level)
        assertEquals(2, cautionStreak.trailingLosses)

        val breached = CalculationEngine.evaluateDailyRiskGuardrail(
            listOf(base.copy(netPnl = BigDecimal("-10500.00"))),
            BigDecimal("10000")
        )
        assertEquals(CalculationEngine.GuardrailLevel.BREACHED, breached.level)

        // Capital 5,00,000, Risk 1% = 5,000; Entry 120, SL 105 (risk/unit = 15); NIFTY 50 lotSize 165 -> 5000 / 15 = 333 units -> 2 lots = 330 qty
        val formula = CalculationEngine.calculateRiskQuantityFormula(
            capital = BigDecimal("500000"),
            riskPercent = BigDecimal("1.0"),
            entryPrice = BigDecimal("120"),
            stopLossPrice = BigDecimal("105"),
            lotSize = 165
        )!!
        assertEquals(2, formula.calculatedLots)
        assertEquals(330, formula.calculatedQty)
        assertEquals(0, BigDecimal("4950.00").compareTo(formula.estimatedRiskRupees))
    }
}
