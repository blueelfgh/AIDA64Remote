package com.example.aida64remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aida64remote.data.Aida64SseClient
import com.example.aida64remote.data.AppSettings
import com.example.aida64remote.data.BarScalePeaks
import com.example.aida64remote.data.ConnectionConfig
import com.example.aida64remote.data.LhmHttpClient
import com.example.aida64remote.data.SensorParser
import com.example.aida64remote.data.ServiceType
import com.example.aida64remote.data.SettingsRepository
import com.example.aida64remote.data.ThemeMode
import com.example.aida64remote.data.parseSensorNumber
import com.example.aida64remote.model.ConnectionEvent
import com.example.aida64remote.model.ConnectionStatus
import com.example.aida64remote.model.DashboardSnapshot
import com.example.aida64remote.model.toDashboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetricStats(
    val min: Float? = null,
    val max: Float? = null,
    val avg: Float? = null,
)

data class MonitorUiState(
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val dashboard: DashboardSnapshot = DashboardSnapshot(),
    val errorMessage: String? = null,
    val config: ConnectionConfig = ConnectionConfig(),
    val keepScreenOn: Boolean = false,
    val isFullscreen: Boolean = false,
    val cpuTempStats: MetricStats = MetricStats(),
    val gpuTempStats: MetricStats = MetricStats(),
    val ramTemp1Stats: MetricStats = MetricStats(),
    val ramTemp2Stats: MetricStats = MetricStats(),
    val fpsStats: MetricStats = MetricStats(),
    val barPeaks: BarScalePeaks = BarScalePeaks(),
)

