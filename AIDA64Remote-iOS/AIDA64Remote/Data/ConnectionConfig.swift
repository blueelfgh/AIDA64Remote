import Foundation

enum ServiceType: String, CaseIterable, Identifiable, Codable, Sendable {
    case aida64 = "Aida64"
    case libreHardwareMonitor = "LibreHardwareMonitor"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .aida64: return "AIDA64"
        case .libreHardwareMonitor: return "LibreHardwareMonitor"
        }
    }

    var defaultPort: Int {
        switch self {
        case .aida64: return ConnectionConfig.defaultAidaPort
        case .libreHardwareMonitor: return ConnectionConfig.defaultLhmPort
        }
    }
}

struct ConnectionConfig: Equatable, Sendable {
    var host: String
    var port: Int
    var serviceType: ServiceType

    init(
        host: String = Self.defaultHost,
        port: Int = Self.defaultAidaPort,
        serviceType: ServiceType = .aida64
    ) {
        self.host = host
        self.port = port
        self.serviceType = serviceType
    }

    var baseURL: URL {
        URL(string: "http://\(host):\(port)")!
    }

    var sseURL: URL {
        baseURL.appendingPathComponent("sse")
    }

    var lhmSnapshotURL: URL {
        baseURL.appendingPathComponent("api/snapshot")
    }

    static let defaultHost = "192.168.50.23"
    static let defaultAidaPort = 35080
    static let defaultLhmPort = 18080
    static let defaultPort = defaultAidaPort
}
