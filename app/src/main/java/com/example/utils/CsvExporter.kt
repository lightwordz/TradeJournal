package com.example.utils

import com.example.engine.BrokerRateProfile
import com.example.engine.CalculationEngine
import com.example.engine.MatchingMethod
import com.example.model.Trade
import java.math.BigDecimal

object CsvExporter {
    fun toCsv(
        trades: List<Trade>,
        profile: BrokerRateProfile,
        matchingMethod: MatchingMethod = MatchingMethod.FIFO
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Exchange,Market,Symbol,Side,Qty,Entry,Exit,GrossPnL,Fees,NetPnL,Setup,Notes")
        for (trade in trades) {
            val res = CalculationEngine.calculateTradeResult(trade, profile, matchingMethod)
            val entry = trade.orders.firstOrNull()?.price ?: BigDecimal.ZERO
            val exit = trade.orders.lastOrNull()?.price ?: BigDecimal.ZERO
            val qty = trade.lots * trade.lotSize
            val safeNotes = trade.notes.replace("\"", "\"\"")
            val safeSetup = trade.setup.replace("\"", "\"\"")
            sb.appendLine(
                "${trade.date},${trade.exchange.name},${trade.segment.displayName},\"${trade.symbol}\",${trade.side.displayName},$qty,$entry,$exit,${res.grossPnl},${res.charges.totalCharges},${res.netPnl},\"$safeSetup\",\"$safeNotes\""
            )
        }
        return sb.toString()
    }
}
