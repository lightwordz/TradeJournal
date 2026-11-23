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

class AnalyticsEngineTest {

    private fun sampleTrade(
        id: String,
        date: String,
        entry: String,
        exit: String,
        emotion: String,
        rating: Int,
        mistakes: List<String> = emptyList(),
        setup: String = "Breakout",
        side: TradeSide = TradeSide.BUY
    ): Trade = Trade(
        id = id,
        segment = Segment.OPT,
        exchange = Exchange.NSE,
        symbol = "NIFTY 24600 CE",
        side = side,
        orders = listOf(
            OrderFill(datetime = "${date}T09:30:00", qty = 165, price = BigDecimal(entry)),
            OrderFill(datetime = "${date}T10:30:00", qty = 165, price = BigDecimal(exit))
        ),
        lots = 1,
        lotSize = 165,
        plannedRisk = BigDecimal("2000"),
        setup = setup,
        tags = listOf("Options"),
        emotionBefore = emotion,
        emotionDuring = "Calm",
        emotionAfter = "Calm",
        mistakes = mistakes,
        rating = rating,
        notes = "",
        date = date
    )

    @Test
    fun computesEdgeDrawdownAndBehavioralTiltAccurately() {
        val chronological = listOf(
            // Win #1 (Calm, 5*, Monday 2026-03-02)
            sampleTrade("1", "2026-03-02", "100", "140", emotion = "Calm", rating = 5),
            // Loss #1 after win (Calm, 4*, Tuesday 2026-03-03)
            sampleTrade("2", "2026-03-03", "100", "90", emotion = "Calm", rating = 4),
            // Loss #2 after 1 loss (Rushed, 2*, Entered early, Wednesday 2026-03-04)
            sampleTrade("3", "2026-03-04", "100", "85", emotion = "Rushed", rating = 2, mistakes = listOf("Entered early")),
            // Loss #3 after 2 losses (Rushed, 1*, Oversized, Thursday 2026-03-05)
            sampleTrade("4", "2026-03-05", "100", "75", emotion = "Rushed", rating = 1, mistakes = listOf("Oversized"))
        )

        val report = AnalyticsEngine.buildReport(
            chronologicalTrades = chronological,
            profile = BrokerRateProfile.forBroker("Zerodha"),
            matchingMethod = MatchingMethod.FIFO
        )

        assertEquals(4, report.metrics.totalTrades)
        assertEquals(1, report.metrics.winningTrades)
        assertEquals(3, report.metrics.losingTrades)

        // Drawdown series tracks underwater dip from peak after Win #1
        assertEquals(4, report.equityDrawdownSeries.size)
        assertTrue(report.equityDrawdownSeries[0].drawdown.compareTo(BigDecimal.ZERO) == 0)
        assertTrue(report.equityDrawdownSeries[3].drawdown < BigDecimal.ZERO)

        // Sequence tilt detection
        assertEquals(1, report.sequenceBehavior.afterWin.count)
        assertEquals(2, report.sequenceBehavior.afterLoss.count)
        assertEquals(1, report.sequenceBehavior.afterTwoLosses.count)
        assertEquals(0, report.sequenceBehavior.afterTwoLosses.winRatePct)

        // Emotion sorting: Calm (net positive) precedes Rushed (net negative)
        assertEquals("Calm", report.emotionStats.first().label)
        assertEquals("Rushed", report.emotionStats.last().label)
        assertTrue(report.emotionStats.first().netPnl > report.emotionStats.last().netPnl)

        // Process split: High process (4-5*) beats low process (1-3*)
        assertEquals(2, report.processSplit.highProcess.count)
        assertEquals(2, report.processSplit.lowProcess.count)
        assertTrue(report.processSplit.highProcess.netPnl > report.processSplit.lowProcess.netPnl)

        // Weekday checks (2026-03-02 is Monday)
        val mon = report.weekdayStats.first { it.label == "Mon" }
        assertEquals(1, mon.count)
        assertTrue(mon.netPnl > BigDecimal.ZERO)
    }
}
