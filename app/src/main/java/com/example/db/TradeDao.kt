package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY date DESC, createdAt DESC")
    fun observeAllTrades(): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrade(trade: TradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrades(trades: List<TradeEntity>)

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteTradeById(id: String)

    @Query("DELETE FROM trades")
    suspend fun clearAllTrades()

    @Query("SELECT COUNT(*) FROM trades")
    suspend fun countTrades(): Int
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE id = 1")
    fun observePreferences(): Flow<PreferencesEntity?>

    @Query("SELECT * FROM preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): PreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: PreferencesEntity)
}

@Dao
interface CapitalDao {
    @Query("SELECT * FROM capital_entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<CapitalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CapitalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<CapitalEntryEntity>)

    @Query("DELETE FROM capital_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM capital_entries")
    suspend fun clearAll()
}
