import SwiftUI

struct MonitorView: View {
    let state: MonitorUiState
    let onRetry: () -> Void
    let onOpenSettings: () -> Void
    let onToggleFullscreen: () -> Void
    let onExitFullscreen: () -> Void

    @Environment(\.dashColors) private var colors

    private var isWaitingData: Bool {
        state.status == .connecting
            || state.status == .reconnecting
            || (state.status == .connected
                && state.dashboard.cpuTemp == "—"
                && state.dashboard.cpuClock == "—")
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
                            onToggleFullscreen: onToggleFullscreen
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
                        data: state.dashboard,
                        keepScreenOn: state.keepScreenOn,
                        isFullscreen: state.isFullscreen,
                        gap: gap,
                        onToggleFullscreen: onToggleFullscreen
                    )
                } else {
                    PortraitDashboard(
                        data: state.dashboard,
                        keepScreenOn: state.keepScreenOn,
                        isFullscreen: state.isFullscreen,
                        gap: gap,
                        onToggleFullscreen: onToggleFullscreen
                    )
                }
            }
            .padding(pad)
            .frame(width: geo.size.width, height: geo.size.height, alignment: .top)
        }
    }
}

private struct LandscapeDashboard: View {
    let data: DashboardSnapshot
    let keepScreenOn: Bool
    let isFullscreen: Bool
    let gap: CGFloat
    let onToggleFullscreen: () -> Void

    var body: some View {
        GeometryReader { geo in
            let rowGap = gap
            let usable = geo.size.height - rowGap * 2
            let row1 = usable * 0.36
            let row2 = usable * 0.34
            let row3 = usable * 0.30
            let colGap = gap
            let leftW = (geo.size.width - colGap) * 0.46
            let rightW = geo.size.width - colGap - leftW

            VStack(spacing: rowGap) {
                HStack(spacing: colGap) {
                    CpuPanel(data: data, showKeepScreenOn: keepScreenOn, density: .regular)
                        .frame(width: leftW, height: row1)
                    GpuPanel(data: data, density: .compact)
                        .frame(width: rightW, height: row1)
                }

                HStack(spacing: colGap) {
                    FpsPanel(data: data)
                        .frame(width: leftW, height: row2)
                    RamPanel(data: data, density: .regular)
                        .frame(width: rightW, height: row2)
                }

                HStack(spacing: colGap) {
                    let total = geo.size.width - colGap * 2
                    StoragePanel(data: data, density: .compact)
                        .frame(width: total * 0.38, height: row3)
                    LogoTimePanel(data: data, density: .compact)
                        .frame(width: total * 0.28, height: row3)
                    NetFanPanel(
                        data: data,
                        density: .compact,
                        isFullscreen: isFullscreen,
                        onToggleFullscreen: onToggleFullscreen
                    )
                    .frame(width: total * 0.34, height: row3)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height, alignment: .top)
        }
    }
}

private struct PortraitDashboard: View {
    let data: DashboardSnapshot
    let keepScreenOn: Bool
    let isFullscreen: Bool
    let gap: CGFloat
    let onToggleFullscreen: () -> Void

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: gap) {
                CpuPanel(data: data, showKeepScreenOn: keepScreenOn, density: .regular)
                    .frame(minHeight: 120)
                    .frame(maxWidth: .infinity)
                    .aspectRatio(2.4, contentMode: .fit)

                GpuPanel(data: data, density: .regular)
                    .frame(minHeight: 160)
                    .frame(maxWidth: .infinity)
                    .aspectRatio(1.8, contentMode: .fit)

                HStack(spacing: gap) {
                    FpsPanel(data: data)
                        .frame(maxWidth: .infinity)
                        .frame(height: 140)
                    RamPanel(data: data, density: .compact)
                        .frame(maxWidth: .infinity)
                        .frame(height: 140)
                }

                StoragePanel(data: data, density: .regular)
                    .frame(minHeight: 120)
                    .frame(maxWidth: .infinity)

                HStack(spacing: gap) {
                    LogoTimePanel(data: data, density: .regular)
                        .frame(maxWidth: .infinity)
                        .frame(height: 140)
                    NetFanPanel(
                        data: data,
                        density: .compact,
                        isFullscreen: isFullscreen,
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
