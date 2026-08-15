import Foundation

/// 各进度条指标曾出现过的历史最大值（持久化，可在设置中清除）。
/// 使用率类指标（CPU/GPU/内存）固定以 100% 为上限，不写入本结构。
struct BarScalePeaks: Equatable, Codable, Sendable {
    var cpuClock: Float?
    var cpuUsage: Float? // 保留字段以兼容旧持久化数据，不再更新
    var vramUsed: Float?
    var vramFree: Float?
    var gpuClock: Float?
    var gpuMemClock: Float?
    var gpuUsage: Float? // 保留字段以兼容旧持久化数据，不再更新
    var gpuTemp: Float?
    var ramUsed: Float?
    var ramFree: Float?
    var ramUsage: Float? // 保留字段以兼容旧持久化数据，不再更新
    var ramTemp1: Float?
    var ramTemp2: Float?
    var driveC: Float?
    var driveD: Float?
    var driveE: Float?
    /// 其它盘符温度峰值（C/D/E 以外）
    var driveTemps: [String: Float] = [:]
    var volume: Float?

    func peak(forDrive letter: String) -> Float? {
        switch letter.uppercased() {
        case "C": return driveC
        case "D": return driveD
        case "E": return driveE
        default: return driveTemps[letter.uppercased()]
        }
    }

    /// 用当前快照抬升峰值；返回更新后的峰值与是否有变化。
    func absorb(_ dashboard: DashboardSnapshot) -> (BarScalePeaks, Bool) {
        var changed = false
        func bump(_ old: Float?, _ raw: String) -> Float? {
            guard let value = parseSensorNumber(raw) else { return old }
            if old == nil || value > old! {
                changed = true
                return value
            }
            return old
        }

        var next = self
        next.cpuClock = bump(cpuClock, dashboard.cpuClock)
        next.vramUsed = bump(vramUsed, dashboard.vramUsed)
        next.vramFree = bump(vramFree, dashboard.vramFree)
        next.gpuClock = bump(gpuClock, dashboard.gpuClock)
        next.gpuMemClock = bump(gpuMemClock, dashboard.gpuMemClock)
        next.gpuTemp = bump(gpuTemp, dashboard.gpuTemp)
        next.ramTemp1 = bump(ramTemp1, dashboard.ramTemp1)
        next.ramTemp2 = bump(ramTemp2, dashboard.ramTemp2)
        next.volume = bump(volume, String(dashboard.volumeBar * 100))

        for drive in dashboard.drives {
            let letter = drive.letter.uppercased()
            switch letter {
            case "C":
                next.driveC = bump(next.driveC, drive.temp)
            case "D":
                next.driveD = bump(next.driveD, drive.temp)
            case "E":
                next.driveE = bump(next.driveE, drive.temp)
            default:
                next.driveTemps[letter] = bump(next.driveTemps[letter], drive.temp)
            }
        }

        return (next, changed)
    }
}

func parseSensorNumber(_ raw: String) -> Float? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.isEmpty || trimmed == "—" { return nil }
    var number = ""
    var seenDot = false
    for ch in trimmed {
        if ch.isNumber {
            number.append(ch)
        } else if ch == "." && !seenDot {
            number.append(ch)
            seenDot = true
        } else if ch == "-" && number.isEmpty {
            number.append(ch)
        }
    }
    return Float(number)
}

func formatScaleMax(_ max: Float) -> String {
    let rounded: Float
    if max >= 100 {
        rounded = Float(Int(max.rounded()))
    } else if max >= 10 {
        rounded = Float((max * 10).rounded()) / 10
    } else {
        rounded = Float((max * 100).rounded()) / 100
    }
    if rounded == Float(Int(rounded)) {
        return String(Int(rounded))
    }
    return String(rounded)
}
