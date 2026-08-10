import SwiftUI

struct AppThemeModifier: ViewModifier {
    let themeMode: ThemeMode
    @Environment(\.colorScheme) private var systemScheme

    private var resolvedScheme: ColorScheme {
        switch themeMode {
        case .dark: return .dark
        case .light: return .light
        case .system: return systemScheme
        }
    }

    private var colors: DashboardColors {
        resolvedScheme == .dark ? DashPalette.dark : DashPalette.light
    }

    func body(content: Content) -> some View {
        content
            .preferredColorScheme(themeMode == .system ? nil : resolvedScheme)
            .environment(\.dashColors, colors)
            .tint(colors.accentYellow)
    }
}

extension View {
    func appTheme(_ mode: ThemeMode) -> some View {
        modifier(AppThemeModifier(themeMode: mode))
    }
}
