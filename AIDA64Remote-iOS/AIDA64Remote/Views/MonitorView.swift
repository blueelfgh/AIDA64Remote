import SwiftUI

struct MonitorView: View {
    let state: MonitorUiState
    let onRetry: () -> Void
    let onOpenSettings: () -> Void
    let onToggleFullscreen: () -> Void
    let onExitFullscreen: () -> Void
    let onResetStats: () -> Void

    @Environment(\.dashColors) private var colors

    private var isWaitingData: Bool {
        state.status == .connecting
            || state.status == .reconnecting
            || (state.status == .connected && !state.dashboard.hasRenderableSensorData)
    }

    var body: some View {
        ZStack {
            HexBackground()

            Group {
                switch state.status {
                case .error:
                    CenterMessage(
                        message: state.errorMessage ?? "连接失败",
                        actionLabel: "重试",
                        onAction: onRetry,
                        onSettings: onOpenSettings
                    )
                default:
                    if isWaitingData {
                        waitingOverlay
                    } else {
                        DashboardLayout(
                            state: state,
                            onOpenSettings: onOpenSettings,
                            onToggleFullscreen: onToggleFullscreen,
                            onResetStats: onResetStats
                        )
                    }
                }
            }
        }
        .statusBarHidden(state.isFullscreen)
        .simultaneousGesture(
            TapGesture(count: 2).onEnded { onToggleFullscreen() }
        )
    }

    private var waitingOverlay: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 12) {
                ProgressView()
                    .tint(colors.accentYellow)
                Text(state.status == .reconnecting ? "重连中…" : "等待传感器数据…")
                    .foregroundStyle(colors.text)
                if let message = state.errorMessage {
                    Text(message)
                        .font(.caption)
                        .foregroundStyle(colors.muted)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if !state.isFullscreen {
                TopActions(state: state, onOpenSettings: onOpenSettings)
                    .padding(.horizontal, 12)
                    .padding(.top, 8)
            }
        }
    }
}

/// 按可用宽高在横屏网格 / 竖屏滚动栈之间切换。
private struct DashboardLayout: View {
    let state: MonitorUiState
    let onOpenSettings: () -> Void
    let onToggleFullscreen: () -> Void
    let onResetStats: () -> Void

    var body: some View {
        GeometryReader { geo in
            let landscape = geo.size.width > geo.size.height
            let gap: CGFloat = landscape ? 8 : 10
            let pad: CGFloat = state.isFullscreen ? 6 : 10

            VStack(spacing: gap) {
                if !state.isFullscreen {
                    TopActions(state: state, onOpenSettings: onOpenSettings)
                        .fixedSize(horizontal: false, vertical: true)
                }

                if landscape {
                    LandscapeDashboard(
                        state: state,
                        gap: gap,
                        onToggleFullscreen: onToggleFullscreen,
                        onResetStats: onResetStats
                    )
                } else {
                    PortraitDashboard(
                        state: state,
                        gap: gap,
                        onToggleFullscreen: onToggleFullscreen,
                        onResetStats: onResetStats
                    )
                }
            }
            .padding(pad)
            .frame(width: geo.size.width, height: geo.size.height, alignment: .top)
        }
    }
}

private struct LandscapeDashboard: View {
    let state: MonitorUiState
    let gap: CGFloat
    let onToggleFullscreen: () -> Void
    let onResetStats: () -> Void

    private var data: DashboardSnapshot { state.dashboard }

    /// 对齐 Android：上两行左右约 1 : 1.15，底行 1.2 : 0.85 : 1。
    private let leftWeight: CGFloat = 1
    private let rightWeight: CGFloat = 1.15
    private let rowWeights: [CGFloat] = [1.1, 1.1, 1.0]
    private let bottomWeights: [CGFloat] = [1.2, 0.85, 1.0]

