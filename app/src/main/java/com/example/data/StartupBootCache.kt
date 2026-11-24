package com.example.data

import android.content.Context

/**
 * Ultra-fast SharedPreferences mirror for startup-critical flags so frame #1 renders
 * immediately on cold launch (Dashboard for returning users, Name screen for new installs)
 * without waiting for Room SQLite initialization.
 */
object StartupBootCache {
    private const val PREFS_NAME = "tradejournal_fast_boot"
    private const val KEY_HAS_ONBOARDED = "has_completed_onboarding"
    private const val KEY_TRADER_NAME = "trader_name"
    private const val KEY_IS_DARK = "is_dark_theme"
    private const val KEY_DEFAULT_BROKER = "default_broker"
    private const val DB_NAME = "tradejournal.db"

    fun isDarkThemeFast(context: Context): Boolean {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_IS_DARK, false)
    }

    fun readInitialPreferences(context: Context): TraderPreferences? {
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (sp.contains(KEY_HAS_ONBOARDED)) {
            return TraderPreferences(
                traderName = sp.getString(KEY_TRADER_NAME, "") ?: "",
                isDarkTheme = sp.getBoolean(KEY_IS_DARK, false),
                defaultBroker = sp.getString(KEY_DEFAULT_BROKER, "Zerodha") ?: "Zerodha",
                hasCompletedOnboarding = sp.getBoolean(KEY_HAS_ONBOARDED, false)
            )
        }
        // Brand-new install (no SQLite DB yet): show Name onboarding immediately on frame #1.
        if (!appContext.getDatabasePath(DB_NAME).exists()) {
            return TraderPreferences(hasCompletedOnboarding = false)
        }
        // One-time upgrade path if DB existed before StartupBootCache was introduced.
        return null
    }

    fun sync(context: Context, prefs: TraderPreferences) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_ONBOARDED, prefs.hasCompletedOnboarding)
            .putString(KEY_TRADER_NAME, prefs.traderName)
            .putBoolean(KEY_IS_DARK, prefs.isDarkTheme)
            .putString(KEY_DEFAULT_BROKER, prefs.defaultBroker)
            .apply()
    }
}
