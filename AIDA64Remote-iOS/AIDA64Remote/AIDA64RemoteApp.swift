import SwiftUI
import UIKit

@main
struct AIDA64RemoteApp: App {
    @State private var viewModel = SensorViewModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentRootView(viewModel: viewModel)
                .appTheme(viewModel.settings.themeMode)
                .onAppear {
                    applyIdleTimer(viewModel.settings.keepScreenOn)
                }
                .onChange(of: viewModel.settings.keepScreenOn) { _, enabled in
                    applyIdleTimer(enabled)
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active {
                        applyIdleTimer(viewModel.settings.keepScreenOn)
                    }
                }
        }
    }

    private func applyIdleTimer(_ keepOn: Bool) {
        UIApplication.shared.isIdleTimerDisabled = keepOn
    }
}
