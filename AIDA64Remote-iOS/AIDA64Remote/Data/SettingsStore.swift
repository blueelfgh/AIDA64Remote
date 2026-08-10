import Foundation
import SwiftUI

enum ThemeMode: String, CaseIterable, Identifiable, Sendable {
    case dark = "Dark"
    case light = "Light"
    case system = "System"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .dark: return "深色"
        case .light: return "亮色"
        case .system: return "跟随系统"
        }
    }
}

@Observable
final class SettingsStore {
    private enum Keys {
        static let host = "host"
        static let port = "port"
        static let serviceType = "service_type"
        static let keepScreenOn = "keep_screen_on"
        static let themeMode = "theme_mode"
        static let barScalePeaks = "bar_scale_peaks"
    }

    var host: String {
        didSet { UserDefaults.standard.set(host, forKey: Keys.host) }
    }

    var port: Int {
        didSet { UserDefaults.standard.set(port, forKey: Keys.port) }
    }

    var serviceType: ServiceType {
        didSet { UserDefaults.standard.set(serviceType.rawValue, forKey: Keys.serviceType) }
    }

    var keepScreenOn: Bool {
        didSet { UserDefaults.standard.set(keepScreenOn, forKey: Keys.keepScreenOn) }
    }

    var themeMode: ThemeMode {
        didSet { UserDefaults.standard.set(themeMode.rawValue, forKey: Keys.themeMode) }
    }

    var barScalePeaks: BarScalePeaks {
        didSet { persistBarScalePeaks() }
    }

    var connection: ConnectionConfig {
        ConnectionConfig(host: host, port: port, serviceType: serviceType)
    }

    init(defaults: UserDefaults = .standard) {
        self.host = defaults.string(forKey: Keys.host) ?? ConnectionConfig.defaultHost
        let storedPort = defaults.object(forKey: Keys.port) as? Int
        self.port = storedPort ?? ConnectionConfig.defaultPort
        if let raw = defaults.string(forKey: Keys.serviceType),
           let type = ServiceType(rawValue: raw) {
            self.serviceType = type
        } else {
            self.serviceType = .aida64
        }
        self.keepScreenOn = defaults.bool(forKey: Keys.keepScreenOn)
        if let raw = defaults.string(forKey: Keys.themeMode),
           let mode = ThemeMode(rawValue: raw) {
            self.themeMode = mode
        } else {
            self.themeMode = .dark
        }
        if let data = defaults.data(forKey: Keys.barScalePeaks),
           let peaks = try? JSONDecoder().decode(BarScalePeaks.self, from: data) {
            self.barScalePeaks = peaks
        } else {
            self.barScalePeaks = BarScalePeaks()
        }
    }

    func saveConnection(_ config: ConnectionConfig) {
        host = config.host
        port = config.port
        serviceType = config.serviceType
    }

    /// 切换服务类型；若当前端口仍是某一默认端口，则同步改成新类型默认端口。
    func setServiceType(_ type: ServiceType) {
        let shouldResetPort =
            port == ServiceType.aida64.defaultPort
            || port == ServiceType.libreHardwareMonitor.defaultPort
        serviceType = type
        if shouldResetPort {
            port = type.defaultPort
        }
    }

    func clearBarScalePeaks() {
        barScalePeaks = BarScalePeaks()
    }

    private func persistBarScalePeaks() {
        if let data = try? JSONEncoder().encode(barScalePeaks) {
            UserDefaults.standard.set(data, forKey: Keys.barScalePeaks)
        }
    }
}
