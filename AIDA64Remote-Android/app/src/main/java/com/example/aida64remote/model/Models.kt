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
    data class DashboardUpdated(val dashboard: DashboardSnapshot) : ConnectionEvent
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
    val ramTemp1: String = "—",
    val ramTemp2: String = "—",
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
    fun first(vararg ids: String, fallback: String = "—"): String {
        for (id in ids) {
            val value = this[id]?.takeIf { it.isNotBlank() }
            if (value != null) return value
        }
        return fallback
    }
    fun bar(id: String): Float = this[id]?.toFloatOrNull()?.coerceIn(0f, 100f)?.div(100f) ?: 0f
    fun temp(id: String): String {
        val raw = this[id]?.takeIf { it.isNotBlank() } ?: return "—"
        val cleaned = raw
            .replace("温度:", "")
            .replace("温度：", "")
            .replace("&nbsp;", " ")
            .trim()
        if (cleaned.isEmpty()) return "—"
        return if (cleaned.contains('°')) cleaned else "$cleaned°"
    }
    fun firstTemp(vararg ids: String): String {
        for (id in ids) {
            val value = temp(id)
            if (value != "—") return value
        }
        return "—"
    }

    return DashboardSnapshot(
        cpuName = labels["Label2"] ?: "Intel Core",
        cpuTemp = v("Simple3"),
        cpuClock = v("SIV4"),
        cpuClockBar = bar("Bar4p"),
        cpuUsage = v("SIV5"),
        cpuUsageBar = bar("Bar5p"),
        gpuTemp = firstTemp("SIV11", "Simple31"),
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
        fps = first("SIV25", "SIV23", fallback = "0"),
        fpsHistory = fpsHistory,
        gpuHistory = gpuHistory,
        ramType = first("Simple23", "Simple21"),
        ramUsed = first("Simple20", "Simple18"),
        ramUsedBar = bar("Bar15p"),
        ramFree = first("Simple21", "Simple19"),
        ramFreeBar = bar("Bar16p"),
        ramUsage = first("Simple22", "Simple20"),
        ramUsageBar = bar("Bar17p"),
        ramTemp1 = temp("SIV18"),
        ramTemp2 = temp("SIV19"),
        driveCUsage = v("SIV12"),
        driveCBar = bar("Bar12p"),
        driveCTemp = firstTemp("Simple28", "Simple26"),
        driveDUsage = v("SIV13"),
        driveDBar = bar("Bar13p"),
        driveDTemp = firstTemp("Simple29", "Simple27"),
        driveEUsage = v("SIV14"),
        driveEBar = bar("Bar14p"),
        driveETemp = firstTemp("Simple30", "Simple28"),
        date = first("Simple33", "Simple31"),
        time = first("Simple34", "Simple32"),
        upload = first("SIV35", "SIV33"),
        download = first("SIV36", "SIV34"),
        volumeBar = (this["Bar22p"]?.toFloatOrNull() ?: 0f).coerceIn(0f, 100f) / 100f,
        cpuFan = first("SIV37", "SIV35"),
        gpuFan = first("SIV38", "SIV36"),
    )
}