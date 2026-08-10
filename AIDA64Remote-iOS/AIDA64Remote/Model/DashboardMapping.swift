import Foundation

extension Dictionary where Key == String, Value == String {
    func toDashboard(
        labels: [String: String],
        fpsHistory: [Float],
        gpuHistory: [Float]
    ) -> DashboardSnapshot {
        func v(_ id: String, fallback: String = "—") -> String {
            guard let value = self[id], !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                return fallback
            }
            return value
        }

        func bar(_ id: String) -> Float {
            guard let raw = self[id], let value = Float(raw) else { return 0 }
            return Swift.min(Swift.max(value, 0), 100) / 100
        }

        func temp(_ id: String) -> String {
            let raw = v(id, fallback: "")
            if raw.isEmpty { return "—" }
            return raw
                .replacingOccurrences(of: "温度:", with: "")
                .replacingOccurrences(of: "温度：", with: "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
        }

        let gpuTempPrimary = v("Simple29", fallback: "")
        let gpuTemp = gpuTempPrimary.isEmpty ? v("SIV11") : gpuTempPrimary

        let volumeRaw = Float(self["SIV22"] ?? "") ?? Float(self["Bar22p"] ?? "") ?? 0

        return DashboardSnapshot(
            cpuName: labels["Label2"] ?? "Intel Core",
            cpuTemp: v("Simple3"),
            cpuClock: v("SIV4"),
            cpuClockBar: bar("Bar4p"),
            cpuUsage: v("SIV5"),
            cpuUsageBar: bar("Bar5p"),
            gpuTemp: gpuTemp,
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
            fps: v("SIV23", fallback: "0"),
            fpsHistory: fpsHistory,
            gpuHistory: gpuHistory,
            ramType: v("Simple21"),
            ramUsed: v("Simple18"),
            ramUsedBar: bar("Bar15p"),
            ramFree: v("Simple19"),
            ramFreeBar: bar("Bar16p"),
            ramUsage: v("Simple20"),
            ramUsageBar: bar("Bar17p"),
            boardTemp: v("Simple30"),
            driveCUsage: v("SIV12"),
            driveCBar: bar("Bar12p"),
            driveCTemp: temp("Simple26"),
            driveDUsage: v("SIV13"),
            driveDBar: bar("Bar13p"),
            driveDTemp: temp("Simple27"),
            driveEUsage: v("SIV14"),
            driveEBar: bar("Bar14p"),
            driveETemp: temp("Simple28"),
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
