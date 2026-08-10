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
        static let keepScreenOn = "keep_screen_on"
        static let themeMode = "theme_mode"
    }

    var host: String {
        didSet { UserDefaults.standard.set(host, forKey: Keys.host) }
    }

    var port: Int {
        didSet { UserDefaults.standard.set(port, forKey: Keys.port) }
    }

    var keepScreenOn: Bool {
        didSet { UserDefaults.standard.set(keepScreenOn, forKey: Keys.keepScreenOn) }
    }

    var themeMode: ThemeMode {
        didSet { UserDefaults.standard.set(themeMode.rawValue, forKey: Keys.themeMode) }
    }

    var connection: ConnectionConfig {
        ConnectionConfig(host: host, port: port)
    }

    init(defaults: UserDefaults = .standard) {
        self.host = defaults.string(forKey: Keys.host) ?? ConnectionConfig.defaultHost
        let storedPort = defaults.object(forKey: Keys.port) as? Int
        self.port = storedPort ?? ConnectionConfig.defaultPort
        self.keepScreenOn = defaults.bool(forKey: Keys.keepScreenOn)
        if let raw = defaults.string(forKey: Keys.themeMode),
           let mode = ThemeMode(rawValue: raw) {
            self.themeMode = mode
        } else {
            self.themeMode = .dark
        }
    }

    func saveConnection(_ config: ConnectionConfig) {
        host = config.host
        port = config.port
    }
}
