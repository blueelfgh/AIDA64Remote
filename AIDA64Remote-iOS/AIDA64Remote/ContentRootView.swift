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
                        onExitFullscreen: { viewModel.setFullscreen(false) }
                    )
                    .navigationTitle("监控")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar(.hidden, for: .navigationBar)
                } else {
                    SettingsView(
                        initialHost: viewModel.savedConfig.host,
                        initialPort: viewModel.savedConfig.port,
                        keepScreenOn: viewModel.settings.keepScreenOn,
                        themeMode: viewModel.settings.themeMode,
                        showsDisconnect: false,
                        onKeepScreenOnChange: viewModel.setKeepScreenOn,
                        onThemeModeChange: viewModel.setThemeMode,
                        onConnect: { host, port in
                            didAutoConnect = true
                            viewModel.saveAndConnect(host: host, port: port)
                            path = NavigationPath()
                            rootIsMonitor = true
                        },
                        onDisconnect: nil
                    )
                }
            }
            .navigationDestination(for: Destination.self) { destination in
                switch destination {
                case .settings:
                    SettingsView(
                        initialHost: viewModel.savedConfig.host,
                        initialPort: viewModel.savedConfig.port,
                        keepScreenOn: viewModel.settings.keepScreenOn,
                        themeMode: viewModel.settings.themeMode,
                        showsDisconnect: true,
                        onKeepScreenOnChange: viewModel.setKeepScreenOn,
                        onThemeModeChange: viewModel.setThemeMode,
                        onConnect: { host, port in
                            didAutoConnect = true
                            viewModel.saveAndConnect(host: host, port: port)
                            path.removeLast()
                        },
                        onDisconnect: {
                            viewModel.disconnect()
                            path = NavigationPath()
                            rootIsMonitor = false
                        }
                    )
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
