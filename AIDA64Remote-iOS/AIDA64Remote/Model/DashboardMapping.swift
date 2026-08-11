import Foundation

extension Dictionary where Key == String, Value == String {
    /// 与 Android `Models.kt` `toDashboard` 对齐的 AIDA64 RemoteSensor 字段映射。
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

        func firstTemp(_ ids: String...) -> String {
            for id in ids {
                let value = temp(id)
                if value != "—" { return value }
            }
            return "—"
        }

        let cpuName = (labels["Label2"] ?? "Intel Core")
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "\u{00A0}", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        // 对齐 Android：固定 C/D/E（SIV12–14）
        let driveSpecs: [(letter: String, usageId: String, barId: String, tempIds: [String])] = [
            ("C", "SIV12", "Bar12p", ["Simple28", "Simple26"]),
            ("D", "SIV13", "Bar13p", ["Simple29", "Simple27"]),
            ("E", "SIV14", "Bar14p", ["Simple30", "Simple28"]),
        ]
        let drives: [DriveSnapshot] = driveSpecs.map { spec in
            let label = labels[spec.usageId]
            let title: String = {
                if let label {
                    let trimmed = label
                        .replacingOccurrences(of: "&nbsp;", with: " ")
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                        .trimmingCharacters(in: CharacterSet(charactersIn: "：:"))
                    if !trimmed.isEmpty { return trimmed }
                }
                return "\(spec.letter)盘使用率"
            }()
            var driveTemp = "—"
            for id in spec.tempIds {
                let value = temp(id)
                if value != "—" {
                    driveTemp = value
                    break
                }
            }
            return DriveSnapshot(
                letter: spec.letter,
                title: title,
                usage: v(spec.usageId),
                bar: bar(spec.barId),
                temp: driveTemp
            )
        }

        let volumeRaw = Float(self["Bar22p"] ?? "") ?? 0

        return DashboardSnapshot(
            cpuName: cpuName.isEmpty ? "Intel Core" : cpuName,
            cpuTemp: v("Simple3"),
            cpuClock: v("SIV4"),
            cpuClockBar: bar("Bar4p"),
            cpuUsage: v("SIV5"),
            cpuUsageBar: bar("Bar5p"),
            gpuTemp: firstTemp("SIV11", "Simple31"),
            vramUsed: v("SIV6"),
            vramUsedBar: bar("Bar6p"),
            vramFree: v("SIV7"),
            vramFreeBar: bar("Bar7p"),
            gpuClock: v("SIV8"),
            gpuClockBar: bar("Bar8p"),
            gpuMemClock: v("SIV9"),
            gpuMemClockBar: bar("Bar9p"),
            gpuUsage: v("SIV10"),
            gpuUsageBar: bar("Bar10p"),
            gpuTempBar: bar("Bar11p"),
            fps: first("SIV25", "SIV23", fallback: "0"),
            fpsHistory: fpsHistory,
            gpuHistory: gpuHistory,
            ramType: first("Simple23", "Simple21"),
            ramUsed: first("Simple20", "Simple18"),
            ramUsedBar: bar("Bar15p"),
            ramFree: first("Simple21", "Simple19"),
            ramFreeBar: bar("Bar16p"),
            ramUsage: first("Simple22", "Simple20"),
            ramUsageBar: bar("Bar17p"),
            ramTemp1: temp("SIV18"),
            ramTemp2: temp("SIV19"),
            drives: drives,
            date: first("Simple33", "Simple31"),
            time: first("Simple34", "Simple32"),
            upload: first("SIV35", "SIV33"),
            download: first("SIV36", "SIV34"),
            volumeBar: Swift.min(Swift.max(volumeRaw, 0), 100) / 100,
            cpuFan: first("SIV37", "SIV35"),
            gpuFan: first("SIV38", "SIV36")
        )
    }
}
