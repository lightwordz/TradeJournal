package com.example.screens

import com.example.model.Exchange
import com.example.model.OrderFill
import com.example.model.Segment
import com.example.model.Trade
import com.example.model.TradeSide
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class DashboardPeriodTest {

    private fun tradeOn(date: String) = Trade(
        id = date,
        segment = Segment.OPT,
        exchange = Exchange.NSE,
        symbol = "NIFTY",
        side = TradeSide.BUY,
        orders = listOf(OrderFill(datetime = "${date}T10:00:00", qty = 25, price = BigDecimal("100"))),
        lots = 1,
        lotSize = 25,
        setup = "Breakout",
        tags = emptyList(),
        emotionBefore = "Calm",
        emotionDuring = "Calm",
        emotionAfter = "Calm",
        mistakes = emptyList(),
        rating = 4,
        notes = "",
        date = date
    )

    @Test
    fun testPeriodFilteringIncludes1W1M6M1YAndAll() {
        val trades = listOf(
            tradeOn("2026-03-06"), // latest
            tradeOn("2026-03-02"), // within 1W
            tradeOn("2026-02-15"), // within 1M
            tradeOn("2025-11-01"), // within 6M
            tradeOn("2025-05-01"), // within 1Y
            tradeOn("2024-01-10")  // > 1Y ago
        )

        assertEquals(2, filterTradesByPeriod(trades, "1W").size)
        assertEquals(3, filterTradesByPeriod(trades, "1M").size)
        assertEquals(4, filterTradesByPeriod(trades, "6M").size)
        assertEquals(5, filterTradesByPeriod(trades, "1Y").size)
        assertEquals(6, filterTradesByPeriod(trades, "ALL").size)
    }
}
