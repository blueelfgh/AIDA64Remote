package com.example.aida64remote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val portKey = intPreferencesKey("port")
    private val serviceTypeKey = stringPreferencesKey("service_type")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val followSystemThemeKey = booleanPreferencesKey("follow_system_theme")

    private val peakCpuClockKey = floatPreferencesKey("peak_cpu_clock")
    private val peakCpuUsageKey = floatPreferencesKey("peak_cpu_usage")
    private val peakVramUsedKey = floatPreferencesKey("peak_vram_used")
    private val peakVramFreeKey = floatPreferencesKey("peak_vram_free")
    private val peakGpuClockKey = floatPreferencesKey("peak_gpu_clock")
    private val peakGpuMemClockKey = floatPreferencesKey("peak_gpu_mem_clock")
    private val peakGpuUsageKey = floatPreferencesKey("peak_gpu_usage")
    private val peakGpuTempKey = floatPreferencesKey("peak_gpu_temp")
    private val peakRamUsedKey = floatPreferencesKey("peak_ram_used")
    private val peakRamFreeKey = floatPreferencesKey("peak_ram_free")
    private val peakRamUsageKey = floatPreferencesKey("peak_ram_usage")
    private val peakDriveCKey = floatPreferencesKey("peak_drive_c")
    private val peakDriveDKey = floatPreferencesKey("peak_drive_d")
    private val peakDriveEKey = floatPreferencesKey("peak_drive_e")
    private val peakVolumeKey = floatPreferencesKey("peak_volume")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val serviceType = prefs[serviceTypeKey]
            ?.let { runCatching { ServiceType.valueOf(it) }.getOrNull() }
            ?: ServiceType.Aida64
        AppSettings(
            host = prefs[hostKey] ?: ConnectionConfig.DEFAULT_HOST,
            port = prefs[portKey] ?: serviceType.defaultPort,
            serviceType = serviceType,
            keepScreenOn = prefs[keepScreenOnKey] ?: false,
            themeMode = prefs[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: if (prefs[followSystemThemeKey] == true) ThemeMode.System else ThemeMode.Dark,
        )
    }

    val barScalePeaksFlow: Flow<BarScalePeaks> = context.dataStore.data.map { prefs ->
        BarScalePeaks(
            cpuClock = prefs[peakCpuClockKey],
            cpuUsage = prefs[peakCpuUsageKey],
            vramUsed = prefs[peakVramUsedKey],
            vramFree = prefs[peakVramFreeKey],
            gpuClock = prefs[peakGpuClockKey],
            gpuMemClock = prefs[peakGpuMemClockKey],
            gpuUsage = prefs[peakGpuUsageKey],
            gpuTemp = prefs[peakGpuTempKey],
            ramUsed = prefs[peakRamUsedKey],
            ramFree = prefs[peakRamFreeKey],
            ramUsage = prefs[peakRamUsageKey],
            driveC = prefs[peakDriveCKey],
            driveD = prefs[peakDriveDKey],
            driveE = prefs[peakDriveEKey],
            volume = prefs[peakVolumeKey],
        )
    }

    suspend fun saveConnection(config: ConnectionConfig) {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = config.host
            prefs[portKey] = config.port
            prefs[serviceTypeKey] = config.serviceType.name
        }
    }

    suspend fun setServiceType(type: ServiceType, alsoUpdateDefaultPort: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[serviceTypeKey] = type.name
            if (alsoUpdateDefaultPort) {
                prefs[portKey] = type.defaultPort
            }
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

    suspend fun saveBarScalePeaks(peaks: BarScalePeaks) {
        context.dataStore.edit { prefs ->
            fun put(key: Preferences.Key<Float>, value: Float?) {
                if (value == null) prefs.remove(key) else prefs[key] = value
            }
            put(peakCpuClockKey, peaks.cpuClock)
            put(peakCpuUsageKey, peaks.cpuUsage)
            put(peakVramUsedKey, peaks.vramUsed)
            put(peakVramFreeKey, peaks.vramFree)
            put(peakGpuClockKey, peaks.gpuClock)
            put(peakGpuMemClockKey, peaks.gpuMemClock)
            put(peakGpuUsageKey, peaks.gpuUsage)
            put(peakGpuTempKey, peaks.gpuTemp)
            put(peakRamUsedKey, peaks.ramUsed)
            put(peakRamFreeKey, peaks.ramFree)
            put(peakRamUsageKey, peaks.ramUsage)
            put(peakDriveCKey, peaks.driveC)
            put(peakDriveDKey, peaks.driveD)
            put(peakDriveEKey, peaks.driveE)
            put(peakVolumeKey, peaks.volume)
        }
    }

    suspend fun clearBarScalePeaks() {
        saveBarScalePeaks(BarScalePeaks())
    }

    suspend fun save(config: ConnectionConfig) = saveConnection(config)
}
