package com.example.utils

import com.example.model.Exchange
import com.example.model.OrderFill
import com.example.model.Segment
import com.example.model.Trade
import com.example.model.TradeSide
import java.math.BigDecimal
import java.util.UUID

enum class BrokerPreset(val label: String) {
    AUTO("Auto-Detect"),
    ZERODHA("Zerodha"),
    UPSTOX("Upstox"),
    GROWW("Groww"),
    ANGEL("Angel One")
}

data class CsvParseSummary(
    val importedTrades: List<Trade>,
    val duplicateCount: Int,
    val skippedRows: Int
)

object CsvImporter {

    fun parseCsv(
        rawText: String,
        existingTrades: List<Trade>,
        preset: BrokerPreset = BrokerPreset.AUTO
    ): CsvParseSummary {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return CsvParseSummary(emptyList(), 0, 0)

        val headers = splitCsvLine(lines.first()).map { it.lowercase().trim() }
        val existingSignatures = existingTrades.map { signature(it) }.toMutableSet()

        val symbolIdx = findCol(headers, listOf("symbol", "tradingsymbol", "scrip", "instrument", "stock name"))
        val dateIdx = findCol(headers, listOf("date", "trade_date", "order_execution_time", "execution time", "trade date"))
        val sideIdx = findCol(headers, listOf("side", "trade_type", "type", "buy/sell", "transaction"))
        val qtyIdx = findCol(headers, listOf("qty", "quantity", "filled qty", "shares"))
        val entryIdx = findCol(headers, listOf("entry", "buy price", "price", "trade_price", "avg price"))
        val exitIdx = findCol(headers, listOf("exit", "sell price", "exit price"))
        val marketIdx = findCol(headers, listOf("market", "segment", "product"))
        val exchangeIdx = findCol(headers, listOf("exchange", "exch"))
        val setupIdx = findCol(headers, listOf("setup", "strategy"))
        val notesIdx = findCol(headers, listOf("notes", "remarks"))

        if (symbolIdx < 0 || entryIdx < 0) {
            return CsvParseSummary(emptyList(), 0, lines.size - 1)
        }

        val parsed = mutableListOf<Trade>()
        var duplicates = 0
        var skipped = 0

        for (line in lines.drop(1)) {
            val cols = splitCsvLine(line)
            val symbol = cols.getOrNull(symbolIdx)?.trim()?.uppercase().orEmpty()
            val entry = cols.getOrNull(entryIdx)?.replace(",", "")?.toBigDecimalOrNull()
            if (symbol.isBlank() || entry == null) {
                skipped++
                continue
            }

            val exit = if (exitIdx >= 0) {
                cols.getOrNull(exitIdx)?.replace(",", "")?.toBigDecimalOrNull() ?: entry
            } else {
                entry
            }
            val qty = (if (qtyIdx >= 0) cols.getOrNull(qtyIdx)?.toLongOrNull() else null) ?: 25L
            val rawDate = (if (dateIdx >= 0) cols.getOrNull(dateIdx) else null)?.take(10)
                ?.takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) } ?: "2026-03-06"

            val rawSide = (if (sideIdx >= 0) cols.getOrNull(sideIdx) else null).orEmpty().uppercase()
            val side = if (rawSide.contains("SELL") || rawSide == "S") TradeSide.SELL else TradeSide.BUY

            val rawExchange = (if (exchangeIdx >= 0) cols.getOrNull(exchangeIdx) else null).orEmpty().uppercase()
            val exchange = when {
                rawExchange.contains("MCX") -> Exchange.MCX
                rawExchange.contains("BSE") -> Exchange.BSE
                else -> Exchange.NSE
            }

            val rawMarket = (if (marketIdx >= 0) cols.getOrNull(marketIdx) else null).orEmpty().uppercase()
            val segment = when {
                exchange == Exchange.MCX || rawMarket.contains("COMMOD") -> Segment.COMMODITY_FUT
                rawMarket.contains("OPT") || symbol.endsWith("CE") || symbol.endsWith("PE") -> Segment.OPT
                rawMarket.contains("FUT") || symbol.contains("FUT") -> Segment.FUT
                rawMarket.contains("HOLD") || rawMarket.contains("CNC") || rawMarket.contains("DELIV") -> Segment.EQ_DELIVERY
                else -> Segment.EQ_INTRADAY
            }

            val brokerName = when (preset) {
                BrokerPreset.AUTO -> "IMPORTED"
                else -> preset.label.uppercase()
            }

            val trade = Trade(
                id = UUID.randomUUID().toString(),
                segment = segment,
                exchange = exchange,
                symbol = symbol,
                side = side,
                orders = listOf(
                    OrderFill(datetime = "${rawDate}T09:30:00", qty = qty, price = entry),
                    OrderFill(datetime = "${rawDate}T15:00:00", qty = qty, price = exit)
                ),
                lots = 1,
                lotSize = qty.toInt().coerceAtLeast(1),
                plannedRisk = BigDecimal("1000"),
                setup = (if (setupIdx >= 0) cols.getOrNull(setupIdx) else null).orEmpty().ifBlank { "Imported" },
                tags = listOf(segment.displayName),
                emotionBefore = "Calm",
                emotionDuring = "Focused",
                emotionAfter = "Neutral",
                mistakes = emptyList(),
                rating = 3,
                notes = (if (notesIdx >= 0) cols.getOrNull(notesIdx) else null).orEmpty(),
                brokerId = brokerName,
                date = rawDate
            )

            val sig = signature(trade)
            if (existingSignatures.contains(sig)) {
                duplicates++
            } else {
                existingSignatures.add(sig)
                parsed.add(trade)
            }
        }

        return CsvParseSummary(parsed, duplicates, skipped)
    }

    private fun signature(trade: Trade): String {
        val entry = trade.orders.firstOrNull()?.price?.stripTrailingZeros()?.toPlainString() ?: "0"
        val exit = trade.orders.lastOrNull()?.price?.stripTrailingZeros()?.toPlainString() ?: "0"
        return "${trade.date}|${trade.symbol.uppercase()}|${trade.side.name}|${trade.lots * trade.lotSize}|$entry|$exit"
    }

    private fun findCol(headers: List<String>, candidates: List<String>): Int {
        for (c in candidates) {
            val idx = headers.indexOfFirst { it == c || it.contains(c) }
            if (idx >= 0) return idx
        }
        return -1
    }

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    out.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        out.add(current.toString())
        return out
    }
}
