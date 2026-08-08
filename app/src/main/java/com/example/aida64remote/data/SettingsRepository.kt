package com.example.aida64remote.data

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val portKey = intPreferencesKey("port")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val followSystemThemeKey = booleanPreferencesKey("follow_system_theme")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            host = prefs[hostKey] ?: ConnectionConfig.DEFAULT_HOST,
            port = prefs[portKey] ?: ConnectionConfig.DEFAULT_PORT,
            keepScreenOn = prefs[keepScreenOnKey] ?: false,
            themeMode = prefs[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: if (prefs[followSystemThemeKey] == true) ThemeMode.System else ThemeMode.Dark,
        )
    }

    suspend fun saveConnection(config: ConnectionConfig) {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = config.host
            prefs[portKey] = config.port
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keepScreenOnKey] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
            prefs.remove(followSystemThemeKey)
        }
    }

    suspend fun save(config: ConnectionConfig) = saveConnection(config)
}
