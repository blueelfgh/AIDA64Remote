import SwiftUI

struct DashboardColors {
    let bg: Color
    let panel: Color
    let border: Color
    let track: Color
    let fill: Color
    let text: Color
    let muted: Color
    let accentYellow: Color
    let accentRed: Color
    let hex: Color
    let hexGlow: Color
    let sparkline: Color
    let badgeBg: Color
    let badgeText: Color
}

enum DashPalette {
    static let dark = DashboardColors(
        bg: Color(red: 0x12 / 255, green: 0x12 / 255, blue: 0x14 / 255),
        panel: Color(red: 0x1A / 255, green: 0x1A / 255, blue: 0x1E / 255).opacity(0.8),
        border: Color(red: 0x8A / 255, green: 0x8A / 255, blue: 0x90 / 255),
        track: Color(red: 0x3A / 255, green: 0x3A / 255, blue: 0x40 / 255),
        fill: Color(red: 0xD0 / 255, green: 0xD0 / 255, blue: 0xD4 / 255),
        text: Color(red: 0xF2 / 255, green: 0xF2 / 255, blue: 0xF2 / 255),
        muted: Color(red: 0xB8 / 255, green: 0xB8 / 255, blue: 0xBE / 255),
        accentYellow: Color(red: 1, green: 0xE1 / 255, blue: 0x4D / 255),
        accentRed: Color(red: 1, green: 0x4D / 255, blue: 0x4D / 255),
        hex: Color(red: 0x2A / 255, green: 0x2A / 255, blue: 0x30 / 255),
        hexGlow: Color(red: 0xC4 / 255, green: 0x1A / 255, blue: 0).opacity(0.4),
        sparkline: Color(red: 0x9A / 255, green: 0xD1 / 255, blue: 1),
        badgeBg: Color.black.opacity(0.8),
        badgeText: Color(red: 1, green: 0xE1 / 255, blue: 0x4D / 255)
    )

    static let light = DashboardColors(
        bg: Color(red: 0xE8 / 255, green: 0xEC / 255, blue: 0xF2 / 255),
        panel: Color.white.opacity(0.95),
        border: Color(red: 0x9A / 255, green: 0xA3 / 255, blue: 0xB2 / 255),
        track: Color(red: 0xD5 / 255, green: 0xDB / 255, blue: 0xE5 / 255),
        fill: Color(red: 0x4A / 255, green: 0x55 / 255, blue: 0x68 / 255),
        text: Color(red: 0x1A / 255, green: 0x1F / 255, blue: 0x2C / 255),
        muted: Color(red: 0x5C / 255, green: 0x66 / 255, blue: 0x7A / 255),
        accentYellow: Color(red: 0xC4 / 255, green: 0x8A / 255, blue: 0),
        accentRed: Color(red: 0xD3 / 255, green: 0x2F / 255, blue: 0x2F / 255),
        hex: Color(red: 0xCD / 255, green: 0xD5 / 255, blue: 0xE2 / 255),
        hexGlow: Color(red: 0xA0 / 255, green: 0xAE / 255, blue: 0xC0 / 255).opacity(0.4),
        sparkline: Color(red: 0x2B / 255, green: 0x6C / 255, blue: 0xB0 / 255),
        badgeBg: Color(red: 0x1A / 255, green: 0x1F / 255, blue: 0x2C / 255),
        badgeText: Color(red: 1, green: 0xE1 / 255, blue: 0x4D / 255)
    )
}

private struct DashboardColorsKey: EnvironmentKey {
    static let defaultValue = DashPalette.dark
}

extension EnvironmentValues {
    var dashColors: DashboardColors {
        get { self[DashboardColorsKey.self] }
        set { self[DashboardColorsKey.self] = newValue }
    }
}
