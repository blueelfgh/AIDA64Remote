package com.example.aida64remote.data

enum class ThemeMode {
    Dark,
    Light,
    System,
}

data class AppSettings(
    val host: String = ConnectionConfig.DEFAULT_HOST,
    val port: Int = ConnectionConfig.DEFAULT_PORT,
    val keepScreenOn: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Dark,
) {
    val connection: ConnectionConfig
        get() = ConnectionConfig(host = host, port = port)
}
