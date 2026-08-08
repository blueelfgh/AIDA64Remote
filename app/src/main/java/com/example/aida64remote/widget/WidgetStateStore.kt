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
    val hostLabel: String = "",
    val cpuTemp: String = "—",
    val cpuUsage: String = "—",
    val cpuUsageBar: Float = 0f,
    val cpuClock: String = "—",
    val gpuTemp: String = "—",
    val gpuUsage: String = "—",
    val gpuUsageBar: Float = 0f,
    val vramUsed: String = "—",
    val ramUsage: String = "—",
    val ramUsageBar: Float = 0f,
    val ramUsed: String = "—",
    val upload: String = "—",
    val download: String = "—",
    val status: WidgetConnStatus = WidgetConnStatus.Idle,
    val message: String = "",
    val updatedAtEpochMs: Long = 0L,
)

class WidgetStateStore(private val context: Context) {
    private val hostKey = stringPreferencesKey("host_label")
    private val cpuTempKey = stringPreferencesKey("cpu_temp")
    private val cpuUsageKey = stringPreferencesKey("cpu_usage")
    private val cpuUsageBarKey = floatPreferencesKey("cpu_usage_bar")
    private val cpuClockKey = stringPreferencesKey("cpu_clock")
    private val gpuTempKey = stringPreferencesKey("gpu_temp")
    private val gpuUsageKey = stringPreferencesKey("gpu_usage")
    private val gpuUsageBarKey = floatPreferencesKey("gpu_usage_bar")
    private val vramUsedKey = stringPreferencesKey("vram_used")
    private val ramUsageKey = stringPreferencesKey("ram_usage")
    private val ramUsageBarKey = floatPreferencesKey("ram_usage_bar")
    private val ramUsedKey = stringPreferencesKey("ram_used")
    private val uploadKey = stringPreferencesKey("upload")
    private val downloadKey = stringPreferencesKey("download")
    private val statusKey = stringPreferencesKey("status")
    private val messageKey = stringPreferencesKey("message")
    private val updatedAtKey = longPreferencesKey("updated_at")

    val stateFlow: Flow<WidgetUiState> = context.widgetDataStore.data.map { prefs ->
        WidgetUiState(
            hostLabel = prefs[hostKey].orEmpty(),
            cpuTemp = prefs[cpuTempKey] ?: "—",
            cpuUsage = prefs[cpuUsageKey] ?: "—",
            cpuUsageBar = prefs[cpuUsageBarKey] ?: 0f,
            cpuClock = prefs[cpuClockKey] ?: "—",
            gpuTemp = prefs[gpuTempKey] ?: "—",
            gpuUsage = prefs[gpuUsageKey] ?: "—",
            gpuUsageBar = prefs[gpuUsageBarKey] ?: 0f,
            vramUsed = prefs[vramUsedKey] ?: "—",
            ramUsage = prefs[ramUsageKey] ?: "—",
            ramUsageBar = prefs[ramUsageBarKey] ?: 0f,
            ramUsed = prefs[ramUsedKey] ?: "—",
            upload = prefs[uploadKey] ?: "—",
            download = prefs[downloadKey] ?: "—",
            status = prefs[statusKey]
                ?.let { runCatching { WidgetConnStatus.valueOf(it) }.getOrNull() }
                ?: WidgetConnStatus.Idle,
            message = prefs[messageKey].orEmpty(),
            updatedAtEpochMs = prefs[updatedAtKey] ?: 0L,
        )
    }

    suspend fun current(): WidgetUiState = stateFlow.first()

    suspend fun saveSuccess(snapshot: DashboardSnapshot, hostLabel: String = "") {
        context.widgetDataStore.edit { prefs ->
            prefs[hostKey] = hostLabel
            prefs[cpuTempKey] = snapshot.cpuTemp
            prefs[cpuUsageKey] = snapshot.cpuUsage
            prefs[cpuUsageBarKey] = snapshot.cpuUsageBar.coerceIn(0f, 1f)
                .takeIf { it > 0f }
                ?: parseUsageBar(snapshot.cpuUsage)
            prefs[cpuClockKey] = snapshot.cpuClock
            prefs[gpuTempKey] = snapshot.gpuTemp
            prefs[gpuUsageKey] = snapshot.gpuUsage
            prefs[gpuUsageBarKey] = snapshot.gpuUsageBar.coerceIn(0f, 1f)
                .takeIf { it > 0f }
                ?: parseUsageBar(snapshot.gpuUsage)
            prefs[vramUsedKey] = snapshot.vramUsed
            prefs[ramUsageKey] = snapshot.ramUsage
            prefs[ramUsageBarKey] = snapshot.ramUsageBar.coerceIn(0f, 1f)
                .takeIf { it > 0f }
                ?: parseUsageBar(snapshot.ramUsage)
            prefs[ramUsedKey] = snapshot.ramUsed
            prefs[uploadKey] = snapshot.upload
            prefs[downloadKey] = snapshot.download
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

    suspend fun saveError(message: String, hostLabel: String = "") {
        context.widgetDataStore.edit { prefs ->
            if (hostLabel.isNotBlank()) {
                prefs[hostKey] = hostLabel
            }
            prefs[cpuTempKey] = "—"
            prefs[cpuUsageKey] = "—"
            prefs[cpuUsageBarKey] = 0f
            prefs[cpuClockKey] = "—"
            prefs[gpuTempKey] = "—"
            prefs[gpuUsageKey] = "—"
            prefs[gpuUsageBarKey] = 0f
            prefs[vramUsedKey] = "—"
            prefs[ramUsageKey] = "—"
            prefs[ramUsageBarKey] = 0f
            prefs[ramUsedKey] = "—"
            prefs[uploadKey] = "—"
            prefs[downloadKey] = "—"
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
