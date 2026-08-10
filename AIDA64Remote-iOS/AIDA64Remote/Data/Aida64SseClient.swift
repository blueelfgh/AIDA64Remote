import Foundation

actor Aida64SseClient {
    private let session: URLSession

    init(session: URLSession? = nil) {
        if let session {
            self.session = session
        } else {
            let config = URLSessionConfiguration.default
            config.timeoutIntervalForRequest = .infinity
            config.timeoutIntervalForResource = .infinity
            config.requestCachePolicy = .reloadIgnoringLocalCacheData
            config.waitsForConnectivity = false
            self.session = URLSession(configuration: config)
        }
    }

    func connect(config: ConnectionConfig) -> AsyncStream<ConnectionEvent> {
        AsyncStream { continuation in
            let task = Task {
                var attempt = 0
                var labels: [String: String] = [:]

                while !Task.isCancelled {
                    do {
                        if attempt == 0 {
                            continuation.yield(.connecting)
                        } else {
                            continuation.yield(.reconnecting(attempt: attempt, message: nil))
                        }

                        if labels.isEmpty {
                            labels = await fetchLabels(config: config)
                        }

                        var request = URLRequest(url: config.sseURL)
                        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
                        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
                        request.timeoutInterval = .infinity

                        let (bytes, response) = try await session.bytes(for: request)
                        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                            throw URLError(.badServerResponse, userInfo: [
                                NSLocalizedDescriptionKey: "HTTP \(http.statusCode)",
                            ])
                        }

                        continuation.yield(.connected)
                        attempt = 0

                        for try await line in bytes.lines {
                            try Task.checkCancellation()
                            guard line.hasPrefix("data:") else { continue }
                            let sensors = SensorParser.parse(line).map { item in
                                var copy = item
                                copy.label = labels[item.id] ?? item.label
                                return copy
                            }
                            if !sensors.isEmpty {
                                continuation.yield(.sensorsUpdated(sensors: sensors, labels: labels))
                            }
                        }

                        throw URLError(.networkConnectionLost, userInfo: [
                            NSLocalizedDescriptionKey: "SSE 连接已关闭",
                        ])
                    } catch is CancellationError {
                        break
                    } catch {
                        attempt += 1
                        let message = error.localizedDescription
                        continuation.yield(.reconnecting(attempt: attempt, message: message))
                        let delayMs = Swift.min(15_000, (1 << Swift.min(attempt - 1, 4)) * 1_000)
                        try? await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)
                    }
                }

                continuation.finish()
            }

            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }

    private func fetchLabels(config: ConnectionConfig) async -> [String: String] {
        do {
            var request = URLRequest(url: config.baseURL)
            request.timeoutInterval = 10
            let (data, response) = try await session.data(for: request)
            if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                return [:]
            }
            let html = String(data: data, encoding: .utf8) ?? ""
            return HtmlLabelParser.parseLabels(html)
        } catch {
            return [:]
        }
    }
}
