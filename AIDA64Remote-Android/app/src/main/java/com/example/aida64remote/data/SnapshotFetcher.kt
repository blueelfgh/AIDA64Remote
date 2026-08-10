package com.example.aida64remote.data

import com.example.aida64remote.model.DashboardSnapshot
import com.example.aida64remote.model.toDashboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * One-shot sensor fetch for widget / background use.
 */
class SnapshotFetcher(
    private val lhmClient: OkHttpClient = LhmHttpClient.defaultClient(),
    private val aidaClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) {
    suspend fun fetch(config: ConnectionConfig): DashboardSnapshot = withContext(Dispatchers.IO) {
        when (config.serviceType) {
            ServiceType.LibreHardwareMonitor -> fetchLhm(config)
            ServiceType.Aida64 -> withTimeout(8_000L) { fetchAidaOnce(config) }
        }
    }

    private fun fetchLhm(config: ConnectionConfig): DashboardSnapshot {
        val request = Request.Builder()
            .url(config.lhmSnapshotUrl)
            .header("Accept", "application/json")
            .get()
            .build()
        lhmClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("空响应")
            return LhmHttpClient.parseSnapshot(JSONObject(body))
        }
    }

    private fun fetchAidaOnce(config: ConnectionConfig): DashboardSnapshot {
        val labels = fetchAidaLabels(config)
        val request = Request.Builder()
            .url(config.sseUrl)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        aidaClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("空响应")
            BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val sensors = SensorParser.parse(line).map { item ->
                        item.copy(label = labels[item.id] ?: item.label)
                    }
                    if (sensors.isEmpty()) continue
                    return SensorParser.toMap(sensors).toDashboard(
                        labels = labels,
                        fpsHistory = emptyList(),
                        gpuHistory = emptyList(),
                    )
                }
            }
        }
        throw IOException("未收到传感器数据")
    }

    private fun fetchAidaLabels(config: ConnectionConfig): Map<String, String> {
        return try {
            val request = Request.Builder()
                .url(config.baseUrl + "/")
                .get()
                .build()
            aidaClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyMap()
                val html = response.body?.string().orEmpty()
                HtmlLabelParser.parseLabels(html)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