    var body: some View {
        GeometryReader { geo in
            let rowGap = gap
            let usableH = max(geo.size.height - rowGap * 2, 0)
            let rowHs = proportionalSizes(total: usableH, weights: rowWeights)
            let colGap = gap
            let usableW = max(geo.size.width - colGap, 0)
            let topCols = proportionalSizes(total: usableW, weights: [leftWeight, rightWeight])
            let bottomUsableW = max(geo.size.width - colGap * 2, 0)
            let bottomCols = proportionalSizes(total: bottomUsableW, weights: bottomWeights)

            VStack(spacing: rowGap) {
                HStack(spacing: colGap) {
                    CpuPanel(
                        data: data,
                        cpuTempStats: state.cpuTempStats,
                        barPeaks: state.barPeaks,
                        showKeepScreenOn: state.keepScreenOn,
                        density: .compact
                    )
                    .frame(width: topCols[0], height: rowHs[0])
                    .clipped()
                    GpuPanel(
                        data: data,
                        gpuTempStats: state.gpuTempStats,
                        barPeaks: state.barPeaks,
                        density: .compact,
                        forceTwoColumnMetrics: true
                    )
                    .frame(width: topCols[1], height: rowHs[0])
                    .clipped()
                }
                .frame(width: geo.size.width, height: rowHs[0], alignment: .top)
                .clipped()

                HStack(spacing: colGap) {
                    FpsPanel(data: data, fpsStats: state.fpsStats)
                        .frame(width: topCols[0], height: rowHs[1])
                        .clipped()
                    RamPanel(
                        data: data,
                        ramTemp1Stats: state.ramTemp1Stats,
                        ramTemp2Stats: state.ramTemp2Stats,
                        density: .compact
                    )
                    .frame(width: topCols[1], height: rowHs[1])
                    .clipped()
                }
                .frame(width: geo.size.width, height: rowHs[1], alignment: .top)
                .clipped()

                HStack(spacing: colGap) {
                    StoragePanel(data: data, barPeaks: state.barPeaks, density: .compact)
                        .frame(width: bottomCols[0], height: rowHs[2])
                        .clipped()
                    LogoTimePanel(data: data, density: .compact, onResetStats: onResetStats)
                        .frame(width: bottomCols[1], height: rowHs[2])
                        .clipped()
                    NetFanPanel(
                        data: data,
                        barPeaks: state.barPeaks,
                        density: .compact,
                        isFullscreen: state.isFullscreen,
                        onToggleFullscreen: onToggleFullscreen
                    )
                    .frame(width: bottomCols[2], height: rowHs[2])
                    .clipped()
                }
                .frame(width: geo.size.width, height: rowHs[2], alignment: .top)
                .clipped()
            }
            .frame(width: geo.size.width, height: geo.size.height, alignment: .top)
            .clipped()
        }
    }

    private func proportionalSizes(total: CGFloat, weights: [CGFloat]) -> [CGFloat] {
        let sum = weights.reduce(0, +)
        guard sum > 0 else { return weights.map { _ in 0 } }
        return weights.map { floor(total * ($0 / sum)) }
    }
}

private struct PortraitDashboard: View {
    let state: MonitorUiState
    let gap: CGFloat
    let onToggleFullscreen: () -> Void
    let onResetStats: () -> Void

    private var data: DashboardSnapshot { state.dashboard }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: gap) {
                CpuPanel(
                    data: data,
                    cpuTempStats: state.cpuTempStats,
                    barPeaks: state.barPeaks,
                    showKeepScreenOn: state.keepScreenOn,
                    density: .regular
                )
                .frame(minHeight: 120)
                .frame(maxWidth: .infinity)
                .aspectRatio(2.4, contentMode: .fit)

                GpuPanel(
                    data: data,
                    gpuTempStats: state.gpuTempStats,
                    barPeaks: state.barPeaks,
                    density: .regular
                )
                .frame(minHeight: 160)
                .frame(maxWidth: .infinity)
                .aspectRatio(1.8, contentMode: .fit)

                HStack(spacing: gap) {
                    FpsPanel(data: data, fpsStats: state.fpsStats)
                        .frame(maxWidth: .infinity)
                        .frame(height: 140)
                    RamPanel(
                        data: data,
                        ramTemp1Stats: state.ramTemp1Stats,
                        ramTemp2Stats: state.ramTemp2Stats,
                        density: .compact
                    )
                    .frame(maxWidth: .infinity)
                    .frame(height: 140)
                }

                StoragePanel(data: data, barPeaks: state.barPeaks, density: .regular)
                    .frame(minHeight: 120)
                    .frame(maxWidth: .infinity)

                HStack(spacing: gap) {
                    LogoTimePanel(data: data, density: .regular, onResetStats: onResetStats)
                        .frame(maxWidth: .infinity)
                        .frame(height: 140)
                    NetFanPanel(
                        data: data,
                        barPeaks: state.barPeaks,
                        density: .compact,
                        isFullscreen: state.isFullscreen,
                        onToggleFullscreen: onToggleFullscreen
                    )
                    .frame(maxWidth: .infinity)
                    .frame(height: 140)
                }
            }
            .padding(.bottom, 8)
        }
    }
}

private struct TopActions: View {
    let state: MonitorUiState
    let onOpenSettings: () -> Void
    @Environment(\.dashColors) private var colors

    var body: some View {
        HStack {
            Text(statusLabel(state.status))
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(statusColor(state.status))
            Spacer()
            Button(action: onOpenSettings) {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(colors.text)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("设置")
        }
    }

    private func statusLabel(_ status: ConnectionStatus) -> String {
        switch status {
        case .idle: return "未连接"
        case .connecting: return "连接中…"
        case .connected: return "已连接"
        case .reconnecting: return "重连中…"
        case .error: return "连接失败"
        }
    }

    private func statusColor(_ status: ConnectionStatus) -> Color {
        switch status {
        case .connected: return colors.accentYellow
        case .error: return colors.accentRed
        default: return colors.muted
        }
    }
}

private struct CenterMessage: View {
    let message: String
    let actionLabel: String
    let onAction: () -> Void
    let onSettings: () -> Void
    @Environment(\.dashColors) private var colors

    var body: some View {
        VStack(spacing: 12) {
            Text(message).foregroundStyle(colors.text)
            Button(actionLabel, action: onAction)
                .buttonStyle(.borderedProminent)
            Button("设置", action: onSettings)
                .foregroundStyle(colors.muted)
        }
    }
}
