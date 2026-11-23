package com.example.data

import com.example.db.AppDatabase
import com.example.db.CapitalEntryEntity
import com.example.db.PreferencesEntity
import com.example.db.TradeEntity
import com.example.engine.BrokerRateProfile
import com.example.engine.DefaultLotSizes
import com.example.engine.MatchingMethod
import com.example.model.CapitalEntry
import com.example.model.Trade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

data class TraderPreferences(
    val traderName: String = "",
    val isDarkTheme: Boolean = false,
    val startingCapital: BigDecimal = BigDecimal("500000"),
    val dailyLossLimit: BigDecimal = BigDecimal("10000"),
    val defaultBroker: String = "Zerodha",
    val hasCompletedOnboarding: Boolean = false,
    val matchingMethod: MatchingMethod = MatchingMethod.FIFO,
    val customRateProfile: BrokerRateProfile? = null,
    val lotSizes: Map<String, Int> = DefaultLotSizes.seeded()
) {
    val activeRateProfile: BrokerRateProfile
        get() = customRateProfile ?: BrokerRateProfile.forBroker(defaultBroker)
}

interface TradeDataRepository {
    val trades: Flow<List<Trade>>
    val preferences: Flow<TraderPreferences>
    val capitalEntries: Flow<List<CapitalEntry>>

    suspend fun ensureInitialized()
    suspend fun saveTrade(trade: Trade)
    suspend fun saveTrades(trades: List<Trade>)
    suspend fun deleteTrade(id: String)
    suspend fun clearAllTrades()
    suspend fun wipeAllData()
    suspend fun addCapitalEntry(entry: CapitalEntry)
    suspend fun deleteCapitalEntry(id: String)
    suspend fun completeOnboarding(name: String, broker: String, startingCapital: BigDecimal)
    suspend fun updateTraderName(name: String)
    suspend fun updateTheme(isDark: Boolean)
    suspend fun updateStartingCapital(amount: BigDecimal)
    suspend fun updateDailyLossLimit(amount: BigDecimal)
    suspend fun updateBroker(broker: String)
    suspend fun updateCustomRateProfile(profile: BrokerRateProfile?)
    suspend fun updateMatchingMethod(method: MatchingMethod)
    suspend fun updateLotSizes(lotSizes: Map<String, Int>)
}

class RoomTradeRepository(private val db: AppDatabase) : TradeDataRepository {
    private val tradeDao = db.tradeDao()
    private val prefsDao = db.preferencesDao()
    private val capitalDao = db.capitalDao()

    override val trades: Flow<List<Trade>> =
        tradeDao.observeAllTrades().map { list -> list.map { it.toDomain() } }

    override val preferences: Flow<TraderPreferences> =
        prefsDao.observePreferences().map { it?.toDomain() ?: TraderPreferences() }

    override val capitalEntries: Flow<List<CapitalEntry>> =
        capitalDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureInitialized() {
        val existingPrefs = prefsDao.getPreferencesOnce()
        if (existingPrefs == null) {
            prefsDao.upsertPreferences(PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false))
        }
    }

    override suspend fun saveTrade(trade: Trade) {
        tradeDao.upsertTrade(TradeEntity.fromDomain(trade))
    }

    override suspend fun saveTrades(trades: List<Trade>) {
        if (trades.isEmpty()) return
        val now = System.currentTimeMillis()
        tradeDao.upsertTrades(trades.mapIndexed { idx, trade ->
            TradeEntity.fromDomain(trade, createdAt = now - idx * 10L)
        })
    }

    override suspend fun deleteTrade(id: String) {
        tradeDao.deleteTradeById(id)
    }

    override suspend fun clearAllTrades() {
        tradeDao.clearAllTrades()
    }

    override suspend fun wipeAllData() {
        tradeDao.clearAllTrades()
        capitalDao.clearAll()
    }

    override suspend fun addCapitalEntry(entry: CapitalEntry) {
        capitalDao.upsert(CapitalEntryEntity.fromDomain(entry))
    }

    override suspend fun deleteCapitalEntry(id: String) {
        capitalDao.deleteById(id)
    }

    override suspend fun completeOnboarding(name: String, broker: String, startingCapital: BigDecimal) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(
            current.copy(
                traderName = name.trim(),
                defaultBroker = broker,
                startingCapital = startingCapital.toPlainString(),
                hasCompletedOnboarding = true,
                customRatesJson = ""
            )
        )
    }

    override suspend fun updateTraderName(name: String) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(current.copy(traderName = name.trim()))
    }

    override suspend fun updateTheme(isDark: Boolean) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(current.copy(isDarkTheme = isDark))
    }

    override suspend fun updateStartingCapital(amount: BigDecimal) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(current.copy(startingCapital = amount.toPlainString()))
    }

    override suspend fun updateDailyLossLimit(amount: BigDecimal) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(current.copy(dailyLossLimit = amount.toPlainString()))
    }

    override suspend fun updateBroker(broker: String) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(current.copy(defaultBroker = broker, customRatesJson = ""))
    }

    override suspend fun updateCustomRateProfile(profile: BrokerRateProfile?) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(
            current.copy(customRatesJson = PreferencesEntity.encodeRateProfile(profile))
        )
    }

    override suspend fun updateMatchingMethod(method: MatchingMethod) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(current.copy(matchingMethod = method.name))
    }

    override suspend fun updateLotSizes(lotSizes: Map<String, Int>) {
        val current = prefsDao.getPreferencesOnce() ?: PreferencesEntity.fromDomain(TraderPreferences(), hasSeededDemo = false)
        prefsDao.upsertPreferences(
            current.copy(lotSizesJson = PreferencesEntity.encodeLotSizes(lotSizes))
        )
    }
}
