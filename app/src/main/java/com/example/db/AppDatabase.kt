package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TradeEntity::class, PreferencesEntity::class, CapitalEntryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun capitalDao(): CapitalDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `capital_entries` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `preferences` ADD COLUMN `hasCompletedOnboarding` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `preferences` ADD COLUMN `matchingMethod` TEXT NOT NULL DEFAULT 'FIFO'")
                db.execSQL("ALTER TABLE `preferences` ADD COLUMN `customRatesJson` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `preferences` ADD COLUMN `lotSizesJson` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Upgrade previously seeded defaults ("NIFTY":25) to NIFTY 50 (165), SENSEX (20), BANKNIFTY (30)
                db.execSQL("UPDATE `preferences` SET `lotSizesJson` = '' WHERE `lotSizesJson` LIKE '%\"NIFTY\":25%'")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tradejournal.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
