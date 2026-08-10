import Foundation

struct SensorItem: Equatable, Sendable {
    let id: String
    let value: String
    var label: String?

    var displayName: String {
        if let label, !label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return label
        }
        return id
    }
}

enum ConnectionStatus: Equatable, Sendable {
    case idle
    case connecting
    case connected
    case reconnecting
    case error
}

enum ConnectionEvent: Sendable {
    case connecting
    case connected
    case sensorsUpdated(sensors: [SensorItem], labels: [String: String])
    case reconnecting(attempt: Int, message: String?)
    case disconnected(message: String?)
}

struct DriveSnapshot: Equatable, Identifiable, Sendable {
    var id: String { letter }
    let letter: String
    let title: String
    let usage: String
    let bar: Float
    let temp: String
}

struct DashboardSnapshot: Equatable, Sendable {
    var cpuName: String = "—"
    var cpuTemp: String = "—"
    var cpuClock: String = "—"
    var cpuClockBar: Float = 0
    var cpuUsage: String = "—"
    var cpuUsageBar: Float = 0
    var gpuTemp: String = "—"
    var vramUsed: String = "—"
    var vramUsedBar: Float = 0
    var vramFree: String = "—"
    var vramFreeBar: Float = 0
    var gpuClock: String = "—"
    var gpuClockBar: Float = 0
    var gpuMemClock: String = "—"
    var gpuMemClockBar: Float = 0
    var gpuUsage: String = "—"
    var gpuUsageBar: Float = 0
    var gpuTempBar: Float = 0
    var fps: String = "0"
    var fpsHistory: [Float] = []
    var gpuHistory: [Float] = []
    var ramType: String = "—"
    var ramUsed: String = "—"
    var ramUsedBar: Float = 0
    var ramFree: String = "—"
    var ramFreeBar: Float = 0
    var ramUsage: String = "—"
    var ramUsageBar: Float = 0
    var boardTemp: String = "—"
    var drives: [DriveSnapshot] = []
    var date: String = "—"
    var time: String = "—"
    var upload: String = "—"
    var download: String = "—"
    var volumeBar: Float = 0
    var cpuFan: String = "—"
    var gpuFan: String = "—"
}

struct MonitorUiState: Equatable, Sendable {
    var status: ConnectionStatus = .idle
    var dashboard: DashboardSnapshot = DashboardSnapshot()
    var errorMessage: String?
    var config: ConnectionConfig = ConnectionConfig()
    var keepScreenOn: Bool = false
    var isFullscreen: Bool = false
}
