package com.example.utils

import com.example.engine.DefaultLotSizes
import com.example.model.Segment
import com.example.model.TradeSide
import java.math.BigDecimal

data class ScreenshotTradeDraft(
    val symbol: String = "",
    val matchedTickerKey: String? = null,
    val segment: Segment = Segment.OPT,
    val side: TradeSide = TradeSide.BUY,
    val entryPrice: BigDecimal? = null,
    val exitPrice: BigDecimal? = null,
    val lots: Int? = null,
    val lotSize: Int? = null,
    val quantity: Int? = null,
    val stopLoss: BigDecimal? = null,
    val target: BigDecimal? = null,
    val detectedPnl: BigDecimal? = null,
    val rawText: String = "",
    val detectedFields: List<String> = emptyList(),
    val screenshotPath: String? = null
) {
    val hasUsefulData: Boolean
        get() = symbol.isNotBlank() || entryPrice != null || exitPrice != null || quantity != null
}

object TradeScreenshotParser {

    private val INDEX_OR_COMMODITY_LINE = Regex(
        """(?i)\b(NIFTY\s*50|NIFTY|BANKNIFTY|FINNIFTY|MIDCPNIFTY|SENSEX|BANKEX|GOLDM|GOLD|SILVERM|SILVER|CRUDEOILM|CRUDEOIL|NATURALGAS)\b([^\n]{0,36})"""
    )

    private val OPTION_STRIKE_SUFFIX = Regex(
        """(?i)\b(\d{4,6})\s*(CE|PE|CALL|PUT)\b"""
    )

