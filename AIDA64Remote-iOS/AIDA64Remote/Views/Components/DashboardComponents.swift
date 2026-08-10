import SwiftUI

enum PanelDensity {
    case compact
    case regular

    var panelPadding: CGFloat { self == .compact ? 8 : 10 }
    var metricSpacing: CGFloat { self == .compact ? 3 : 6 }
    var labelSize: CGFloat { self == .compact ? 11 : 13 }
    var valueSize: CGFloat { self == .compact ? 12 : 14 }
    var tempSize: CGFloat { self == .compact ? 26 : 34 }
    var titleSize: CGFloat { self == .compact ? 13 : 16 }
}

struct HexBackground: View {
    @Environment(\.dashColors) private var colors

    var body: some View {
        Canvas { context, size in
            let hexSize: CGFloat = 28
            let h = hexSize * 1.73205
            let w = hexSize * 2
            let rows = Int(size.height / (h * 0.75)) + 2
            let cols = Int(size.width / (w * 0.75)) + 2
            for row in 0..<rows {
                for col in 0..<cols {
                    let x = CGFloat(col) * w * 0.75 + (row % 2 == 0 ? 0 : w * 0.375)
                    let y = CGFloat(row) * h * 0.75
                    let radius = hexSize * 0.92
                    var path = Path()
                    for i in 0..<6 {
                        let angle = Angle.degrees(60 * Double(i) - 30).radians
                        let point = CGPoint(
                            x: x + radius * CGFloat(cos(angle)),
                            y: y + radius * CGFloat(sin(angle))
                        )
                        if i == 0 {
                            path.move(to: point)
                        } else {
                            path.addLine(to: point)
                        }
                    }
                    path.closeSubpath()
                    let edge = row == 0 || col == 0 || row == rows - 1 || col == cols - 1
                    context.stroke(path, with: .color(edge ? colors.hexGlow : colors.hex), lineWidth: 1.2)
                }
            }
        }
        .background(colors.bg)
        .ignoresSafeArea()
    }
}

struct DashPanel<Content: View>: View {
    var density: PanelDensity = .regular
    @Environment(\.dashColors) private var colors
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(density.panelPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(colors.panel)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(colors.border, lineWidth: 1)
            )
            .contentShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

struct ThinProgressBar: View {
    let progress: Float
    @Environment(\.dashColors) private var colors

    var body: some View {
        Capsule(style: .continuous)
            .fill(colors.track)
            .frame(height: 7)
            .overlay(alignment: .leading) {
                GeometryReader { geo in
                    Capsule(style: .continuous)
                        .fill(colors.fill)
                        .frame(width: geo.size.width * CGFloat(Swift.min(Swift.max(progress, 0), 1)))
                }
            }
    }
}

struct MetricBarRow: View {
    let label: String
    let value: String
    var unit: String = ""
    let progress: Float
    var peakMax: Float? = nil
    var density: PanelDensity = .regular
    @Environment(\.dashColors) private var colors

    private var resolvedProgress: Float {
        if let peakMax, peakMax > 0, let current = parseSensorNumber(value) {
            return min(max(current / peakMax, 0), 1)
        }
        return progress
    }

    private var valueText: String {
        let maxLabel = peakMax.map(formatScaleMax)
        switch (maxLabel, unit.isEmpty) {
        case (nil, true): return value
        case (nil, false): return "\(value) \(unit)"
        case (let max?, true): return "\(value) / \(max)"
        case (let max?, false): return "\(value) / \(max) \(unit)"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 6) {
                Text(label)
                    .font(.system(size: density.labelSize, weight: .semibold))
                    .foregroundStyle(colors.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                Spacer(minLength: 4)
                Text(valueText)
                    .font(.system(size: density.valueSize, weight: .bold))
                    .foregroundStyle(colors.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
            ThinProgressBar(progress: resolvedProgress)
        }
    }
}

struct StatsCaption: View {
    let stats: MetricStats
    var decimals: Int = 0
    @Environment(\.dashColors) private var colors

    var body: some View {
        Text("高 \(formatStat(stats.max)) 低 \(formatStat(stats.min)) 均 \(formatStat(stats.avg))")
            .font(.system(size: 10, weight: .medium))
            .foregroundStyle(colors.muted)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }

    private func formatStat(_ value: Float?) -> String {
        guard let value else { return "—" }
        if decimals <= 0 {
            return String(Int(value.rounded()))
        }
        return String(format: "%.\(decimals)f", value)
    }
}

struct Sparkline: View {
    let values: [Float]
    var maxY: Float = 100
    var lineColor: Color?
    @Environment(\.dashColors) private var colors

    var body: some View {
        Canvas { context, size in
            guard !values.isEmpty else { return }
            let maxVal = Swift.max(Swift.max(maxY, values.max() ?? maxY), 1)
            let stepX = values.count <= 1 ? size.width : size.width / CGFloat(values.count - 1)
            var path = Path()
            for (index, value) in values.enumerated() {
                let x = CGFloat(index) * stepX
                let y = size.height - CGFloat(value / maxVal) * size.height
                if index == 0 {
                    path.move(to: CGPoint(x: x, y: y))
                } else {
                    path.addLine(to: CGPoint(x: x, y: y))
                }
            }
            context.stroke(
                path,
                with: .color(lineColor ?? colors.fill),
                style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round)
            )
        }
    }
}

struct TempValue: View {
    let temp: String
    var tempSize: CGFloat = 32
    @Environment(\.dashColors) private var colors

