package com.example.data

object FeatureFlags {
    const val CLOUD_SYNC = false
    const val BROKER_LIVE_IMPORT = false
    const val AI_COACH = false
    const val TAX_REPORT = false
    const val PRO_SUBSCRIPTION = false
}

interface MarketDataProvider {
    suspend fun getLastTradedPrice(exchange: String, symbol: String): Double?
}

class NoOpMarketDataProvider : MarketDataProvider {
    override suspend fun getLastTradedPrice(exchange: String, symbol: String): Double? = null
}
