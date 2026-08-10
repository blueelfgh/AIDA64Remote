import SwiftUI

struct ContentRootView: View {
    @Bindable var viewModel: SensorViewModel
    @State private var path = NavigationPath()
    @State private var rootIsMonitor = false
    @State private var didAutoConnect = false

    private enum Destination: Hashable {
        case settings
    }

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if rootIsMonitor {
                    MonitorView(
                        state: viewModel.uiState,
                        onRetry: { viewModel.connect() },
                        onOpenSettings: { path.append(Destination.settings) },
                        onToggleFullscreen: viewModel.toggleFullscreen,
                        onExitFullscreen: { viewModel.setFullscreen(false) },
                        onResetStats: viewModel.resetMetricStats
                    )
                    .navigationTitle("监控")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar(.hidden, for: .navigationBar)
                } else {
                    settingsView(showsDisconnect: false) { host, port, type in
                        didAutoConnect = true
                        viewModel.saveAndConnect(host: host, port: port, serviceType: type)
                        path = NavigationPath()
                        rootIsMonitor = true
                    }
                }
            }
            .navigationDestination(for: Destination.self) { destination in
                switch destination {
                case .settings:
                    settingsView(showsDisconnect: true) { host, port, type in
                        didAutoConnect = true
                        viewModel.saveAndConnect(host: host, port: port, serviceType: type)
                        path.removeLast()
                    } onDisconnect: {
                        viewModel.disconnect()
                        path = NavigationPath()
                        rootIsMonitor = false
                    }
                }
            }
        }
        .onAppear {
            viewModel.syncSettingsIntoState()
            autoConnectIfNeeded()
        }
        .onChange(of: viewModel.settings.host) { _, _ in
            autoConnectIfNeeded()
        }
    }

    @ViewBuilder
    private func settingsView(
        showsDisconnect: Bool,
        onConnect: @escaping (String, Int, ServiceType) -> Void,
        onDisconnect: (() -> Void)? = nil
    ) -> some View {
        SettingsView(
            initialHost: viewModel.savedConfig.host,
            initialPort: viewModel.savedConfig.port,
            serviceType: viewModel.savedConfig.serviceType,
            keepScreenOn: viewModel.settings.keepScreenOn,
            themeMode: viewModel.settings.themeMode,
            showsDisconnect: showsDisconnect,
            onServiceTypeChange: viewModel.setServiceType,
            onKeepScreenOnChange: viewModel.setKeepScreenOn,
            onThemeModeChange: viewModel.setThemeMode,
            onClearBarPeaks: viewModel.clearBarScalePeaks,
            onConnect: onConnect,
            onDisconnect: onDisconnect
        )
    }

    private func autoConnectIfNeeded() {
        guard !didAutoConnect,
              viewModel.uiState.status == .idle,
              !viewModel.savedConfig.host.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else { return }
        didAutoConnect = true
        viewModel.connect(config: viewModel.savedConfig)
        rootIsMonitor = true
    }
}
