package com.example.utils

import com.example.model.Exchange
import com.example.model.Segment
import com.example.model.TradeSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CsvImporterTest {

    @Test
    fun testParseBrokerCsvAndSkipDuplicates() {
        val csv = """
            Date,Exchange,Market,Symbol,Side,Qty,Entry,Exit,Setup,Notes
            2026-03-07,NSE,Options,NIFTY 24600 CE,Buy,50,110,145,Breakout,Clean move
            2026-03-07,NSE,Options,NIFTY 24600 CE,Buy,50,110,145,Breakout,Duplicate row
            2026-03-07,MCX,Commodities,GOLDM,Sell,10,85200,84900,Pullback,Evening session
        """.trimIndent()

        val summary = CsvImporter.parseCsv(csv, emptyList(), BrokerPreset.ZERODHA)
        assertEquals(2, summary.importedTrades.size)
        assertEquals(1, summary.duplicateCount)
        assertEquals(0, summary.skippedRows)

        val first = summary.importedTrades[0]
        assertEquals("NIFTY 24600 CE", first.symbol)
        assertEquals(Segment.OPT, first.segment)
        assertEquals(TradeSide.BUY, first.side)
        assertEquals(0, BigDecimal("110").compareTo(first.orders.first().price))

        val second = summary.importedTrades[1]
        assertEquals(Exchange.MCX, second.exchange)
        assertEquals(Segment.COMMODITY_FUT, second.segment)
        assertEquals(TradeSide.SELL, second.side)
    }

    @Test
    fun testIndianMarketCalendarHolidaysAndMonthGrid() {
        assertTrue(IndianMarketCalendar.holidays2026.containsKey("2026-03-03"))
        assertEquals(31, IndianMarketCalendar.daysInMonth(MonthKey(2026, 3)))
        assertEquals(6, IndianMarketCalendar.leadingBlanksMonFirst(MonthKey(2026, 3)))
    }
}