    private var color: Color {
        guard let value = parseSensorNumber(temp) else { return colors.text }
        if value > 85 { return colors.accentRed }
        if value > 75 { return colors.accentYellow }
        return colors.text
    }

    var body: some View {
        Text(temp)
            .font(.system(size: tempSize, weight: .bold))
            .foregroundStyle(color)
            .lineLimit(1)
            .minimumScaleFactor(0.5)
    }
}

struct CpuPanel: View {
    let data: DashboardSnapshot
    var cpuTempStats: MetricStats = MetricStats()
    var barPeaks: BarScalePeaks = BarScalePeaks()
    var showKeepScreenOn: Bool = false
    var density: PanelDensity = .regular
    @Environment(\.dashColors) private var colors

    var body: some View {
        DashPanel(density: density) {
            ZStack(alignment: .topLeading) {
                if showKeepScreenOn {
                    Text("屏幕保持常亮")
                        .font(.system(size: density == .compact ? 11 : 13, weight: .bold))
                        .foregroundStyle(colors.badgeText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(colors.badgeBg)
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(colors.badgeText, lineWidth: 1))
                        .zIndex(1)
                }

                HStack(alignment: .center, spacing: 8) {
                    VStack(spacing: 4) {
                        TempValue(temp: data.cpuTemp, tempSize: density.tempSize)
                        StatsCaption(stats: cpuTempStats)
                    }
                    .frame(minWidth: density == .compact ? 52 : 64)

                    VStack(alignment: .leading, spacing: density.metricSpacing) {
                        Text(data.cpuName)
                            .font(.system(size: density.titleSize, weight: .bold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        MetricBarRow(
                            label: "CPU 核心频率",
                            value: data.cpuClock,
                            unit: "MHz",
                            progress: data.cpuClockBar,
                            peakMax: barPeaks.cpuClock,
                            density: density
                        )
                        MetricBarRow(
                            label: "CPU 使用率",
                            value: data.cpuUsage,
                            unit: "%",
                            progress: data.cpuUsageBar,
                            peakMax: barPeaks.cpuUsage,
                            density: density
                        )
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.top, showKeepScreenOn ? 20 : 0)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
        }
    }
}

struct GpuPanel: View {
    let data: DashboardSnapshot
    var gpuTempStats: MetricStats = MetricStats()
    var barPeaks: BarScalePeaks = BarScalePeaks()
    var density: PanelDensity = .compact
    /// 横屏等矮面板强制双列指标，避免单列撑破行高。
    var forceTwoColumnMetrics: Bool = false

    private var gpuTempDigits: String {
        String(data.gpuTemp.filter { $0.isNumber || $0 == "." })
    }

    private var rows: [(String, String, String, Float, Float?)] {
        let short = density == .compact
        return [
            (short ? "已用显存" : "已用显存", data.vramUsed, "MB", data.vramUsedBar, barPeaks.vramUsed),
            (short ? "可用显存" : "可用显存", data.vramFree, "MB", data.vramFreeBar, barPeaks.vramFree),
            (short ? "核心频率" : "GPU 核心频率", data.gpuClock, "MHz", data.gpuClockBar, barPeaks.gpuClock),
            (short ? "显存频率" : "GPU 显存频率", data.gpuMemClock, "MHz", data.gpuMemClockBar, barPeaks.gpuMemClock),
            (short ? "使用率" : "GPU 使用率", data.gpuUsage, "%", data.gpuUsageBar, barPeaks.gpuUsage),
            (short ? "温度" : "GPU 温度", gpuTempDigits, "°C", data.gpuTempBar, barPeaks.gpuTemp),
        ]
    }

    var body: some View {
        DashPanel(density: density) {
            GeometryReader { geo in
                let narrow = geo.size.width < 280
                let columns = forceTwoColumnMetrics || geo.size.height < 160 || !narrow ? 2 : 1
                let tempSize = min(density.tempSize, max(18, geo.size.height * 0.28))

                Group {
                    if narrow && columns == 1 {
                        VStack(alignment: .leading, spacing: density.metricSpacing) {
                            VStack(spacing: 2) {
                                TempValue(temp: data.gpuTemp, tempSize: tempSize)
                                StatsCaption(stats: gpuTempStats)
                            }
                            metricsGrid(rows: rows, columns: 1)
                        }
                    } else {
                        HStack(alignment: .center, spacing: 6) {
                            VStack(spacing: 2) {
                                TempValue(temp: data.gpuTemp, tempSize: tempSize)
                                StatsCaption(stats: gpuTempStats)
                            }
                            .frame(width: min(64, geo.size.width * 0.18))
                            metricsGrid(rows: rows, columns: columns)
                        }
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height, alignment: .center)
                .clipped()
            }
        }
    }

    @ViewBuilder
    private func metricsGrid(rows: [(String, String, String, Float, Float?)], columns: Int) -> some View {
        if columns == 2 {
            let mid = (rows.count + 1) / 2
            HStack(alignment: .top, spacing: 8) {
                metricColumn(Array(rows.prefix(mid)))
                metricColumn(Array(rows.suffix(rows.count - mid)))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            metricColumn(rows)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func metricColumn(_ rows: [(String, String, String, Float, Float?)]) -> some View {
        VStack(spacing: density.metricSpacing) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                MetricBarRow(
                    label: row.0,
                    value: row.1,
                    unit: row.2,
                    progress: row.3,
                    peakMax: row.4,
                    density: density
                )
            }
        }
    }
}

struct FpsPanel: View {
    let data: DashboardSnapshot
    var fpsStats: MetricStats = MetricStats()
    @Environment(\.dashColors) private var colors

    var body: some View {
        DashPanel(density: .compact) {
            ZStack {
                Text("120")
                    .font(.system(size: 11))
                    .foregroundStyle(colors.muted)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                Text("0")
                    .font(.system(size: 11))
                    .foregroundStyle(colors.muted)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(data.fps) FPS")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(colors.text)
                    StatsCaption(stats: fpsStats)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                Sparkline(values: data.fpsHistory, maxY: 120, lineColor: colors.sparkline)
                    .padding(.leading, 22)
                    .padding(.top, 18)
                    .padding(.trailing, 8)
                    .padding(.bottom, 8)
            }
        }
    }
}

struct RamPanel: View {
    let data: DashboardSnapshot
    var ramTemp1Stats: MetricStats = MetricStats()
    var ramTemp2Stats: MetricStats = MetricStats()
    var density: PanelDensity = .regular
    @Environment(\.dashColors) private var colors

    private var ramTotal: Float? {
        guard let used = parseSensorNumber(data.ramUsed),
              let free = parseSensorNumber(data.ramFree)
        else { return nil }
        return used + free
    }

    private var showTemp2: Bool {
        let t = data.ramTemp2.trimmingCharacters(in: .whitespacesAndNewlines)
        return !t.isEmpty && t != "—"
    }

    var body: some View {
        DashPanel(density: density) {
            HStack(alignment: .center, spacing: 6) {
                VStack(alignment: .leading, spacing: density.metricSpacing) {
                    HStack(spacing: 6) {
                        Image(systemName: "memorychip")
                            .foregroundStyle(colors.text)
                            .font(.system(size: density == .compact ? 14 : 20))
                        Text(data.ramType)
                            .font(.system(size: density.titleSize, weight: .bold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    MetricBarRow(
                        label: "已用内存",
                        value: data.ramUsed,
                        progress: data.ramUsedBar,
                        peakMax: ramTotal,
                        density: density
                    )
                    MetricBarRow(
                        label: "可用内存",
                        value: data.ramFree,
                        progress: data.ramFreeBar,
                        peakMax: ramTotal,
                        density: density
                    )
                    MetricBarRow(
                        label: "使用率",
                        value: String(data.ramUsage.filter { $0.isNumber || $0 == "." }),
                        unit: "%",
                        progress: data.ramUsageBar,
                        peakMax: 100,
                        density: density
                    )
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(spacing: density == .compact ? 4 : 8) {
                    tempBlock(title: showTemp2 ? "内存温度1" : "主板温度", temp: data.ramTemp1, stats: ramTemp1Stats)
                    if showTemp2 {
                        tempBlock(title: "内存温度2", temp: data.ramTemp2, stats: ramTemp2Stats)
                    }
                }
                .frame(width: density == .compact ? (showTemp2 ? 68 : 64) : 88)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
        }
    }

    @ViewBuilder
    private func tempBlock(title: String, temp: String, stats: MetricStats) -> some View {
        VStack(spacing: 1) {
            Text(title)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(colors.muted)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            TempValue(temp: temp, tempSize: density == .compact ? 20 : 26)
            StatsCaption(stats: stats)
        }
    }
}

struct StoragePanel: View {
    let data: DashboardSnapshot
    var barPeaks: BarScalePeaks = BarScalePeaks()
    var density: PanelDensity = .compact
    @Environment(\.dashColors) private var colors

    var body: some View {
        DashPanel(density: density) {
            if data.drives.isEmpty {
                Text("暂无磁盘数据")
                    .font(.system(size: density.labelSize))
                    .foregroundStyle(colors.muted)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: density.metricSpacing + 2) {
                    ForEach(data.drives) { drive in
                        DriveRow(
                            name: drive.title,
                            usage: drive.usage,
                            bar: drive.bar,
                            temp: drive.temp,
                            peakMax: barPeaks.peak(forDrive: drive.letter),
                            density: density
                        )
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
        }
    }

    private struct DriveRow: View {
        let name: String
        let usage: String
        let bar: Float
        let temp: String
        var peakMax: Float?
        var density: PanelDensity
        @Environment(\.dashColors) private var colors

        private var tempText: String {
            if let peakMax {
                return "温度: \(temp) / \(formatScaleMax(peakMax))"
            }
            return "温度: \(temp)"
        }

        var body: some View {
            HStack(spacing: 6) {
                Image(systemName: "externaldrive")
                    .font(.system(size: density == .compact ? 14 : 16))
                    .foregroundStyle(colors.text)
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(name): \(usage)%")
                        .font(.system(size: density.labelSize, weight: .semibold))
                        .foregroundStyle(colors.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                    ThinProgressBar(progress: bar)
                }
                Text(tempText)
                    .font(.system(size: density.labelSize, weight: .medium))
                    .foregroundStyle(colors.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                    .layoutPriority(1)
            }
        }
    }
}

struct LogoTimePanel: View {
    let data: DashboardSnapshot
    var density: PanelDensity = .compact
    var onResetStats: (() -> Void)?
    @Environment(\.dashColors) private var colors

    var body: some View {
        DashPanel(density: density) {
            GeometryReader { geo in
                let hasReset = onResetStats != nil
                let dateSize = max(16, min(geo.size.height * (hasReset ? 0.18 : 0.22), 26))
                let timeSize = max(22, min(geo.size.height * (hasReset ? 0.32 : 0.42), 44))
                let dateIcon = max(16, dateSize * 0.9)
                let timeIcon = max(20, timeSize * 0.7)

                VStack(spacing: max(geo.size.height * 0.05, 4)) {
                    if let onResetStats {
                        Button("重置统计", action: onResetStats)
                            .font(.system(size: min(13, geo.size.height * 0.12), weight: .bold))
                            .foregroundStyle(colors.accentYellow)
                            .buttonStyle(.plain)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    HStack(spacing: 6) {
                        Image(systemName: "calendar")
                            .font(.system(size: dateIcon, weight: .semibold))
                            .foregroundStyle(colors.text)
                        Text(data.date)
                            .font(.system(size: dateSize, weight: .semibold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.5)
                    }
                    HStack(spacing: 6) {
                        Image(systemName: "clock")
                            .font(.system(size: timeIcon, weight: .semibold))
                            .foregroundStyle(colors.text)
                        Text(data.time)
                            .font(.system(size: timeSize, weight: .bold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.4)
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height)
                .clipped()
            }
        }
    }
}

struct NetFanPanel: View {
    let data: DashboardSnapshot
    var barPeaks: BarScalePeaks = BarScalePeaks()
    var density: PanelDensity = .compact
    var isFullscreen: Bool = false
    var onToggleFullscreen: (() -> Void)?
    @Environment(\.dashColors) private var colors

    private var gpuFanColor: Color {
        (Int(data.gpuFan) ?? 0) == 0 ? colors.accentRed : colors.accentYellow
    }

    private var volumeProgress: Float {
        if let peak = barPeaks.volume, peak > 0 {
            return min(max(data.volumeBar * 100 / peak, 0), 1)
        }
        return data.volumeBar
    }

    var body: some View {
        DashPanel(density: density) {
            GeometryReader { geo in
                let tight = geo.size.height < 120
                let spacing = tight ? max(2, geo.size.height * 0.04) : CGFloat(density.metricSpacing + 2)
                let iconSize: CGFloat = tight ? 13 : (density == .compact ? 16 : 20)
                let labelSize: CGFloat = tight ? 10 : density.labelSize + 1
                let fanLabelWidth: CGFloat = tight ? 52 : (density == .compact ? 58 : 72)

                ZStack(alignment: .bottomTrailing) {
                    VStack(alignment: .leading, spacing: spacing) {
                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "cloud")
                                .font(.system(size: iconSize))
                                .foregroundStyle(colors.text)
                                .frame(width: iconSize + 2, alignment: .center)
                            VStack(alignment: .leading, spacing: tight ? 1 : 2) {
                                Text("上传  \(data.upload) KB/s")
                                    .font(.system(size: labelSize, weight: .semibold))
                                    .foregroundStyle(colors.text)
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.65)
                                Text("下载  \(data.download) KB/s")
                                    .font(.system(size: labelSize, weight: .semibold))
                                    .foregroundStyle(colors.text)
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.65)
                            }
                        }

                        HStack(spacing: 6) {
                            Image(systemName: "speaker.wave.2")
                                .font(.system(size: iconSize - 1))
                                .foregroundStyle(colors.text)
                                .frame(width: iconSize + 2, alignment: .center)
                            ThinProgressBar(progress: volumeProgress)
                        }

                        FanRow(
                            label: "CPU/FAN",
                            value: data.cpuFan,
                            color: colors.accentYellow,
                            labelWidth: fanLabelWidth,
                            fontSize: labelSize,
                            iconSize: iconSize - 2
                        )
                        FanRow(
                            label: "GPU/FAN",
                            value: data.gpuFan,
                            color: gpuFanColor,
                            labelWidth: fanLabelWidth,
                            fontSize: labelSize,
                            iconSize: iconSize - 2
                        )

                        Spacer(minLength: 0)
                    }
                    .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
                    .padding(.trailing, onToggleFullscreen == nil ? 0 : 18)
                    .padding(.bottom, onToggleFullscreen == nil ? 0 : 2)

                    if let onToggleFullscreen {
                        Button(action: onToggleFullscreen) {
                            Image(systemName: isFullscreen ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                                .foregroundStyle(colors.text)
                                .font(.system(size: tight ? 12 : 14, weight: .semibold))
                                .padding(2)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
                .clipped()
            }
        }
    }

    private struct FanRow: View {
        let label: String
        let value: String
        let color: Color
        var labelWidth: CGFloat
        var fontSize: CGFloat
        var iconSize: CGFloat
        @Environment(\.dashColors) private var colors

        var body: some View {
            HStack(spacing: 4) {
                Image(systemName: "fanblades")
                    .font(.system(size: iconSize))
                    .foregroundStyle(colors.text)
                    .frame(width: iconSize + 2, alignment: .center)
                Text(label)
                    .font(.system(size: fontSize))
                    .foregroundStyle(colors.text)
                    .frame(width: labelWidth, alignment: .leading)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text("\(value) RPM")
                    .font(.system(size: fontSize, weight: .bold))
                    .foregroundStyle(color)
                    .lineLimit(1)
                    .minimumScaleFactor(0.65)
            }
        }
    }
}
