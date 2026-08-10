package com.example.aida64remote.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class DashboardColors(
    val bg: Color,
    val panel: Color,
    val border: Color,
    val track: Color,
    val fill: Color,
    val text: Color,
    val muted: Color,
    val accentYellow: Color,
    val accentGreen: Color,
    val accentRed: Color,
    val hex: Color,
    val hexGlow: Color,
    val sparkline: Color,
    val badgeBg: Color,
    val badgeText: Color,
)

val DarkDashboardColors = DashboardColors(
    bg = Color(0xFF121214),
    panel = Color(0xCC1A1A1E),
    border = Color(0xFF8A8A90),
    track = Color(0xFF3A3A40),
    fill = Color(0xFFD0D0D4),
    text = Color(0xFFF2F2F2),
    muted = Color(0xFFB8B8BE),
    accentYellow = Color(0xFFFFE14D),
    accentGreen = Color(0xFF4CD964),
    accentRed = Color(0xFFFF4D4D),
    hex = Color(0xFF2A2A30),
    hexGlow = Color(0x66C41A00),
    sparkline = Color(0xFF9AD1FF),
    badgeBg = Color(0xCC000000),
    badgeText = Color(0xFFFFE14D),
)

val LightDashboardColors = DashboardColors(
    bg = Color(0xFFE8ECF2),
    panel = Color(0xF2FFFFFF),
    border = Color(0xFF9AA3B2),
    track = Color(0xFFD5DBE5),
    fill = Color(0xFF4A5568),
    text = Color(0xFF1A1F2C),
    muted = Color(0xFF5C667A),
    accentYellow = Color(0xFFC48A00),
    accentGreen = Color(0xFF2E7D32),
    accentRed = Color(0xFFD32F2F),
    hex = Color(0xFFCDD5E2),
    hexGlow = Color(0x66A0AEC0),
    sparkline = Color(0xFF2B6CB0),
    badgeBg = Color(0xFF1A1F2C),
    badgeText = Color(0xFFFFE14D),
)

val LocalDashboardColors = staticCompositionLocalOf { DarkDashboardColors }

val DashColors: DashboardColors
    @Composable
    get() = LocalDashboardColors.current

// Compat aliases used during migration / Material schemes
val DashBg get() = DarkDashboardColors.bg
val DashPanel get() = DarkDashboardColors.panel
val DashBorder get() = DarkDashboardColors.border
val DashTrack get() = DarkDashboardColors.track
val DashFill get() = DarkDashboardColors.fill
val DashText get() = DarkDashboardColors.text
val DashMuted get() = DarkDashboardColors.muted
val DashAccentYellow get() = DarkDashboardColors.accentYellow
val DashAccentGreen get() = DarkDashboardColors.accentGreen
val DashAccentRed get() = DarkDashboardColors.accentRed
val DashHex get() = DarkDashboardColors.hex
val DashHexGlow get() = DarkDashboardColors.hexGlow
