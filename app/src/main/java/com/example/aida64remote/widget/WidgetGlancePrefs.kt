package com.example.aida64remote.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Glance session state keys — UI must read these via currentState(), not a frozen DataStore snapshot. */
object WidgetGlancePrefs {
    val cpuTemp = stringPreferencesKey("g_cpu_temp")
    val cpuUsage = stringPreferencesKey("g_cpu_usage")
    val cpuUsageBar = floatPreferencesKey("g_cpu_usage_bar")
    val gpuTemp = stringPreferencesKey("g_gpu_temp")
    val gpuUsage = stringPreferencesKey("g_gpu_usage")
    val gpuUsageBar = floatPreferencesKey("g_gpu_usage_bar")
    val status = stringPreferencesKey("g_status")
    val message = stringPreferencesKey("g_message")
    val updatedAt = longPreferencesKey("g_updated_at")

    fun Preferences.toUiState(): WidgetUiState {
        return WidgetUiState(
            cpuTemp = this[cpuTemp] ?: "—",
            cpuUsage = this[cpuUsage] ?: "—",
            cpuUsageBar = this[cpuUsageBar] ?: 0f,
            gpuTemp = this[gpuTemp] ?: "—",
            gpuUsage = this[gpuUsage] ?: "—",
            gpuUsageBar = this[gpuUsageBar] ?: 0f,
            status = this[status]
                ?.let { runCatching { WidgetConnStatus.valueOf(it) }.getOrNull() }
                ?: WidgetConnStatus.Idle,
            message = this[message].orEmpty(),
            updatedAtEpochMs = this[updatedAt] ?: 0L,
        )
    }

    fun MutablePreferences.write(state: WidgetUiState) {
        this[cpuTemp] = state.cpuTemp
        this[cpuUsage] = state.cpuUsage
        this[cpuUsageBar] = state.cpuUsageBar
        this[gpuTemp] = state.gpuTemp
        this[gpuUsage] = state.gpuUsage
        this[gpuUsageBar] = state.gpuUsageBar
        this[status] = state.status.name
        this[message] = state.message
        this[updatedAt] = state.updatedAtEpochMs
    }
}
