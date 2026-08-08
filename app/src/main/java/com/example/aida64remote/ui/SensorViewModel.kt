package com.example.aida64remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aida64remote.data.Aida64SseClient
import com.example.aida64remote.data.AppSettings
import com.example.aida64remote.data.ConnectionConfig
import com.example.aida64remote.data.SensorParser
import com.example.aida64remote.data.SettingsRepository
import com.example.aida64remote.data.ThemeMode
import com.example.aida64remote.model.ConnectionEvent
import com.example.aida64remote.model.ConnectionStatus
import com.example.aida64remote.model.DashboardSnapshot
import com.example.aida64remote.model.toDashboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonitorUiState(
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val dashboard: DashboardSnapshot = DashboardSnapshot(),
    val errorMessage: String? = null,
    val config: ConnectionConfig = ConnectionConfig(),
    val keepScreenOn: Boolean = false,
    val isFullscreen: Boolean = false,
)

class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val client = Aida64SseClient()

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    val savedConfig: StateFlow<ConnectionConfig> = appSettings
        .map { it.connection }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionConfig(),
        )

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var connectionJob: Job? = null
    private val fpsHistory = ArrayDeque<Float>(HISTORY_SIZE)
    private val gpuHistory = ArrayDeque<Float>(HISTORY_SIZE)

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        config = settings.connection,
                        keepScreenOn = settings.keepScreenOn,
                    )
                }
            }
        }
    }

    fun saveAndConnect(host: String, port: Int) {
        val config = ConnectionConfig(host = host.trim(), port = port)
        viewModelScope.launch {
            settingsRepository.saveConnection(config)
            connect(config)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(
                if (enabled) ThemeMode.System else ThemeMode.Dark,
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun setFullscreen(enabled: Boolean) {
        _uiState.update { it.copy(isFullscreen = enabled) }
    }

    fun connect(config: ConnectionConfig = _uiState.value.config) {
        connectionJob?.cancel()
        fpsHistory.clear()
        gpuHistory.clear()
        _uiState.update {
            it.copy(
                status = ConnectionStatus.Connecting,
                dashboard = DashboardSnapshot(),
                errorMessage = null,
                config = config,
            )
        }
        connectionJob = viewModelScope.launch {
            client.connect(config).collect { event ->
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
                        pushHistory(fpsHistory, values["Gph24p"] ?: values["SIV23"])
                        pushHistory(gpuHistory, values["Gph25p"] ?: values["SIV10"])
                        val dashboard = values.toDashboard(
                            labels = event.labels,
                            fpsHistory = fpsHistory.toList(),
                            gpuHistory = gpuHistory.toList(),
                        )
                        _uiState.update {
                            it.copy(
                                status = ConnectionStatus.Connected,
                                dashboard = dashboard,
                                errorMessage = null,
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
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        fpsHistory.clear()
        gpuHistory.clear()
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
