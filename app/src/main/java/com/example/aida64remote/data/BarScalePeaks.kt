package com.example.aida64remote.data

import com.example.aida64remote.model.DashboardSnapshot

/** 各进度条指标曾出现过的历史最大值（持久化，可在设置中清除）。 */
data class BarScalePeaks(
    val cpuClock: Float? = null,
    val cpuUsage: Float? = null,
    val vramUsed: Float? = null,
    val vramFree: Float? = null,
    val gpuClock: Float? = null,
    val gpuMemClock: Float? = null,
    val gpuUsage: Float? = null,
    val gpuTemp: Float? = null,
    val ramUsed: Float? = null,
    val ramFree: Float? = null,
    val ramUsage: Float? = null,
    val driveC: Float? = null,
    val driveD: Float? = null,
    val driveE: Float? = null,
    val volume: Float? = null,
) {
    /** 用当前快照抬升峰值；返回更新后的峰值与是否有变化。 */
    fun absorb(dashboard: DashboardSnapshot): Pair<BarScalePeaks, Boolean> {
        var changed = false
        fun bump(old: Float?, raw: String): Float? {
            val value = parseSensorNumber(raw) ?: return old
            return if (old == null || value > old) {
                changed = true
                value
            } else {
                old
            }
        }
        val next = copy(
            cpuClock = bump(cpuClock, dashboard.cpuClock),
            cpuUsage = bump(cpuUsage, dashboard.cpuUsage),
            vramUsed = bump(vramUsed, dashboard.vramUsed),
            vramFree = bump(vramFree, dashboard.vramFree),
            gpuClock = bump(gpuClock, dashboard.gpuClock),
            gpuMemClock = bump(gpuMemClock, dashboard.gpuMemClock),
            gpuUsage = bump(gpuUsage, dashboard.gpuUsage),
            gpuTemp = bump(gpuTemp, dashboard.gpuTemp),
            ramUsed = bump(ramUsed, dashboard.ramUsed),
            ramFree = bump(ramFree, dashboard.ramFree),
            ramUsage = bump(ramUsage, dashboard.ramUsage),
            driveC = bump(driveC, dashboard.driveCUsage),
            driveD = bump(driveD, dashboard.driveDUsage),
            driveE = bump(driveE, dashboard.driveEUsage),
            volume = bump(volume, (dashboard.volumeBar * 100f).toString()),
        )
        return next to changed
    }
}

fun parseSensorNumber(raw: String): Float? {
    if (raw.isBlank() || raw == "—") return null
    val number = buildString {
        var seenDot = false
        for (ch in raw) {
            when {
                ch.isDigit() -> append(ch)
                ch == '.' && !seenDot -> {
                    append(ch)
                    seenDot = true
                }
                ch == '-' && isEmpty() -> append(ch)
            }
        }
    }
    return number.toFloatOrNull()
}
