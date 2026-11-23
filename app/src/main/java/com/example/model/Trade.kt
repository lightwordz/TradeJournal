package com.example.model

import java.math.BigDecimal

enum class Segment(val displayName: String) {
    EQ_INTRADAY("Stocks · Day"),
    EQ_DELIVERY("Stocks · Hold"),
    FUT("Futures"),
    OPT("Options"),
    COMMODITY_FUT("Commodities"),
    COMMODITY_OPT("Commodity Opt")
}

enum class Exchange(val displayName: String) {
    NSE("NSE"),
    BSE("BSE"),
    MCX("MCX")
}

enum class OptionType(val displayName: String) {
    CE("Call"),
    PE("Put")
}

enum class TradeSide(val displayName: String) {
    BUY("Buy"),
    SELL("Sell")
}

data class OrderFill(
    val id: String = java.util.UUID.randomUUID().toString(),
    val datetime: String, // ISO format or IST formatted string
    val qty: Long,
    val price: BigDecimal // in rupees or paise as BigDecimal
)

data class Trade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val segment: Segment,
    val exchange: Exchange,
    val symbol: String,
    val expiry: String = "",
    val strike: BigDecimal = BigDecimal.ZERO,
    val optionType: OptionType? = null,
    val side: TradeSide,
    val orders: List<OrderFill>,
    val lots: Int,
    val lotSize: Int,
    val multiplier: BigDecimal = BigDecimal.ONE,
    val stopLoss: BigDecimal = BigDecimal.ZERO,
    val target: BigDecimal = BigDecimal.ZERO,
    val plannedRisk: BigDecimal = BigDecimal.ZERO, // in rupees
    val setup: String,
    val tags: List<String>,
    val emotionBefore: String,
    val emotionDuring: String,
    val emotionAfter: String,
    val mistakes: List<String>,
    val rating: Int, // 1 to 5
    val notes: String,
    val screenshots: List<String> = emptyList(),
    val brokerId: String = "ZERODHA",
    val date: String // YYYY-MM-DD for grouping
)

data class ChargeBreakdown(
    val brokerage: BigDecimal,
    val stt: BigDecimal,
    val exchangeCharges: BigDecimal,
    val sebiFee: BigDecimal,
    val stampDuty: BigDecimal,
    val gst: BigDecimal,
    val dpCharges: BigDecimal,
    val totalCharges: BigDecimal
)

data class TradeResult(
    val tradeId: String,
    val grossPnl: BigDecimal,
    val charges: ChargeBreakdown,
    val netPnl: BigDecimal,
    val rMultiple: BigDecimal,
    val riskReward: BigDecimal
)
