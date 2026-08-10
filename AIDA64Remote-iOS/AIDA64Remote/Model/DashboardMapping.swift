import Foundation

extension Dictionary where Key == String, Value == String {
    /// 按当前 AIDA64 RemoteSensor 页面传感器 ID 映射到仪表盘。
    /// 对照 http://host:35080/ 标签：SIV3 频率、SIV4 使用率、SIV5–10 GPU、SIV11+ 磁盘等。
    func toDashboard(
        labels: [String: String],
        fpsHistory: [Float],
        gpuHistory: [Float]
    ) -> DashboardSnapshot {
        func v(_ id: String, fallback: String = "—") -> String {
            guard let value = self[id]?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !value.isEmpty
            else { return fallback }
            return value
        }

        func first(_ ids: String..., fallback: String = "—") -> String {
            for id in ids {
                let value = v(id, fallback: "")
                if !value.isEmpty { return value }
            }
            return fallback
        }

        func bar(_ id: String) -> Float {
            guard let raw = self[id], let value = Float(raw) else { return 0 }
            return Swift.min(Swift.max(value, 0), 100) / 100
        }

        func temp(_ id: String) -> String {
            let raw = v(id, fallback: "")
            if raw.isEmpty { return "—" }
            var cleaned = raw
                .replacingOccurrences(of: "温度:", with: "")
                .replacingOccurrences(of: "温度：", with: "")
                .replacingOccurrences(of: "&nbsp;", with: " ")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if cleaned.isEmpty { return "—" }
            if !cleaned.contains("°") { cleaned += "°" }
            return cleaned
        }

        func firstTemp(_ ids: [String]) -> String {
            for id in ids {
                let value = temp(id)
                if value != "—" { return value }
            }
            return "—"
        }

        func firstTemp(_ ids: String...) -> String {
            firstTemp(Array(ids))
        }

        let cpuName = (labels["Label2"] ?? "Intel Core")
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "\u{00A0}", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        let volumeRaw = Float(self["Bar21p"] ?? "")
            ?? Float(self["Bar22p"] ?? "")
            ?? Float(self["SIV21"] ?? "")
            ?? 0

        // SIV11–SIV20 对应 C–L；仅当使用率有值时才展示（避免扫到 SIV22 FPS / SIV35 风扇等）
        let driveLetters = Array("CDEFGHIJKL")
        let driveTempIds: [[String]] = [
            ["Simple26", "Simple25"],
            ["Simple27"],
            ["Simple28"],
            ["Simple35"],
            ["Simple36"],
            ["Simple37"],
            ["Simple38"],
            ["Simple39"],
            ["Simple40"],
            ["Simple41"],
        ]
        var drives: [DriveSnapshot] = []
        for (index, letter) in driveLetters.enumerated() {
            let n = 11 + index
            let usageId = "SIV\(n)"
            let usage = v(usageId, fallback: "")
            guard !usage.isEmpty else { continue }
            let temps = index < driveTempIds.count ? driveTempIds[index] : ["Simple\(25 + index)"]
            let label = labels[usageId] ?? labels["SI\(n)"]
            let title: String = {
                if let label {
                    let trimmed = label
                        .replacingOccurrences(of: "&nbsp;", with: " ")
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                        .trimmingCharacters(in: CharacterSet(charactersIn: "：:"))
                    if !trimmed.isEmpty { return trimmed }
                }
                return "\(letter)盘使用率"
            }()
            drives.append(
                DriveSnapshot(
                    letter: String(letter),
                    title: title,
                    usage: usage,
                    bar: bar("Bar\(n)p"),
                    temp: firstTemp(temps)
                )
            )
        }

        return DashboardSnapshot(
            cpuName: cpuName.isEmpty ? "Intel Core" : cpuName,
            cpuTemp: firstTemp("Simple3", "Simple24"),
            cpuClock: v("SIV3"),
            cpuClockBar: bar("Bar3p"),
            cpuUsage: v("SIV4"),
            cpuUsageBar: bar("Bar4p"),
            gpuTemp: firstTemp("Simple29", "SIV10"),
            vramUsed: v("SIV5"),
            vramUsedBar: bar("Bar5p"),
            vramFree: v("SIV6"),
            vramFreeBar: bar("Bar6p"),
            gpuClock: v("SIV7"),
            gpuClockBar: bar("Bar7p"),
            gpuMemClock: v("SIV8"),
            gpuMemClockBar: bar("Bar8p"),
            gpuUsage: v("SIV9"),
            gpuUsageBar: bar("Bar9p"),
            gpuTempBar: bar("Bar10p"),
            fps: first("SIV22", "Gph23p", fallback: "0"),
            fpsHistory: fpsHistory,
            gpuHistory: gpuHistory,
            ramType: v("Simple20"),
            ramUsed: v("Simple18"),
            ramUsedBar: bar("Bar15p"),
            ramFree: v("Simple17"),
            ramFreeBar: bar("Bar14p"),
            ramUsage: v("Simple19"),
            ramUsageBar: bar("Bar16p"),
            boardTemp: firstTemp("Simple30"),
            drives: drives,
            date: v("Simple31"),
            time: v("Simple32"),
            upload: v("SIV33"),
            download: v("SIV34"),
            volumeBar: Swift.min(Swift.max(volumeRaw, 0), 100) / 100,
            cpuFan: v("SIV35"),
            gpuFan: v("SIV36")
        )
    }
}