class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val aidaClient = Aida64SseClient()
    private val lhmClient = LhmHttpClient()

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings(host = ""),
        )

    val savedConfig: StateFlow<ConnectionConfig> = appSettings
        .map { it.connection }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ConnectionConfig(host = ""),
        )

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    private var connectionJob: Job? = null
    private val fpsHistory = ArrayDeque<Float>(HISTORY_SIZE)
    private val gpuHistory = ArrayDeque<Float>(HISTORY_SIZE)

    private val cpuTempAccumulator = MetricAccumulator()
    private val gpuTempAccumulator = MetricAccumulator()
    private val ramTemp1Accumulator = MetricAccumulator()
    private val ramTemp2Accumulator = MetricAccumulator()
    private val fpsAccumulator = MetricAccumulator()
    private var barPeaks = BarScalePeaks()
    private var persistPeaksJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        config = settings.connection,
                        keepScreenOn = settings.keepScreenOn,
                    )
                }
                _settingsLoaded.value = true
            }
        }
        viewModelScope.launch {
            val peaks = settingsRepository.barScalePeaksFlow.first()
            barPeaks = peaks
            _uiState.update { it.copy(barPeaks = peaks) }
        }
    }

    fun saveAndConnect(host: String, port: Int, serviceType: ServiceType) {
        val config = ConnectionConfig(
            host = host.trim(),
            port = port,
            serviceType = serviceType,
        )
        viewModelScope.launch {
            settingsRepository.saveConnection(config)
            connect(config)
        }
    }

    fun setServiceType(type: ServiceType) {
        val current = _uiState.value.config
        val shouldResetPort =
            current.port == ServiceType.Aida64.defaultPort ||
                current.port == ServiceType.LibreHardwareMonitor.defaultPort
        viewModelScope.launch {
            settingsRepository.setServiceType(type, alsoUpdateDefaultPort = shouldResetPort)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun clearBarScalePeaks() {
        viewModelScope.launch {
            settingsRepository.clearBarScalePeaks()
            barPeaks = BarScalePeaks()
            _uiState.update { it.copy(barPeaks = BarScalePeaks()) }
        }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun setFullscreen(enabled: Boolean) {
        _uiState.update { it.copy(isFullscreen = enabled) }
    }

    fun resetMetricStats() {
        cpuTempAccumulator.reset()
        gpuTempAccumulator.reset()
        ramTemp1Accumulator.reset()
        ramTemp2Accumulator.reset()
        fpsAccumulator.reset()
        _uiState.update {
            it.copy(
                cpuTempStats = MetricStats(),
                gpuTempStats = MetricStats(),
                ramTemp1Stats = MetricStats(),
                ramTemp2Stats = MetricStats(),
                fpsStats = MetricStats(),
            )
        }
    }

    fun connect(config: ConnectionConfig = _uiState.value.config) {
        connectionJob?.cancel()
        fpsHistory.clear()
        gpuHistory.clear()
        resetMetricStats()
        _uiState.update {
            it.copy(
                status = ConnectionStatus.Connecting,
                dashboard = DashboardSnapshot(),
                errorMessage = null,
                config = config,
            )
        }
        val source = when (config.serviceType) {
            ServiceType.Aida64 -> aidaClient.connect(config)
            ServiceType.LibreHardwareMonitor -> lhmClient.connect(config)
        }
        connectionJob = viewModelScope.launch {
            source.collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: ConnectionEvent) {
        when (event) {
            ConnectionEvent.Connecting -> {
                _uiState.update {
                    it.copy(status = ConnectionStatus.Connecting, errorMessage = null)
                }
            }
            ConnectionEvent.Connected -> {
                _uiState.update {
                    it.copy(status = ConnectionStatus.Connected, errorMessage = null)
                }
            }
            is ConnectionEvent.SensorsUpdated -> {
                val values = SensorParser.toMap(event.sensors)
                pushHistory(fpsHistory, values["Gph26"] ?: values["Gph24p"] ?: values["SIV25"] ?: values["SIV23"])
                pushHistory(gpuHistory, values["Gph27"] ?: values["Gph25p"] ?: values["SIV10"])
                val dashboard = values.toDashboard(
                    labels = event.labels,
                    fpsHistory = fpsHistory.toList(),
                    gpuHistory = gpuHistory.toList(),
                )
                recordDashboardMetrics(dashboard)
                val peaks = absorbBarPeaks(dashboard)
                _uiState.update {
                    it.copy(
                        status = ConnectionStatus.Connected,
                        dashboard = dashboard,
                        errorMessage = null,
                        cpuTempStats = cpuTempAccumulator.toStats(),
                        gpuTempStats = gpuTempAccumulator.toStats(),
                        ramTemp1Stats = ramTemp1Accumulator.toStats(),
                        ramTemp2Stats = ramTemp2Accumulator.toStats(),
                        fpsStats = fpsAccumulator.toStats(),
                        barPeaks = peaks,
                    )
                }
            }
            is ConnectionEvent.DashboardUpdated -> {
                val usage = event.dashboard.gpuUsage.toFloatOrNull()
                if (usage != null) {
                    if (gpuHistory.size >= HISTORY_SIZE) gpuHistory.removeFirst()
                    gpuHistory.addLast(usage.coerceAtLeast(0f))
                }
                val dashboard = event.dashboard.copy(
                    fpsHistory = fpsHistory.toList(),
                    gpuHistory = gpuHistory.toList(),
                )
                recordDashboardMetrics(dashboard)
                val peaks = absorbBarPeaks(dashboard)
                _uiState.update {
                    it.copy(
                        status = ConnectionStatus.Connected,
                        dashboard = dashboard,
                        errorMessage = null,
                        cpuTempStats = cpuTempAccumulator.toStats(),
                        gpuTempStats = gpuTempAccumulator.toStats(),
                        ramTemp1Stats = ramTemp1Accumulator.toStats(),
                        ramTemp2Stats = ramTemp2Accumulator.toStats(),
                        fpsStats = fpsAccumulator.toStats(),
                        barPeaks = peaks,
                    )
                }
            }
            is ConnectionEvent.Reconnecting -> {
                _uiState.update {
                    it.copy(
                        status = ConnectionStatus.Reconnecting,
                        errorMessage = event.message,
                    )
                }
            }
            is ConnectionEvent.Disconnected -> {
                _uiState.update {
                    it.copy(
                        status = ConnectionStatus.Error,
                        errorMessage = event.message,
                    )
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        fpsHistory.clear()
        gpuHistory.clear()
        resetMetricStats()
        _uiState.update {
            it.copy(
                status = ConnectionStatus.Idle,
                dashboard = DashboardSnapshot(),
                errorMessage = null,
                isFullscreen = false,
            )
        }
    }

    override fun onCleared() {
        connectionJob?.cancel()
        super.onCleared()
    }

    private fun absorbBarPeaks(dashboard: DashboardSnapshot): BarScalePeaks {
        val (next, changed) = barPeaks.absorb(dashboard)
        if (changed) {
            barPeaks = next
            persistPeaksJob?.cancel()
            persistPeaksJob = viewModelScope.launch {
                settingsRepository.saveBarScalePeaks(next)
            }
        }
        return next
    }

    private fun recordDashboardMetrics(dashboard: DashboardSnapshot) {
        parseSensorNumber(dashboard.cpuTemp)?.let { cpuTempAccumulator.record(it) }
        parseSensorNumber(dashboard.gpuTemp)?.let { gpuTempAccumulator.record(it) }
        parseSensorNumber(dashboard.ramTemp1)?.let { ramTemp1Accumulator.record(it) }
        parseSensorNumber(dashboard.ramTemp2)?.let { ramTemp2Accumulator.record(it) }
        parseSensorNumber(dashboard.fps)?.let { fpsAccumulator.record(it) }
    }

    private fun pushHistory(history: ArrayDeque<Float>, raw: String?) {
        val value = raw?.toFloatOrNull() ?: return
        if (history.size >= HISTORY_SIZE) {
            history.removeFirst()
        }
        history.addLast(value.coerceAtLeast(0f))
    }

    companion object {
        private const val HISTORY_SIZE = 48
    }
}

private class MetricAccumulator {
    private var min: Float? = null
    private var max: Float? = null
    private var sum = 0.0
    private var count = 0

    fun record(value: Float) {
        min = min?.coerceAtMost(value) ?: value
        max = max?.coerceAtLeast(value) ?: value
        sum += value
        count += 1
    }

    fun reset() {
        min = null
        max = null
        sum = 0.0
        count = 0
    }

    fun toStats(): MetricStats {
        if (count == 0) return MetricStats()
        return MetricStats(
            min = min,
            max = max,
            avg = (sum / count).toFloat(),
        )
    }
}
