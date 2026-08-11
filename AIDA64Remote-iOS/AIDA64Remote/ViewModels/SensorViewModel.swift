import Foundation
import Observation

@MainActor
@Observable
final class SensorViewModel {
    private static let historySize = 48

    private let aidaClient = Aida64SseClient()
    private let lhmClient = LhmHttpClient()
    let settings: SettingsStore

    private(set) var uiState = MonitorUiState()
    private var connectionTask: Task<Void, Never>?
    private var persistPeaksTask: Task<Void, Never>?
    private var fpsHistory: [Float] = []
    private var gpuHistory: [Float] = []

    private let cpuTempAccumulator = MetricAccumulator()
    private let gpuTempAccumulator = MetricAccumulator()
    private let ramTemp1Accumulator = MetricAccumulator()
    private let ramTemp2Accumulator = MetricAccumulator()
    private let fpsAccumulator = MetricAccumulator()
    private var barPeaks = BarScalePeaks()

    init(settings: SettingsStore = SettingsStore()) {
        self.settings = settings
        self.barPeaks = settings.barScalePeaks
        syncSettingsIntoState()
        uiState.barPeaks = barPeaks
    }

    var savedConfig: ConnectionConfig {
        settings.connection
    }

    func syncSettingsIntoState() {
        uiState.config = settings.connection
        uiState.keepScreenOn = settings.keepScreenOn
        uiState.barPeaks = settings.barScalePeaks
        barPeaks = settings.barScalePeaks
    }

    func saveAndConnect(host: String, port: Int, serviceType: ServiceType) {
        let config = ConnectionConfig(
            host: host.trimmingCharacters(in: .whitespacesAndNewlines),
            port: port,
            serviceType: serviceType
        )
        settings.saveConnection(config)
        connect(config: config)
    }

    func setServiceType(_ type: ServiceType) {
        settings.setServiceType(type)
        uiState.config = settings.connection
    }

    func setKeepScreenOn(_ enabled: Bool) {
        settings.keepScreenOn = enabled
        uiState.keepScreenOn = enabled
    }

    func setThemeMode(_ mode: ThemeMode) {
        settings.themeMode = mode
    }

    func clearBarScalePeaks() {
        settings.clearBarScalePeaks()
        barPeaks = BarScalePeaks()
        uiState.barPeaks = BarScalePeaks()
    }

    func toggleFullscreen() {
        uiState.isFullscreen.toggle()
    }

    func setFullscreen(_ enabled: Bool) {
        uiState.isFullscreen = enabled
    }

    func resetMetricStats() {
        cpuTempAccumulator.reset()
        gpuTempAccumulator.reset()
        ramTemp1Accumulator.reset()
        ramTemp2Accumulator.reset()
        fpsAccumulator.reset()
        uiState.cpuTempStats = MetricStats()
        uiState.gpuTempStats = MetricStats()
        uiState.ramTemp1Stats = MetricStats()
        uiState.ramTemp2Stats = MetricStats()
        uiState.fpsStats = MetricStats()
    }

    func connect(config: ConnectionConfig? = nil) {
        let resolved = config ?? uiState.config
        connectionTask?.cancel()
        fpsHistory.removeAll()
        gpuHistory.removeAll()
        resetMetricStats()
        uiState.status = .connecting
        uiState.dashboard = DashboardSnapshot()
        uiState.errorMessage = nil
        uiState.config = resolved

        connectionTask = Task { [weak self] in
            guard let self else { return }
            let stream: AsyncStream<ConnectionEvent>
            switch resolved.serviceType {
            case .aida64:
                stream = await aidaClient.connect(config: resolved)
            case .libreHardwareMonitor:
                stream = await lhmClient.connect(config: resolved)
            }
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
        resetMetricStats()
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
            pushHistory(&fpsHistory, raw: values["Gph26"] ?? values["Gph24p"] ?? values["SIV25"] ?? values["SIV23"])
            pushHistory(&gpuHistory, raw: values["Gph27"] ?? values["Gph25p"] ?? values["SIV10"])
            let dashboard = values.toDashboard(
                labels: labels,
                fpsHistory: fpsHistory,
                gpuHistory: gpuHistory
            )
            applyDashboard(dashboard)
        case let .dashboardUpdated(raw):
            if let usage = Float(raw.gpuUsage.filter { $0.isNumber || $0 == "." }) {
                if gpuHistory.count >= Self.historySize {
                    gpuHistory.removeFirst()
                }
                gpuHistory.append(max(usage, 0))
            }
            var dashboard = raw
            dashboard.fpsHistory = fpsHistory
            dashboard.gpuHistory = gpuHistory
            applyDashboard(dashboard)
        case let .reconnecting(attempt, message):
            _ = attempt
            uiState.status = .reconnecting
            uiState.errorMessage = message
        case let .disconnected(message):
            uiState.status = .error
            uiState.errorMessage = message
        }
    }

    private func applyDashboard(_ dashboard: DashboardSnapshot) {
        recordDashboardMetrics(dashboard)
        let peaks = absorbBarPeaks(dashboard)
        uiState.status = .connected
        uiState.dashboard = dashboard
        uiState.errorMessage = nil
        uiState.cpuTempStats = cpuTempAccumulator.toStats()
        uiState.gpuTempStats = gpuTempAccumulator.toStats()
        uiState.ramTemp1Stats = ramTemp1Accumulator.toStats()
        uiState.ramTemp2Stats = ramTemp2Accumulator.toStats()
        uiState.fpsStats = fpsAccumulator.toStats()
        uiState.barPeaks = peaks
    }

    private func absorbBarPeaks(_ dashboard: DashboardSnapshot) -> BarScalePeaks {
        let (next, changed) = barPeaks.absorb(dashboard)
        if changed {
            barPeaks = next
            persistPeaksTask?.cancel()
            persistPeaksTask = Task { [weak self] in
                try? await Task.sleep(nanoseconds: 300_000_000)
                guard let self, !Task.isCancelled else { return }
                self.settings.barScalePeaks = next
            }
        }
        return next
    }

    private func recordDashboardMetrics(_ dashboard: DashboardSnapshot) {
        if let v = parseSensorNumber(dashboard.cpuTemp) { cpuTempAccumulator.record(v) }
        if let v = parseSensorNumber(dashboard.gpuTemp) { gpuTempAccumulator.record(v) }
        if let v = parseSensorNumber(dashboard.ramTemp1) { ramTemp1Accumulator.record(v) }
        if let v = parseSensorNumber(dashboard.ramTemp2) { ramTemp2Accumulator.record(v) }
        if let v = parseSensorNumber(dashboard.fps) { fpsAccumulator.record(v) }
    }

    private func pushHistory(_ history: inout [Float], raw: String?) {
        guard let raw, let value = Float(raw) else { return }
        if history.count >= Self.historySize {
            history.removeFirst()
        }
        history.append(Swift.max(value, 0))
    }
}

private final class MetricAccumulator {
    private var min: Float?
    private var max: Float?
    private var sum = 0.0
    private var count = 0

    func record(_ value: Float) {
        min = min.map { Swift.min($0, value) } ?? value
        max = max.map { Swift.max($0, value) } ?? value
        sum += Double(value)
        count += 1
    }

    func reset() {
        min = nil
        max = nil
        sum = 0
        count = 0
    }

    func toStats() -> MetricStats {
        guard count > 0 else { return MetricStats() }
        return MetricStats(min: min, max: max, avg: Float(sum / Double(count)))
    }
}
