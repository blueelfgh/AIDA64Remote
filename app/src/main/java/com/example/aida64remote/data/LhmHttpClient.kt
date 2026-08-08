package com.example.aida64remote.data

import com.example.aida64remote.model.ConnectionEvent
import com.example.aida64remote.model.DashboardSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

class LhmHttpClient(
    private val client: OkHttpClient = defaultClient(),
    private val pollIntervalMs: Long = 1000L,
) {
    fun connect(config: ConnectionConfig): Flow<ConnectionEvent> = flow {
        var attempt = 0
        emit(ConnectionEvent.Connecting)

        while (currentCoroutineContext().isActive) {
            try {
                if (attempt > 0) {
                    emit(ConnectionEvent.Reconnecting(attempt, null))
                }
                val snapshot = fetchSnapshot(config)
                emit(ConnectionEvent.Connected)
                attempt = 0
                emit(ConnectionEvent.DashboardUpdated(snapshot))

                while (currentCoroutineContext().isActive) {
                    delay(pollIntervalMs)
                    val next = fetchSnapshot(config)
                    emit(ConnectionEvent.DashboardUpdated(next))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempt += 1
                emit(ConnectionEvent.Reconnecting(attempt, e.message ?: e.javaClass.simpleName))
                val delayMs = min(15_000L, (1L shl min(attempt - 1, 4)) * 1_000L)
                delay(delayMs)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun fetchSnapshot(config: ConnectionConfig): DashboardSnapshot {
        val request = Request.Builder()
            .url(config.lhmSnapshotUrl)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("空响应")
            return parseSnapshot(JSONObject(body))
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        fun parseSnapshot(json: JSONObject): DashboardSnapshot = DashboardSnapshot(
            cpuName = json.str("cpuName"),
            cpuTemp = json.str("cpuTemp"),
            cpuClock = json.str("cpuClock"),
            cpuClockBar = json.floatVal("cpuClockBar"),
            cpuUsage = json.str("cpuUsage"),
            cpuUsageBar = json.floatVal("cpuUsageBar"),
            gpuTemp = json.str("gpuTemp"),
            vramUsed = json.str("vramUsed"),
            vramUsedBar = json.floatVal("vramUsedBar"),
            vramFree = json.str("vramFree"),
            vramFreeBar = json.floatVal("vramFreeBar"),
            gpuClock = json.str("gpuClock"),
            gpuClockBar = json.floatVal("gpuClockBar"),
            gpuMemClock = json.str("gpuMemClock"),
            gpuMemClockBar = json.floatVal("gpuMemClockBar"),
            gpuUsage = json.str("gpuUsage"),
            gpuUsageBar = json.floatVal("gpuUsageBar"),
            gpuTempBar = json.floatVal("gpuTempBar"),
            fps = json.optString("fps", "0").ifBlank { "0" },
            ramType = json.str("ramType"),
            ramUsed = json.str("ramUsed"),
            ramUsedBar = json.floatVal("ramUsedBar"),
            ramFree = json.str("ramFree"),
            ramFreeBar = json.floatVal("ramFreeBar"),
            ramUsage = json.str("ramUsage"),
            ramUsageBar = json.floatVal("ramUsageBar"),
            boardTemp = json.str("boardTemp"),
            driveCUsage = json.str("driveCUsage"),
            driveCBar = json.floatVal("driveCBar"),
            driveCTemp = json.str("driveCTemp"),
            driveDUsage = json.str("driveDUsage"),
            driveDBar = json.floatVal("driveDBar"),
            driveDTemp = json.str("driveDTemp"),
            driveEUsage = json.str("driveEUsage"),
            driveEBar = json.floatVal("driveEBar"),
            driveETemp = json.str("driveETemp"),
            date = json.str("date"),
            time = json.str("time"),
            upload = json.str("upload"),
            download = json.str("download"),
            volumeBar = json.floatVal("volumeBar"),
            cpuFan = json.str("cpuFan"),
            gpuFan = json.str("gpuFan"),
        )

        private fun JSONObject.str(key: String): String {
            val value = optString(key, "—")
            return value.ifBlank { "—" }
        }

        private fun JSONObject.floatVal(key: String): Float =
            optDouble(key, 0.0).toFloat().coerceIn(0f, 1f)
    }
}
