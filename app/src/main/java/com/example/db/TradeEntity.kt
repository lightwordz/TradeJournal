package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.TraderPreferences
import com.example.model.CapitalEntry
import com.example.model.CapitalEntryType
import com.example.model.Exchange
import com.example.model.OptionType
import com.example.model.OrderFill
import com.example.model.Segment
import com.example.model.Trade
import com.example.model.TradeSide
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey val id: String,
    val segment: String,
    val exchange: String,
    val symbol: String,
    val expiry: String,
    val strike: String,
    val optionType: String?,
    val side: String,
    val ordersJson: String,
    val lots: Int,
    val lotSize: Int,
    val multiplier: String,
    val stopLoss: String,
    val target: String,
    val plannedRisk: String,
    val setup: String,
    val tagsJson: String,
    val emotionBefore: String,
    val emotionDuring: String,
    val emotionAfter: String,
    val mistakesJson: String,
    val rating: Int,
    val notes: String,
    val screenshotsJson: String,
    val brokerId: String,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Trade {
        val orders = runCatching {
            val ordersArr = JSONArray(ordersJson)
            buildList {
                for (i in 0 until ordersArr.length()) {
                    val obj = ordersArr.getJSONObject(i)
                    add(
                        OrderFill(
                            id = obj.optString("id"),
                            datetime = obj.optString("datetime"),
                            qty = obj.optLong("qty"),
                            price = obj.optString("price", "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        return Trade(
            id = id,
            segment = runCatching { Segment.valueOf(segment) }.getOrDefault(Segment.OPT),
            exchange = runCatching { Exchange.valueOf(exchange) }.getOrDefault(Exchange.NSE),
            symbol = symbol,
            expiry = expiry,
            strike = strike.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            optionType = optionType?.let { runCatching { OptionType.valueOf(it) }.getOrNull() },
            side = runCatching { TradeSide.valueOf(side) }.getOrDefault(TradeSide.BUY),
            orders = orders,
            lots = lots,
            lotSize = lotSize,
            multiplier = multiplier.toBigDecimalOrNull() ?: BigDecimal.ONE,
            stopLoss = stopLoss.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            target = target.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            plannedRisk = plannedRisk.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            setup = setup,
            tags = decodeStringList(tagsJson),
            emotionBefore = emotionBefore,
            emotionDuring = emotionDuring,
            emotionAfter = emotionAfter,
            mistakes = decodeStringList(mistakesJson),
            rating = rating,
            notes = notes,
            screenshots = decodeStringList(screenshotsJson),
            brokerId = brokerId,
            date = date
        )
    }

    companion object {
        fun fromDomain(trade: Trade, createdAt: Long = System.currentTimeMillis()): TradeEntity {
            val ordersArr = JSONArray().apply {
                trade.orders.forEach { fill ->
                    put(
                        JSONObject().apply {
                            put("id", fill.id)
                            put("datetime", fill.datetime)
                            put("qty", fill.qty)
                            put("price", fill.price.toPlainString())
                        }
                    )
                }
            }
            return TradeEntity(
                id = trade.id,
                segment = trade.segment.name,
                exchange = trade.exchange.name,
                symbol = trade.symbol,
                expiry = trade.expiry,
                strike = trade.strike.toPlainString(),
                optionType = trade.optionType?.name,
                side = trade.side.name,
                ordersJson = ordersArr.toString(),
                lots = trade.lots,
                lotSize = trade.lotSize,
                multiplier = trade.multiplier.toPlainString(),
                stopLoss = trade.stopLoss.toPlainString(),
                target = trade.target.toPlainString(),
                plannedRisk = trade.plannedRisk.toPlainString(),
                setup = trade.setup,
                tagsJson = encodeStringList(trade.tags),
                emotionBefore = trade.emotionBefore,
                emotionDuring = trade.emotionDuring,
                emotionAfter = trade.emotionAfter,
                mistakesJson = encodeStringList(trade.mistakes),
                rating = trade.rating,
                notes = trade.notes,
                screenshotsJson = encodeStringList(trade.screenshots),
                brokerId = trade.brokerId,
                date = trade.date,
                createdAt = createdAt
            )
        }

        private fun encodeStringList(list: List<String>): String =
            JSONArray().apply { list.forEach { put(it) } }.toString()

        private fun decodeStringList(json: String): List<String> = runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }
}

@Entity(tableName = "preferences")
data class PreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val traderName: String,
    val isDarkTheme: Boolean,
    val startingCapital: String,
    val dailyLossLimit: String,
    val defaultBroker: String,
    val hasSeededDemo: Boolean,
    val hasCompletedOnboarding: Boolean = false,
    val matchingMethod: String = "FIFO",
    val customRatesJson: String = "",
    val lotSizesJson: String = ""
) {
    fun toDomain(): TraderPreferences = TraderPreferences(
        traderName = traderName,
        isDarkTheme = isDarkTheme,
        startingCapital = startingCapital.toBigDecimalOrNull() ?: BigDecimal("500000"),
        dailyLossLimit = dailyLossLimit.toBigDecimalOrNull() ?: BigDecimal("10000"),
        defaultBroker = defaultBroker,
        hasCompletedOnboarding = hasCompletedOnboarding,
        matchingMethod = runCatching {
            com.example.engine.MatchingMethod.valueOf(matchingMethod)
        }.getOrDefault(com.example.engine.MatchingMethod.FIFO),
        customRateProfile = decodeRateProfile(customRatesJson),
        lotSizes = decodeLotSizes(lotSizesJson)
    )

    companion object {
        fun fromDomain(prefs: TraderPreferences, hasSeededDemo: Boolean = true): PreferencesEntity =
            PreferencesEntity(
                id = 1,
                traderName = prefs.traderName,
                isDarkTheme = prefs.isDarkTheme,
                startingCapital = prefs.startingCapital.toPlainString(),
                dailyLossLimit = prefs.dailyLossLimit.toPlainString(),
                defaultBroker = prefs.defaultBroker,
                hasSeededDemo = hasSeededDemo,
                hasCompletedOnboarding = prefs.hasCompletedOnboarding,
                matchingMethod = prefs.matchingMethod.name,
                customRatesJson = encodeRateProfile(prefs.customRateProfile),
                lotSizesJson = if (prefs.lotSizes == com.example.engine.DefaultLotSizes.seeded()) "" else encodeLotSizes(prefs.lotSizes)
            )

        fun encodeRateProfile(profile: com.example.engine.BrokerRateProfile?): String {
            if (profile == null) return ""
            return JSONObject().apply {
                put("brokerName", profile.brokerName)
                put("eqDeliveryBrokeragePct", profile.eqDeliveryBrokeragePct.toPlainString())
                put("eqDeliveryFlatMax", profile.eqDeliveryFlatMax.toPlainString())
                put("eqIntradayBrokeragePct", profile.eqIntradayBrokeragePct.toPlainString())
                put("eqIntradayFlatPerOrder", profile.eqIntradayFlatPerOrder.toPlainString())
                put("fnoFuturesFlatPerOrder", profile.fnoFuturesFlatPerOrder.toPlainString())
                put("fnoOptionsFlatPerOrder", profile.fnoOptionsFlatPerOrder.toPlainString())
                put("commodityFlatPerOrder", profile.commodityFlatPerOrder.toPlainString())
                put("dpChargesPerSell", profile.dpChargesPerSell.toPlainString())
            }.toString()
        }

        fun decodeRateProfile(raw: String): com.example.engine.BrokerRateProfile? {
            if (raw.isBlank()) return null
            return runCatching {
                val obj = JSONObject(raw)
                val brokerName = obj.optString("brokerName", "Zerodha")
                val base = com.example.engine.BrokerRateProfile.forBroker(brokerName)
                base.copy(
                    brokerName = brokerName,
                    eqDeliveryBrokeragePct = BigDecimal(obj.optString("eqDeliveryBrokeragePct", base.eqDeliveryBrokeragePct.toPlainString())),
                    eqDeliveryFlatMax = BigDecimal(obj.optString("eqDeliveryFlatMax", base.eqDeliveryFlatMax.toPlainString())),
                    eqIntradayBrokeragePct = BigDecimal(obj.optString("eqIntradayBrokeragePct", base.eqIntradayBrokeragePct.toPlainString())),
                    eqIntradayFlatPerOrder = BigDecimal(obj.optString("eqIntradayFlatPerOrder", base.eqIntradayFlatPerOrder.toPlainString())),
                    fnoFuturesFlatPerOrder = BigDecimal(obj.optString("fnoFuturesFlatPerOrder", base.fnoFuturesFlatPerOrder.toPlainString())),
                    fnoOptionsFlatPerOrder = BigDecimal(obj.optString("fnoOptionsFlatPerOrder", base.fnoOptionsFlatPerOrder.toPlainString())),
                    commodityFlatPerOrder = BigDecimal(obj.optString("commodityFlatPerOrder", base.commodityFlatPerOrder.toPlainString())),
                    dpChargesPerSell = BigDecimal(obj.optString("dpChargesPerSell", base.dpChargesPerSell.toPlainString()))
                )
            }.getOrNull()
        }

        fun encodeLotSizes(map: Map<String, Int>): String {
            if (map.isEmpty()) return ""
            return JSONObject().apply {
                map.forEach { (k, v) -> put(k, v) }
            }.toString()
        }

        fun decodeLotSizes(raw: String): Map<String, Int> {
            if (raw.isBlank()) return com.example.engine.DefaultLotSizes.seeded()
            return runCatching {
                val obj = JSONObject(raw)
                val out = linkedMapOf<String, Int>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val size = obj.optInt(key, 1)
                    if (key.isNotBlank() && size > 0) out[key] = size
                }
                if (out.isEmpty()) com.example.engine.DefaultLotSizes.seeded() else out
            }.getOrDefault(com.example.engine.DefaultLotSizes.seeded())
        }
    }
}

@Entity(tableName = "capital_entries")
data class CapitalEntryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: String,
    val date: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CapitalEntry = CapitalEntry(
        id = id,
        type = CapitalEntryType.valueOf(type),
        amount = BigDecimal(amount),
        date = date,
        note = note
    )

    companion object {
        fun fromDomain(entry: CapitalEntry): CapitalEntryEntity = CapitalEntryEntity(
            id = entry.id,
            type = entry.type.name,
            amount = entry.amount.toPlainString(),
            date = entry.date,
            note = entry.note
        )
    }
}
