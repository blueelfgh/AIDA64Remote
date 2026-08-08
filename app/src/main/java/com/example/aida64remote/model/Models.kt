package com.example.aida64remote.model

data class SensorItem(
    val id: String,
    val value: String,
    val label: String? = null,
) {
    val displayName: String get() = label?.takeIf { it.isNotBlank() } ?: id
}

enum class ConnectionStatus {
    Idle,
    Connecting,
    Connected,
    Reconnecting,
    Error,
}

sealed interface ConnectionEvent {
    data object Connecting : ConnectionEvent
    data object Connected : ConnectionEvent
    data class SensorsUpdated(
        val sensors: List<SensorItem>,
        val labels: Map<String, String> = emptyMap(),
    ) : ConnectionEvent
    data class Reconnecting(val attempt: Int, val message: String?) : ConnectionEvent
    data class Disconnected(val message: String?) : ConnectionEvent
}

data class DashboardSnapshot(
    val cpuName: String = "—",
    val cpuTemp: String = "—",
    val cpuClock: String = "—",
    val cpuClockBar: Float = 0f,
    val cpuUsage: String = "—",
    val cpuUsageBar: Float = 0f,
    val gpuTemp: String = "—",
    val vramUsed: String = "—",
    val vramUsedBar: Float = 0f,
    val vramFree: String = "—",
    val vramFreeBar: Float = 0f,
    val gpuClock: String = "—",
    val gpuClockBar: Float = 0f,
    val gpuMemClock: String = "—",
    val gpuMemClockBar: Float = 0f,
    val gpuUsage: String = "—",
    val gpuUsageBar: Float = 0f,
    val gpuTempBar: Float = 0f,
    val fps: String = "0",
    val fpsHistory: List<Float> = emptyList(),
    val gpuHistory: List<Float> = emptyList(),
    val ramType: String = "—",
    val ramUsed: String = "—",
    val ramUsedBar: Float = 0f,
    val ramFree: String = "—",
    val ramFreeBar: Float = 0f,
    val ramUsage: String = "—",
    val ramUsageBar: Float = 0f,
    val boardTemp: String = "—",
    val driveCUsage: String = "—",
    val driveCBar: Float = 0f,
    val driveCTemp: String = "—",
    val driveDUsage: String = "—",
    val driveDBar: Float = 0f,
    val driveDTemp: String = "—",
    val driveEUsage: String = "—",
    val driveEBar: Float = 0f,
    val driveETemp: String = "—",
    val date: String = "—",
    val time: String = "—",
    val upload: String = "—",
    val download: String = "—",
    val volumeBar: Float = 0f,
    val cpuFan: String = "—",
    val gpuFan: String = "—",
)

fun Map<String, String>.toDashboard(
    labels: Map<String, String>,
    fpsHistory: List<Float>,
    gpuHistory: List<Float>,
): DashboardSnapshot {
    fun v(id: String, fallback: String = "—") = this[id]?.takeIf { it.isNotBlank() } ?: fallback
    fun bar(id: String): Float = this[id]?.toFloatOrNull()?.coerceIn(0f, 100f)?.div(100f) ?: 0f
    fun temp(id: String): String {
        val raw = v(id, "")
        if (raw.isEmpty()) return "—"
        return raw.replace("温度:", "").replace("温度：", "").trim()
    }

    return DashboardSnapshot(
        cpuName = labels["Label2"] ?: "Intel Core",
        cpuTemp = v("Simple3"),
        cpuClock = v("SIV4"),
        cpuClockBar = bar("Bar4p"),
        cpuUsage = v("SIV5"),
        cpuUsageBar = bar("Bar5p"),
        gpuTemp = v("Simple29").ifBlank { v("SIV11") },
        vramUsed = v("SIV6"),
        vramUsedBar = bar("Bar6p"),
        vramFree = v("SIV7"),
        vramFreeBar = bar("Bar7p"),
        gpuClock = v("SIV8"),
        gpuClockBar = bar("Bar8p"),
        gpuMemClock = v("SIV9"),
        gpuMemClockBar = bar("Bar9p"),
        gpuUsage = v("SIV10"),
        gpuUsageBar = bar("Bar10p"),
        gpuTempBar = bar("Bar11p"),
        fps = v("SIV23", "0"),
        fpsHistory = fpsHistory,
        gpuHistory = gpuHistory,
        ramType = v("Simple21"),
        ramUsed = v("Simple18"),
        ramUsedBar = bar("Bar15p"),
        ramFree = v("Simple19"),
        ramFreeBar = bar("Bar16p"),
        ramUsage = v("Simple20"),
        ramUsageBar = bar("Bar17p"),
        boardTemp = v("Simple30"),
        driveCUsage = v("SIV12"),
        driveCBar = bar("Bar12p"),
        driveCTemp = temp("Simple26"),
        driveDUsage = v("SIV13"),
        driveDBar = bar("Bar13p"),
        driveDTemp = temp("Simple27"),
        driveEUsage = v("SIV14"),
        driveEBar = bar("Bar14p"),
        driveETemp = temp("Simple28"),
        date = v("Simple31"),
        time = v("Simple32"),
        upload = v("SIV33"),
        download = v("SIV34"),
        volumeBar = (this["SIV22"]?.toFloatOrNull() ?: this["Bar22p"]?.toFloatOrNull() ?: 0f)
            .coerceIn(0f, 100f) / 100f,
        cpuFan = v("SIV35"),
        gpuFan = v("SIV36"),
    )
}
