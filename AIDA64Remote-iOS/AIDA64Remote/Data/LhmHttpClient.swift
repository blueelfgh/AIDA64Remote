import Foundation

actor LhmHttpClient {
    private let pollIntervalNs: UInt64
    private let session: URLSession

    init(pollIntervalMs: UInt64 = 1000) {
        self.pollIntervalNs = pollIntervalMs * 1_000_000
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 8
        config.timeoutIntervalForResource = 12
        config.waitsForConnectivity = false
        self.session = URLSession(configuration: config)
    }

    func connect(config: ConnectionConfig) -> AsyncStream<ConnectionEvent> {
        AsyncStream { continuation in
            let task = Task {
                var attempt = 0
                continuation.yield(.connecting)

                while !Task.isCancelled {
                    do {
                        if attempt > 0 {
                            continuation.yield(.reconnecting(attempt: attempt, message: nil))
                        }
                        let snapshot = try await fetchSnapshot(config: config)
                        continuation.yield(.connected)
                        attempt = 0
                        continuation.yield(.dashboardUpdated(snapshot))

                        while !Task.isCancelled {
                            try await Task.sleep(nanoseconds: pollIntervalNs)
                            let next = try await fetchSnapshot(config: config)
                            continuation.yield(.dashboardUpdated(next))
                        }
                    } catch is CancellationError {
                        break
                    } catch {
                        attempt += 1
                        let message = (error as NSError).localizedDescription
                        continuation.yield(.reconnecting(attempt: attempt, message: message))
                        let delaySec = min(15, (1 << min(attempt - 1, 4)) * 1)
                        try? await Task.sleep(nanoseconds: UInt64(delaySec) * 1_000_000_000)
                    }
                }
                continuation.finish()
            }

            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }

    private func fetchSnapshot(config: ConnectionConfig) async throws -> DashboardSnapshot {
        var request = URLRequest(url: config.lhmSnapshotURL)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        guard !data.isEmpty else {
            throw URLError(.zeroByteResource)
        }
        return try Self.parseSnapshot(data: data)
    }

    static func parseSnapshot(data: Data) throws -> DashboardSnapshot {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw URLError(.cannotParseResponse)
        }
        return parseSnapshot(json: json)
    }

    static func parseSnapshot(json: [String: Any]) -> DashboardSnapshot {
        func str(_ key: String) -> String {
            let value = (json[key] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return value.isEmpty ? "—" : value
        }
        func floatVal(_ key: String) -> Float {
            let raw: Double
            if let n = json[key] as? Double {
                raw = n
            } else if let n = json[key] as? Int {
                raw = Double(n)
            } else if let s = json[key] as? String, let n = Double(s) {
                raw = n
            } else {
                raw = 0
            }
            return Float(Swift.min(Swift.max(raw, 0), 1))
        }

        var drives: [DriveSnapshot] = []
        let driveSpecs: [(String, String, String, String)] = [
            ("C", "driveCUsage", "driveCBar", "driveCTemp"),
            ("D", "driveDUsage", "driveDBar", "driveDTemp"),
            ("E", "driveEUsage", "driveEBar", "driveETemp"),
        ]
        for (letter, usageKey, barKey, tempKey) in driveSpecs {
            let usage = str(usageKey)
            let temp = str(tempKey)
            guard usage != "—" || temp != "—" else { continue }
            drives.append(
                DriveSnapshot(
                    letter: letter,
                    title: "\(letter)盘使用率",
                    usage: usage == "—" ? "0" : usage,
                    bar: floatVal(barKey),
                    temp: temp
                )
            )
        }

        let fpsRaw = (json["fps"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return DashboardSnapshot(
            cpuName: str("cpuName"),
            cpuTemp: str("cpuTemp"),
            cpuClock: str("cpuClock"),
            cpuClockBar: floatVal("cpuClockBar"),
            cpuUsage: str("cpuUsage"),
            cpuUsageBar: floatVal("cpuUsageBar"),
            gpuTemp: str("gpuTemp"),
            vramUsed: str("vramUsed"),
            vramUsedBar: floatVal("vramUsedBar"),
            vramFree: str("vramFree"),
            vramFreeBar: floatVal("vramFreeBar"),
            gpuClock: str("gpuClock"),
            gpuClockBar: floatVal("gpuClockBar"),
            gpuMemClock: str("gpuMemClock"),
            gpuMemClockBar: floatVal("gpuMemClockBar"),
            gpuUsage: str("gpuUsage"),
            gpuUsageBar: floatVal("gpuUsageBar"),
            gpuTempBar: floatVal("gpuTempBar"),
            fps: fpsRaw.isEmpty ? "0" : fpsRaw,
            ramType: str("ramType"),
            ramUsed: str("ramUsed"),
            ramUsedBar: floatVal("ramUsedBar"),
            ramFree: str("ramFree"),
            ramFreeBar: floatVal("ramFreeBar"),
            ramUsage: str("ramUsage"),
            ramUsageBar: floatVal("ramUsageBar"),
            ramTemp1: str("ramTemp1"),
            ramTemp2: str("ramTemp2"),
            drives: drives,
            date: str("date"),
            time: str("time"),
            upload: str("upload"),
            download: str("download"),
            volumeBar: floatVal("volumeBar"),
            cpuFan: str("cpuFan"),
            gpuFan: str("gpuFan")
        )
    }
}