    private val BUY_OR_ENTRY_PRICE = Regex(
        """(?i)\b(?:buy\s*(?:avg\.?|average|price)|entry\s*(?:price)?)\s*[:\-]?\s*(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val SELL_OR_EXIT_PRICE = Regex(
        """(?i)\b(?:sell\s*(?:avg\.?|average|price)|exit\s*(?:price)?|close\s*(?:price)?)\s*[:\-]?\s*(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val AVG_PRICE = Regex(
        """(?i)\b(?:avg\.?\s*(?:price)?|traded\s*price)\s*[:\-]?\s*(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val LTP_PRICE = Regex(
        """(?i)\b(?:ltp|cmp|last\s*price)\s*[:\-]?\s*(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val ARROW_PRICE_PAIR = Regex(
        """(?:₹|rs\.?\s*)?([0-9][0-9,]*(?:\.\d{1,2})?)\s*(?:->|→)\s*(?:₹|rs\.?\s*)?([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val QTY_REGEX = Regex(
        """(?i)\b(?:qty\.?|quantity|filled\s*qty|shares)\s*[:\-]?\s*(?:\d+\s*/\s*)?([0-9][0-9,]*)\b"""
    )

    private val SLASH_QTY_REGEX = Regex(
        """(?i)\b\d+\s*/\s*([0-9][0-9,]*)\s*(?:qty)?\b"""
    )

    private val LOTS_REGEX = Regex(
        """(?i)(?:\b(\d+)\s*lots?\b|\blots?\s*[:\-]?\s*(\d+)\b)"""
    )

    private val SL_REGEX = Regex(
        """(?i)\b(?:stop\s*loss|sl|stoploss)\s*[:\-]?\s*(?:₹|rs\.?)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val TARGET_REGEX = Regex(
        """(?i)\b(?:target|tgt)\s*[:\-]?\s*(?:₹|rs\.?)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val PNL_REGEX = Regex(
        """(?i)(?:realised\s*p\s*&\s*l|net\s*p\s*&\s*l|day'?s?\s*p\s*&\s*l|p\s*&\s*l|pnl)\s*[:\-]?\s*([+\-]?)\s*(?:₹|rs\.?|inr)?\s*([+\-]?[0-9][0-9,]*(?:\.\d{1,2})?)"""
    )

    private val NOISE_WORDS = setOf(
        "ZERODHA", "KITE", "GROWW", "UPSTOX", "ANGEL", "ONE", "DHAN", "FYERS",
        "POSITIONS", "POSITION", "ORDERS", "ORDER", "HOLDINGS", "PORTFOLIO",
        "COMPLETED", "EXECUTED", "FILLED", "CLOSED", "OPEN", "REJECTED",
        "BUY", "SELL", "LONG", "SHORT", "NRML", "MIS", "CNC", "INTRADAY", "DELIVERY",
        "NSE", "BSE", "MCX", "NFO", "BFO", "CDS", "QTY", "AVG", "LTP", "CMP",
        "PNL", "PROFIT", "LOSS", "TOTAL", "NET", "ENTRY", "EXIT", "PRICE", "LOTS", "LOT",
        "TODAY", "SUMMARY", "CHARGES", "MARGIN", "AVAILABLE"
    )

    fun parse(
        rawText: String,
        lotSizes: Map<String, Int> = DefaultLotSizes.seeded(),
        screenshotPath: String? = null
    ): ScreenshotTradeDraft {
        if (rawText.isBlank()) {
            return ScreenshotTradeDraft(rawText = rawText, screenshotPath = screenshotPath)
        }

        val normalizedText = rawText
            .replace("\r\n", "\n")
            .replace("\u20B9", "₹")
        val upper = normalizedText.uppercase()

        val symbol = extractSymbol(normalizedText)
        val matchedTickerKey = matchTickerKey(symbol, lotSizes)
        val segment = detectSegment(symbol, upper)
        val side = detectSide(upper)

        val explicitBuy = BUY_OR_ENTRY_PRICE.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val explicitSell = SELL_OR_EXIT_PRICE.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val avgPrice = AVG_PRICE.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val ltpPrice = LTP_PRICE.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val arrowMatch = ARROW_PRICE_PAIR.find(normalizedText)
        val arrowEntry = arrowMatch?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val arrowExit = arrowMatch?.groupValues?.getOrNull(2)?.let(::parseAmount)

        val (entryPrice, exitPrice) = when {
            explicitBuy != null && explicitSell != null -> {
                if (side == TradeSide.BUY) explicitBuy to explicitSell else explicitSell to explicitBuy
            }
            arrowEntry != null && arrowExit != null -> arrowEntry to arrowExit
            explicitBuy != null && ltpPrice != null -> explicitBuy to ltpPrice
            avgPrice != null && explicitSell != null -> avgPrice to explicitSell
            avgPrice != null && ltpPrice != null -> avgPrice to ltpPrice
            explicitBuy != null -> explicitBuy to null
            avgPrice != null -> avgPrice to null
            explicitSell != null -> null to explicitSell
            else -> null to null
        }

        val rawQty = QTY_REGEX.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parsePositiveInt)
            ?: SLASH_QTY_REGEX.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parsePositiveInt)

        val lotsMatch = LOTS_REGEX.find(normalizedText)
        val rawLots = lotsMatch?.let { m ->
            (m.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() })?.toIntOrNull()
        }?.takeIf { it > 0 }

        val mappedLotSize = matchedTickerKey?.let { lotSizes[it] }
        val isEquity = segment == Segment.EQ_INTRADAY || segment == Segment.EQ_DELIVERY

        val (finalLots, finalLotSize, finalQty) = when {
            isEquity -> {
                val q = rawQty ?: rawLots
                Triple(1, (q ?: 1), q)
            }
            rawQty != null && mappedLotSize != null && mappedLotSize > 1 && rawQty % mappedLotSize == 0 -> {
                Triple((rawQty / mappedLotSize).coerceAtLeast(1), mappedLotSize, rawQty)
            }
            rawQty != null && rawLots != null && rawLots > 0 && rawQty % rawLots == 0 -> {
                Triple(rawLots, rawQty / rawLots, rawQty)
            }
            rawQty != null -> {
                val ls = mappedLotSize ?: rawQty
                if (ls > 0 && rawQty % ls == 0) {
                    Triple(rawQty / ls, ls, rawQty)
                } else {
                    Triple(1, rawQty, rawQty)
                }
            }
            rawLots != null && mappedLotSize != null -> {
                Triple(rawLots, mappedLotSize, rawLots * mappedLotSize)
            }
            mappedLotSize != null -> {
                Triple(1, mappedLotSize, mappedLotSize)
            }
            rawLots != null -> {
                Triple(rawLots, 1, rawLots)
            }
            else -> Triple(null, null, null)
        }

        val stopLoss = SL_REGEX.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val target = TARGET_REGEX.find(normalizedText)?.groupValues?.getOrNull(1)?.let(::parseAmount)
        val detectedPnl = extractPnl(normalizedText)

        val detected = buildList {
            if (symbol.isNotBlank()) add("Ticker")
            if (entryPrice != null) add("Entry")
            if (exitPrice != null) add("Exit")
            if (finalQty != null) add("Qty")
            if (detectedPnl != null) add("P&L")
        }

        return ScreenshotTradeDraft(
            symbol = symbol,
            matchedTickerKey = matchedTickerKey,
            segment = segment,
            side = side,
            entryPrice = entryPrice,
            exitPrice = exitPrice,
            lots = finalLots,
            lotSize = finalLotSize,
            quantity = finalQty,
            stopLoss = stopLoss,
            target = target,
            detectedPnl = detectedPnl,
            rawText = normalizedText,
            detectedFields = detected,
            screenshotPath = screenshotPath
        )
    }

    private fun extractSymbol(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Prefer well-known F&O / Commodity contract lines
        for (line in lines) {
            val match = INDEX_OR_COMMODITY_LINE.find(line) ?: continue
            val rootRaw = match.groupValues[1].uppercase().replace(Regex("""\s+"""), " ").trim()
            val tail = match.groupValues[2].uppercase()
            val normalizedRoot = if (rootRaw == "NIFTY 50") "NIFTY" else rootRaw

            val strikeMatch = OPTION_STRIKE_SUFFIX.find(line)
            if (strikeMatch != null) {
                val strike = strikeMatch.groupValues[1]
                val rawType = strikeMatch.groupValues[2].uppercase()
                val optType = when (rawType) {
                    "CALL" -> "CE"
                    "PUT" -> "PE"
                    else -> rawType
                }
                return "$normalizedRoot $strike $optType"
            }

            if (Regex("""\bFUT\b""").containsMatchIn(tail) || Regex("""\bFUT\b""").containsMatchIn(line)) {
                return "$normalizedRoot FUT"
            }

            return if (rootRaw == "NIFTY") "NIFTY 50" else rootRaw
        }

        // 2. Fallback: look for stock equity symbols near NSE / BSE / MIS / CNC / EQ
        val equityLineRegex = Regex("""(?i)\b([A-Z]{2,15})(?:\s*[-·]?\s*(?:EQ|NSE|BSE|CNC|MIS))\b""")
        for (line in lines) {
            val m = equityLineRegex.find(line)
            if (m != null) {
                val candidate = m.groupValues[1].uppercase()
                if (candidate !in NOISE_WORDS) return candidate
            }
        }

        // 3. Last resort: clean standalone uppercase ticker token
        val standaloneRegex = Regex("""^[A-Z][A-Z0-9&-]{2,14}$""")
        for (line in lines) {
            val tokens = line.split(Regex("""\s+"""))
            for (tok in tokens) {
                val clean = tok.trim(',', '.', ':', '|').uppercase()
                if (standaloneRegex.matches(clean) && clean !in NOISE_WORDS) {
                    return clean
                }
            }
        }

        return ""
    }

    private fun matchTickerKey(symbol: String, lotSizes: Map<String, Int>): String? {
        if (symbol.isBlank()) return null
        val upper = symbol.uppercase()
        lotSizes.keys
            .sortedByDescending { it.length }
            .firstOrNull { upper.startsWith(it) }
            ?.let { return it }

        if (upper.startsWith("NIFTY") && lotSizes.containsKey("NIFTY 50")) return "NIFTY 50"
        if (upper.startsWith("CRUDE") && lotSizes.containsKey("CRUDEOILM")) return "CRUDEOILM"
        if (upper.startsWith("GOLD") && lotSizes.containsKey("GOLDM")) return "GOLDM"
        return null
    }

    private fun detectSegment(symbol: String, upperText: String): Segment {
        val upperSym = symbol.uppercase()
        return when {
            DefaultLotSizes.isCommodity(upperSym) || upperText.contains("MCX") -> Segment.COMMODITY_FUT
            upperSym.endsWith(" CE") || upperSym.endsWith(" PE") ||
                Regex("""\b\d{4,6}\s*(?:CE|PE)\b""").containsMatchIn(upperText) -> Segment.OPT
            upperSym.endsWith(" FUT") || Regex("""\bFUT\b""").containsMatchIn(upperText) -> Segment.FUT
            Regex("""\b(?:CNC|DELIVERY|HOLDING)\b""").containsMatchIn(upperText) -> Segment.EQ_DELIVERY
            Regex("""\b(?:NFO|BFO|OPTIONS?)\b""").containsMatchIn(upperText) -> Segment.OPT
            Regex("""\b(?:MIS|INTRADAY|EQ)\b""").containsMatchIn(upperText) -> Segment.EQ_INTRADAY
            upperSym.isNotBlank() && !DefaultLotSizes.seeded().keys.any { upperSym.startsWith(it) } && !upperSym.startsWith("NIFTY") -> Segment.EQ_INTRADAY
            else -> Segment.OPT
        }
    }

    private fun detectSide(upperText: String): TradeSide {
        val sellHeader = Regex("""\bSELL\s+(?:ORDER|FILLED|EXECUTED|FIRST)\b|\bSHORT\b""")
        val buyHeader = Regex("""\bBUY\s+(?:ORDER|FILLED|EXECUTED|FIRST)\b|\bLONG\b""")
        if (sellHeader.containsMatchIn(upperText) && !buyHeader.containsMatchIn(upperText)) {
            return TradeSide.SELL
        }
        val firstSellIdx = Regex("""\bSELL\b""").find(upperText)?.range?.first
        val firstBuyIdx = Regex("""\bBUY\b""").find(upperText)?.range?.first
        return when {
            firstSellIdx != null && firstBuyIdx == null -> TradeSide.SELL
            firstSellIdx != null && firstBuyIdx != null &&
                !upperText.contains("BUY AVG") && !upperText.contains("SELL AVG") &&
                firstSellIdx < firstBuyIdx -> TradeSide.SELL
            else -> TradeSide.BUY
        }
    }

    private fun extractPnl(text: String): BigDecimal? {
        val m = PNL_REGEX.find(text) ?: return null
        val signPrefix = m.groupValues.getOrNull(1)?.trim().orEmpty()
        val numPart = m.groupValues.getOrNull(2)?.trim().orEmpty()
        val combined = if (signPrefix == "-" && !numPart.startsWith("-")) "-$numPart" else numPart
        return combined.replace(",", "").replace("+", "").toBigDecimalOrNull()
    }

    private fun parseAmount(raw: String): BigDecimal? =
        raw.replace(",", "").toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }

    private fun parsePositiveInt(raw: String): Int? =
        raw.replace(",", "").toIntOrNull()?.takeIf { it > 0 }
}
