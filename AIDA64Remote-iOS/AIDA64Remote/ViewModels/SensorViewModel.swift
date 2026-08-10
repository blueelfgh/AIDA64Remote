import Foundation
import Observation

@MainActor
@Observable
final class SensorViewModel {
    private static let historySize = 48

    private let client = Aida64SseClient()
    let settings: SettingsStore

    private(set) var uiState = MonitorUiState()
    private var connectionTask: Task<Void, Never>?
    private var fpsHistory: [Float] = []
    private var gpuHistory: [Float] = []

    init(settings: SettingsStore = SettingsStore()) {
        self.settings = settings
        syncSettingsIntoState()
    }

    var savedConfig: ConnectionConfig {
        settings.connection
    }

    func syncSettingsIntoState() {
        uiState.config = settings.connection
        uiState.keepScreenOn = settings.keepScreenOn
    }

    func saveAndConnect(host: String, port: Int) {
        let config = ConnectionConfig(host: host.trimmingCharacters(in: .whitespacesAndNewlines), port: port)
        settings.saveConnection(config)
        connect(config: config)
    }

    func setKeepScreenOn(_ enabled: Bool) {
        settings.keepScreenOn = enabled
        uiState.keepScreenOn = enabled
    }

    func setThemeMode(_ mode: ThemeMode) {
        settings.themeMode = mode
    }

    func toggleFullscreen() {
        uiState.isFullscreen.toggle()
    }

    func setFullscreen(_ enabled: Bool) {
        uiState.isFullscreen = enabled
    }

    func connect(config: ConnectionConfig? = nil) {
        let resolved = config ?? uiState.config
        connectionTask?.cancel()
        fpsHistory.removeAll()
        gpuHistory.removeAll()
        uiState.status = .connecting
        uiState.dashboard = DashboardSnapshot()
        uiState.errorMessage = nil
        uiState.config = resolved

        connectionTask = Task { [weak self] in
            guard let self else { return }
            let stream = await client.connect(config: resolved)
            for await event in stream {
                if Task.isCancelled { break }
                handle(event)
            }
        }
    }

    func disconnect() {
        connectionTask?.cancel()
        connectionTask = nil
        fpsHistory.removeAll()
        gpuHistory.removeAll()
        uiState.status = .idle
        uiState.dashboard = DashboardSnapshot()
        uiState.errorMessage = nil
        uiState.isFullscreen = false
    }

    private func handle(_ event: ConnectionEvent) {
        switch event {
        case .connecting:
            uiState.status = .connecting
            uiState.errorMessage = nil
        case .connected:
            uiState.status = .connected
            uiState.errorMessage = nil
        case let .sensorsUpdated(sensors, labels):
            let values = SensorParser.toMap(sensors)
            pushHistory(&fpsHistory, raw: values["Gph23p"] ?? values["SIV22"])
            pushHistory(&gpuHistory, raw: values["Gph24p"] ?? values["SIV9"])
            uiState.status = .connected
            uiState.dashboard = values.toDashboard(
                labels: labels,
                fpsHistory: fpsHistory,
                gpuHistory: gpuHistory
            )
            uiState.errorMessage = nil
        case let .reconnecting(attempt, message):
            _ = attempt
            uiState.status = .reconnecting
            uiState.errorMessage = message
        case let .disconnected(message):
            uiState.status = .error
            uiState.errorMessage = message
        }
    }

    private func pushHistory(_ history: inout [Float], raw: String?) {
        guard let raw, let value = Float(raw) else { return }
        if history.count >= Self.historySize {
            history.removeFirst()
        }
        history.append(Swift.max(value, 0))
    }
}
