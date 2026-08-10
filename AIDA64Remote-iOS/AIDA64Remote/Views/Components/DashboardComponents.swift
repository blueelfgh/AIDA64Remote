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
            .clipped()
    }
}

struct ThinProgressBar: View {
    let progress: Float
    @Environment(\.dashColors) private var colors

    var body: some View {
        // 不用裸 GeometryReader 撑开父布局：用 Preference/背景比例填充。
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
    var density: PanelDensity = .regular
    @Environment(\.dashColors) private var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 6) {
                Text(label)
                    .font(.system(size: density.labelSize, weight: .semibold))
                    .foregroundStyle(colors.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                Spacer(minLength: 4)
                Text(unit.isEmpty ? value : "\(value) \(unit)")
                    .font(.system(size: density.valueSize, weight: .bold))
                    .foregroundStyle(colors.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
            ThinProgressBar(progress: progress)
        }
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

    var body: some View {
        Text(temp)
            .font(.system(size: tempSize, weight: .bold))
            .foregroundStyle(colors.text)
            .lineLimit(1)
            .minimumScaleFactor(0.5)
    }
}

struct CpuPanel: View {
    let data: DashboardSnapshot
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
                    TempValue(temp: data.cpuTemp, tempSize: density.tempSize)
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
                            density: density
                        )
                        MetricBarRow(
                            label: "CPU 使用率",
                            value: data.cpuUsage,
                            unit: "%",
                            progress: data.cpuUsageBar,
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
    var density: PanelDensity = .compact

    private var gpuTempDigits: String {
        String(data.gpuTemp.filter { $0.isNumber || $0 == "." })
    }

    var body: some View {
        DashPanel(density: density) {
            GeometryReader { geo in
                let narrow = geo.size.width < 280
                let rows: [(String, String, String, Float)] = [
                    ("已用显存", data.vramUsed, "MB", data.vramUsedBar),
                    ("可用显存", data.vramFree, "MB", data.vramFreeBar),
                    ("GPU 核心频率", data.gpuClock, "MHz", data.gpuClockBar),
                    ("GPU 显存频率", data.gpuMemClock, "MHz", data.gpuMemClockBar),
                    ("GPU 使用率", data.gpuUsage, "%", data.gpuUsageBar),
                    ("GPU 温度", gpuTempDigits, "°C", data.gpuTempBar),
                ]

                Group {
                    if narrow {
                        VStack(alignment: .leading, spacing: density.metricSpacing) {
                            TempValue(temp: data.gpuTemp, tempSize: density.tempSize)
                            metricsGrid(rows: rows, columns: 1)
                        }
                    } else {
                        HStack(alignment: .center, spacing: 8) {
                            TempValue(temp: data.gpuTemp, tempSize: density.tempSize)
                                .frame(minWidth: 56)
                            metricsGrid(rows: rows, columns: geo.size.height < 140 ? 2 : 1)
                        }
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height, alignment: .center)
            }
        }
    }

    @ViewBuilder
    private func metricsGrid(rows: [(String, String, String, Float)], columns: Int) -> some View {
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

    private func metricColumn(_ rows: [(String, String, String, Float)]) -> some View {
        VStack(spacing: density.metricSpacing) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                MetricBarRow(
                    label: row.0,
                    value: row.1,
                    unit: row.2,
                    progress: row.3,
                    density: density
                )
            }
        }
    }
}

struct FpsPanel: View {
    let data: DashboardSnapshot
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
                Text("\(data.fps) FPS")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(colors.text)
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
    var density: PanelDensity = .regular
    @Environment(\.dashColors) private var colors

    var body: some View {
        DashPanel(density: density) {
            HStack(alignment: .center, spacing: 8) {
                VStack(alignment: .leading, spacing: density.metricSpacing) {
                    HStack(spacing: 6) {
                        Image(systemName: "memorychip")
                            .foregroundStyle(colors.text)
                            .font(.system(size: density == .compact ? 16 : 20))
                        Text(data.ramType)
                            .font(.system(size: density.titleSize, weight: .bold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    MetricBarRow(label: "已用内存", value: data.ramUsed, progress: data.ramUsedBar, density: density)
                    MetricBarRow(label: "可用内存", value: data.ramFree, progress: data.ramFreeBar, density: density)
                    MetricBarRow(label: "使用率", value: data.ramUsage, progress: data.ramUsageBar, density: density)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                TempValue(temp: data.boardTemp, tempSize: density.tempSize)
                    .frame(width: density == .compact ? 56 : 72)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

struct StoragePanel: View {
    let data: DashboardSnapshot
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
        var density: PanelDensity
        @Environment(\.dashColors) private var colors

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
                Text("温度: \(temp)")
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
    @Environment(\.dashColors) private var colors

    var body: some View {
        DashPanel(density: density) {
            // 对齐 Android LibreHardwareMonitorClient：无品牌，日期 22 / 时间 36，并随面板放大。
            GeometryReader { geo in
                let dateSize = max(22, min(geo.size.height * 0.22, 28))
                let timeSize = max(36, min(geo.size.height * 0.42, 52))
                let dateIcon = max(22, dateSize)
                let timeIcon = max(28, timeSize * 0.72)

                VStack(spacing: max(geo.size.height * 0.1, 10)) {
                    HStack(spacing: 8) {
                        Image(systemName: "calendar")
                            .font(.system(size: dateIcon, weight: .semibold))
                            .foregroundStyle(colors.text)
                        Text(data.date)
                            .font(.system(size: dateSize, weight: .semibold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                    }
                    HStack(spacing: 8) {
                        Image(systemName: "clock")
                            .font(.system(size: timeIcon, weight: .semibold))
                            .foregroundStyle(colors.text)
                        Text(data.time)
                            .font(.system(size: timeSize, weight: .bold))
                            .foregroundStyle(colors.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.5)
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height)
            }
        }
    }
}

struct NetFanPanel: View {
    let data: DashboardSnapshot
    var density: PanelDensity = .compact
    var isFullscreen: Bool = false
    var onToggleFullscreen: (() -> Void)?
    @Environment(\.dashColors) private var colors

    private var gpuFanColor: Color {
        (Int(data.gpuFan) ?? 0) == 0 ? colors.accentRed : colors.accentYellow
    }

    var body: some View {
        DashPanel(density: density) {
            ZStack(alignment: .bottomTrailing) {
                VStack(alignment: .leading, spacing: density.metricSpacing + 2) {
                    HStack(spacing: 8) {
                        Image(systemName: "cloud")
                            .font(.system(size: density == .compact ? 16 : 20))
                            .foregroundStyle(colors.text)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("上传  \(data.upload) KB/s")
                                .font(.system(size: density.labelSize + 1, weight: .semibold))
                                .foregroundStyle(colors.text)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                            Text("下载  \(data.download) KB/s")
                                .font(.system(size: density.labelSize + 1, weight: .semibold))
                                .foregroundStyle(colors.text)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                        }
                    }
                    HStack(spacing: 8) {
                        Image(systemName: "speaker.wave.2")
                            .foregroundStyle(colors.text)
                        ThinProgressBar(progress: data.volumeBar)
                    }
                    FanRow(label: "CPU/FAN", value: data.cpuFan, color: colors.accentYellow, density: density)
                    FanRow(label: "GPU/FAN", value: data.gpuFan, color: gpuFanColor, density: density)
                }
                .padding(.bottom, onToggleFullscreen == nil ? 0 : 22)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)

                if let onToggleFullscreen {
                    Button(action: onToggleFullscreen) {
                        Image(systemName: isFullscreen ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                            .foregroundStyle(colors.text)
                            .font(.system(size: 14, weight: .semibold))
                            .padding(4)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private struct FanRow: View {
        let label: String
        let value: String
        let color: Color
        var density: PanelDensity
        @Environment(\.dashColors) private var colors

        var body: some View {
            HStack(spacing: 6) {
                Image(systemName: "fanblades")
                    .font(.system(size: density == .compact ? 12 : 14))
                    .foregroundStyle(colors.text)
                Text(label)
                    .font(.system(size: density.labelSize))
                    .foregroundStyle(colors.text)
                    .frame(width: density == .compact ? 58 : 72, alignment: .leading)
                Text("\(value) RPM")
                    .font(.system(size: density.valueSize, weight: .bold))
                    .foregroundStyle(color)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
    }
}
