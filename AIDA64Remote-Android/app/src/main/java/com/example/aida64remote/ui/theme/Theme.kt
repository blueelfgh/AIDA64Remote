package com.example.aida64remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.aida64remote.data.ThemeMode

private val DashboardDarkScheme = darkColorScheme(
    primary = DarkDashboardColors.fill,
    secondary = DarkDashboardColors.muted,
    tertiary = DarkDashboardColors.accentYellow,
    background = DarkDashboardColors.bg,
    surface = DarkDashboardColors.panel,
    onPrimary = Color.Black,
    onSecondary = DarkDashboardColors.text,
    onTertiary = Color.Black,
    onBackground = DarkDashboardColors.text,
    onSurface = DarkDashboardColors.text,
    error = DarkDashboardColors.accentRed,
)

private val DashboardLightScheme = lightColorScheme(
    primary = LightDashboardColors.fill,
    secondary = LightDashboardColors.muted,
    tertiary = LightDashboardColors.accentYellow,
    background = LightDashboardColors.bg,
    surface = LightDashboardColors.panel,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightDashboardColors.text,
    onSurface = LightDashboardColors.text,
    error = LightDashboardColors.accentRed,
)

@Composable
fun AIDA64RemoteTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val dashColors = if (darkTheme) DarkDashboardColors else LightDashboardColors
    CompositionLocalProvider(LocalDashboardColors provides dashColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DashboardDarkScheme else DashboardLightScheme,
            typography = Typography,
            content = content,
        )
    }
}
