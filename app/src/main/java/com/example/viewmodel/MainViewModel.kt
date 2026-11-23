package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TradeDataRepository
import com.example.data.TraderPreferences
import com.example.engine.BrokerRateProfile
import com.example.engine.MatchingMethod
import com.example.model.CapitalEntry
import com.example.model.CapitalEntryType
import com.example.model.Trade
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MainViewModel(private val repository: TradeDataRepository) : ViewModel() {

    val trades: StateFlow<List<Trade>> = repository.trades
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Nullable initial value eliminates first-frame onboarding flash before SQLite emits
    val preferencesState: StateFlow<TraderPreferences?> = repository.preferences
        .map<TraderPreferences, TraderPreferences?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val preferences: StateFlow<TraderPreferences> = repository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TraderPreferences()
        )

    val capitalEntries: StateFlow<List<CapitalEntry>> = repository.capitalEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val effectiveCapital: StateFlow<BigDecimal> = combine(preferences, capitalEntries) { prefs, entries ->
        entries.fold(prefs.startingCapital) { acc, entry ->
            if (entry.type == CapitalEntryType.DEPOSIT) acc.add(entry.amount) else acc.subtract(entry.amount)
        }.coerceAtLeast(BigDecimal.ZERO)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BigDecimal("500000")
    )

    val brokerProfile: StateFlow<BrokerRateProfile> = preferences
        .map { it.activeRateProfile }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrokerRateProfile.forBroker("Zerodha")
        )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun saveTrade(trade: Trade) {
        viewModelScope.launch { repository.saveTrade(trade) }
    }

    fun saveTrades(trades: List<Trade>) {
        viewModelScope.launch { repository.saveTrades(trades) }
    }

    fun deleteTrade(id: String) {
        viewModelScope.launch { repository.deleteTrade(id) }
    }

    fun clearAllTrades() {
        viewModelScope.launch { repository.clearAllTrades() }
    }

    fun wipeAllData() {
        viewModelScope.launch { repository.wipeAllData() }
    }

    fun addCapitalEntry(entry: CapitalEntry) {
        viewModelScope.launch { repository.addCapitalEntry(entry) }
    }

    fun deleteCapitalEntry(id: String) {
        viewModelScope.launch { repository.deleteCapitalEntry(id) }
    }

    fun completeOnboarding(name: String, broker: String, startingCapital: BigDecimal) {
        viewModelScope.launch { repository.completeOnboarding(name, broker, startingCapital) }
    }

    fun updateTraderName(name: String) {
        viewModelScope.launch { repository.updateTraderName(name) }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch { repository.updateTheme(isDark) }
    }

    fun updateStartingCapital(amount: BigDecimal) {
        viewModelScope.launch { repository.updateStartingCapital(amount) }
    }

    fun updateDailyLossLimit(amount: BigDecimal) {
        viewModelScope.launch { repository.updateDailyLossLimit(amount) }
    }

    fun updateBroker(broker: String) {
        viewModelScope.launch { repository.updateBroker(broker) }
    }

    fun updateCustomRateProfile(profile: BrokerRateProfile?) {
        viewModelScope.launch { repository.updateCustomRateProfile(profile) }
    }

    fun updateMatchingMethod(method: MatchingMethod) {
        viewModelScope.launch { repository.updateMatchingMethod(method) }
    }

    fun updateLotSizes(lotSizes: Map<String, Int>) {
        viewModelScope.launch { repository.updateLotSizes(lotSizes) }
    }

    class Factory(private val repository: TradeDataRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
