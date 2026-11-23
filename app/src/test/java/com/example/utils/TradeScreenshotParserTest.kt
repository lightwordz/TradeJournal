package com.example.utils

import com.example.engine.DefaultLotSizes
import com.example.model.Segment
import com.example.model.TradeSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TradeScreenshotParserTest {

    private val lotSizes = DefaultLotSizes.seeded()

    @Test
    fun parsesZerodhaKiteNiftyOptionsPosition() {
        val ocr = """
            Positions
            NIFTY 24600 CE  NFO NRML
            Qty. 330
            Buy avg. 142.50
            Sell avg. 186.25
            P&L +₹14,437.50
        """.trimIndent()

        val draft = TradeScreenshotParser.parse(ocr, lotSizes)

        assertTrue(draft.hasUsefulData)
        assertEquals("NIFTY 24600 CE", draft.symbol)
        assertEquals("NIFTY 50", draft.matchedTickerKey)
        assertEquals(Segment.OPT, draft.segment)
        assertEquals(TradeSide.BUY, draft.side)
        assertEquals(0, BigDecimal("142.50").compareTo(draft.entryPrice))
        assertEquals(0, BigDecimal("186.25").compareTo(draft.exitPrice))
        assertEquals(2, draft.lots)
        assertEquals(165, draft.lotSize)
        assertEquals(330, draft.quantity)
        assertEquals(0, BigDecimal("14437.50").compareTo(draft.detectedPnl))
    }

    @Test
    fun parsesSensexShortTradeWithLots() {
        val ocr = """
            SELL ORDER EXECUTED
            SENSEX 80500 PE  BFO MIS
            2 Lots
            Buy Price: ₹94.00
            Sell Price: ₹128.50
            Net P&L +₹1,380.00
        """.trimIndent()

        val draft = TradeScreenshotParser.parse(ocr, lotSizes)

        assertEquals("SENSEX 80500 PE", draft.symbol)
        assertEquals("SENSEX", draft.matchedTickerKey)
        assertEquals(Segment.OPT, draft.segment)
        assertEquals(TradeSide.SELL, draft.side)
        // Sell first -> entry is sell price (128.50), exit is buy price (94.00)
        assertEquals(0, BigDecimal("128.50").compareTo(draft.entryPrice))
        assertEquals(0, BigDecimal("94.00").compareTo(draft.exitPrice))
        assertEquals(2, draft.lots)
        assertEquals(20, draft.lotSize)
        assertEquals(40, draft.quantity)
    }

    @Test
    fun parsesCommodityAndEquityFormats() {
        val crudeOcr = """
            CRUDEOILM MAY FUT MCX
            Qty: 20
            Entry 5940 -> Exit 5985
            P&L +₹900
        """.trimIndent()
        val crudeDraft = TradeScreenshotParser.parse(crudeOcr, lotSizes)
        assertEquals("CRUDEOILM FUT", crudeDraft.symbol)
        assertEquals("CRUDEOILM", crudeDraft.matchedTickerKey)
        assertEquals(Segment.COMMODITY_FUT, crudeDraft.segment)
        assertEquals(2, crudeDraft.lots)
        assertEquals(10, crudeDraft.lotSize)
        assertEquals(20, crudeDraft.quantity)
        assertEquals(0, BigDecimal("5940").compareTo(crudeDraft.entryPrice))
        assertEquals(0, BigDecimal("5985").compareTo(crudeDraft.exitPrice))

        val stockOcr = """
            RELIANCE NSE CNC
            Shares: 25
            Avg. 2890.50
            LTP 2942.00
            SL 2860 Target 2960
        """.trimIndent()
        val stockDraft = TradeScreenshotParser.parse(stockOcr, lotSizes)
        assertEquals("RELIANCE", stockDraft.symbol)
        assertEquals(null, stockDraft.matchedTickerKey)
        assertEquals(Segment.EQ_DELIVERY, stockDraft.segment)
        assertEquals(25, stockDraft.quantity)
        assertEquals(0, BigDecimal("2890.50").compareTo(stockDraft.entryPrice))
        assertEquals(0, BigDecimal("2942.00").compareTo(stockDraft.exitPrice))
        assertNotNull(stockDraft.stopLoss)
        assertNotNull(stockDraft.target)
    }
}
