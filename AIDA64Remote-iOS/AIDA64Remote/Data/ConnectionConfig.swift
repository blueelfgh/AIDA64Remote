import Foundation

struct ConnectionConfig: Equatable, Sendable {
    var host: String
    var port: Int

    init(host: String = Self.defaultHost, port: Int = Self.defaultPort) {
        self.host = host
        self.port = port
    }

    var baseURL: URL {
        URL(string: "http://\(host):\(port)")!
    }

    var sseURL: URL {
        baseURL.appendingPathComponent("sse")
    }

    static let defaultHost = "192.168.50.23"
    static let defaultPort = 35080
}
