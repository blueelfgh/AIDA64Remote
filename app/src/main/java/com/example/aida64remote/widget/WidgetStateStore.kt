package com.example.aida64remote.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aida64remote.model.DashboardSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "widget_state")

enum class WidgetConnStatus {
    Idle,
    Ok,
    Updating,
    Error,
}

data class WidgetUiState(
    val cpuTemp: String = "—",
    val cpuUsage: String = "—",
    val cpuUsageBar: Float = 0f,
    val gpuTemp: String = "—",
    val gpuUsage: String = "—",
    val gpuUsageBar: Float = 0f,
    val status: WidgetConnStatus = WidgetConnStatus.Idle,
    val message: String = "",
    val updatedAtEpochMs: Long = 0L,
)

class WidgetStateStore(private val context: Context) {
    private val cpuTempKey = stringPreferencesKey("cpu_temp")
    private val cpuUsageKey = stringPreferencesKey("cpu_usage")
    private val cpuUsageBarKey = floatPreferencesKey("cpu_usage_bar")
    private val gpuTempKey = stringPreferencesKey("gpu_temp")
    private val gpuUsageKey = stringPreferencesKey("gpu_usage")
    private val gpuUsageBarKey = floatPreferencesKey("gpu_usage_bar")
    private val statusKey = stringPreferencesKey("status")
    private val messageKey = stringPreferencesKey("message")
    private val updatedAtKey = longPreferencesKey("updated_at")

    val stateFlow: Flow<WidgetUiState> = context.widgetDataStore.data.map { prefs ->
        WidgetUiState(
            cpuTemp = prefs[cpuTempKey] ?: "—",
            cpuUsage = prefs[cpuUsageKey] ?: "—",
            cpuUsageBar = prefs[cpuUsageBarKey] ?: 0f,
            gpuTemp = prefs[gpuTempKey] ?: "—",
            gpuUsage = prefs[gpuUsageKey] ?: "—",
            gpuUsageBar = prefs[gpuUsageBarKey] ?: 0f,
            status = prefs[statusKey]
                ?.let { runCatching { WidgetConnStatus.valueOf(it) }.getOrNull() }
                ?: WidgetConnStatus.Idle,
            message = prefs[messageKey].orEmpty(),
            updatedAtEpochMs = prefs[updatedAtKey] ?: 0L,
        )
    }

    suspend fun current(): WidgetUiState = stateFlow.first()

    suspend fun saveSuccess(snapshot: DashboardSnapshot) {
        context.widgetDataStore.edit { prefs ->
            prefs[cpuTempKey] = snapshot.cpuTemp
            prefs[cpuUsageKey] = snapshot.cpuUsage
            prefs[cpuUsageBarKey] = snapshot.cpuUsageBar.coerceIn(0f, 1f)
                .takeIf { it > 0f }
                ?: parseUsageBar(snapshot.cpuUsage)
            prefs[gpuTempKey] = snapshot.gpuTemp
            prefs[gpuUsageKey] = snapshot.gpuUsage
            prefs[gpuUsageBarKey] = snapshot.gpuUsageBar.coerceIn(0f, 1f)
                .takeIf { it > 0f }
                ?: parseUsageBar(snapshot.gpuUsage)
            prefs[statusKey] = WidgetConnStatus.Ok.name
            prefs[messageKey] = ""
            prefs[updatedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun saveUpdating() {
        context.widgetDataStore.edit { prefs ->
            prefs[statusKey] = WidgetConnStatus.Updating.name
        }
    }

    suspend fun saveError(message: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[statusKey] = WidgetConnStatus.Error.name
            prefs[messageKey] = message
            prefs[updatedAtKey] = System.currentTimeMillis()
        }
    }

    companion object {
        fun parseUsageBar(raw: String): Float {
            val num = raw.replace("%", "").trim().toFloatOrNull() ?: return 0f
            return (if (num > 1f) num / 100f else num).coerceIn(0f, 1f)
        }
    }
}
