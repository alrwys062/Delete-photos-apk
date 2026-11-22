package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timedelete_settings"
)

enum class ThemeMode(val label: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    SYSTEM("System Default")
}

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    ARABIC("ar")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val isPausedAll: Boolean = false,
    val backgroundMonitorEnabled: Boolean = true,
    val notifyScheduled: Boolean = true,
    val notifyApproaching: Boolean = true,
    val notifyExpired: Boolean = true,
    val notifyAwaitingConfirmation: Boolean = true,
    val notifyCompleted: Boolean = true,
    val notifyFailed: Boolean = true,
    val approachingLeadMinutes: Int = 15
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val IS_PAUSED_ALL = booleanPreferencesKey("is_paused_all")
        val BACKGROUND_MONITOR = booleanPreferencesKey("background_monitor")
        val NOTIFY_SCHEDULED = booleanPreferencesKey("notify_scheduled")
        val NOTIFY_APPROACHING = booleanPreferencesKey("notify_approaching")
        val NOTIFY_EXPIRED = booleanPreferencesKey("notify_expired")
        val NOTIFY_AWAITING_CONFIRMATION = booleanPreferencesKey("notify_awaiting_confirmation")
        val NOTIFY_COMPLETED = booleanPreferencesKey("notify_completed")
        val NOTIFY_FAILED = booleanPreferencesKey("notify_failed")
        val APPROACHING_LEAD_MINUTES = intPreferencesKey("approaching_lead_minutes")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val theme = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.DARK
        val langName = prefs[Keys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name
        val lang = AppLanguage.entries.firstOrNull { it.name == langName } ?: AppLanguage.SYSTEM
        AppSettings(
            themeMode = theme,
            appLanguage = lang,
            isPausedAll = prefs[Keys.IS_PAUSED_ALL] ?: false,
            backgroundMonitorEnabled = prefs[Keys.BACKGROUND_MONITOR] ?: true,
            notifyScheduled = prefs[Keys.NOTIFY_SCHEDULED] ?: true,
            notifyApproaching = prefs[Keys.NOTIFY_APPROACHING] ?: true,
            notifyExpired = prefs[Keys.NOTIFY_EXPIRED] ?: true,
            notifyAwaitingConfirmation = prefs[Keys.NOTIFY_AWAITING_CONFIRMATION] ?: true,
            notifyCompleted = prefs[Keys.NOTIFY_COMPLETED] ?: true,
            notifyFailed = prefs[Keys.NOTIFY_FAILED] ?: true,
            approachingLeadMinutes = prefs[Keys.APPROACHING_LEAD_MINUTES] ?: 15
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setPausedAll(paused: Boolean) {
        context.settingsDataStore.edit { it[Keys.IS_PAUSED_ALL] = paused }
    }

    suspend fun setBackgroundMonitorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BACKGROUND_MONITOR] = enabled }
    }

    suspend fun setNotifyScheduled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFY_SCHEDULED] = enabled }
    }

    suspend fun setNotifyApproaching(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFY_APPROACHING] = enabled }
    }

    suspend fun setNotifyExpired(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFY_EXPIRED] = enabled }
    }

    suspend fun setNotifyAwaitingConfirmation(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFY_AWAITING_CONFIRMATION] = enabled }
    }

    suspend fun setNotifyCompleted(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFY_COMPLETED] = enabled }
    }

    suspend fun setNotifyFailed(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFY_FAILED] = enabled }
    }

    suspend fun setApproachingLeadMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.APPROACHING_LEAD_MINUTES] = minutes.coerceIn(5, 120) }
    }
}
