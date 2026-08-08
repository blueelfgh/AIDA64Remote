package com.example.aida64remote.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Glance session state keys — UI must read these via currentState(), not a frozen DataStore snapshot. */
object WidgetGlancePrefs {
    val hostLabel = stringPreferencesKey("g_host_label")
    val cpuTemp = stringPreferencesKey("g_cpu_temp")
    val cpuUsage = stringPreferencesKey("g_cpu_usage")
    val cpuUsageBar = floatPreferencesKey("g_cpu_usage_bar")
    val cpuClock = stringPreferencesKey("g_cpu_clock")
    val gpuTemp = stringPreferencesKey("g_gpu_temp")
    val gpuUsage = stringPreferencesKey("g_gpu_usage")
    val gpuUsageBar = floatPreferencesKey("g_gpu_usage_bar")
    val vramUsed = stringPreferencesKey("g_vram_used")
    val ramUsage = stringPreferencesKey("g_ram_usage")
    val ramUsageBar = floatPreferencesKey("g_ram_usage_bar")
    val ramUsed = stringPreferencesKey("g_ram_used")
    val upload = stringPreferencesKey("g_upload")
    val download = stringPreferencesKey("g_download")
    val status = stringPreferencesKey("g_status")
    val message = stringPreferencesKey("g_message")
    val updatedAt = longPreferencesKey("g_updated_at")

    fun Preferences.toUiState(): WidgetUiState {
        return WidgetUiState(
            hostLabel = this[hostLabel].orEmpty(),
            cpuTemp = this[cpuTemp] ?: "—",
            cpuUsage = this[cpuUsage] ?: "—",
            cpuUsageBar = this[cpuUsageBar] ?: 0f,
            cpuClock = this[cpuClock] ?: "—",
            gpuTemp = this[gpuTemp] ?: "—",
            gpuUsage = this[gpuUsage] ?: "—",
            gpuUsageBar = this[gpuUsageBar] ?: 0f,
            vramUsed = this[vramUsed] ?: "—",
            ramUsage = this[ramUsage] ?: "—",
            ramUsageBar = this[ramUsageBar] ?: 0f,
            ramUsed = this[ramUsed] ?: "—",
            upload = this[upload] ?: "—",
            download = this[download] ?: "—",
            status = this[status]
                ?.let { runCatching { WidgetConnStatus.valueOf(it) }.getOrNull() }
                ?: WidgetConnStatus.Idle,
            message = this[message].orEmpty(),
            updatedAtEpochMs = this[updatedAt] ?: 0L,
        )
    }

    fun MutablePreferences.write(state: WidgetUiState) {
        this[hostLabel] = state.hostLabel
        this[cpuTemp] = state.cpuTemp
        this[cpuUsage] = state.cpuUsage
        this[cpuUsageBar] = state.cpuUsageBar
        this[cpuClock] = state.cpuClock
        this[gpuTemp] = state.gpuTemp
        this[gpuUsage] = state.gpuUsage
        this[gpuUsageBar] = state.gpuUsageBar
        this[vramUsed] = state.vramUsed
        this[ramUsage] = state.ramUsage
        this[ramUsageBar] = state.ramUsageBar
        this[ramUsed] = state.ramUsed
        this[upload] = state.upload
        this[download] = state.download
        this[status] = state.status.name
        this[message] = state.message
        this[updatedAt] = state.updatedAtEpochMs
    }
}
