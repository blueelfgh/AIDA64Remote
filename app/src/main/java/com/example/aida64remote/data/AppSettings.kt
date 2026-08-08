package com.example.aida64remote.data

enum class ServiceType {
    Aida64,
    LibreHardwareMonitor,
    ;

    val defaultPort: Int
        get() = when (this) {
            Aida64 -> ConnectionConfig.DEFAULT_AIDA_PORT
            LibreHardwareMonitor -> ConnectionConfig.DEFAULT_LHM_PORT
        }
}

enum class ThemeMode {
    Dark,
    Light,
    System,
}

data class AppSettings(
    val host: String = ConnectionConfig.DEFAULT_HOST,
    val port: Int = ConnectionConfig.DEFAULT_AIDA_PORT,
    val serviceType: ServiceType = ServiceType.Aida64,
    val keepScreenOn: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Dark,
) {
    val connection: ConnectionConfig
        get() = ConnectionConfig(host = host, port = port, serviceType = serviceType)
}
