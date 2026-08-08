package com.example.aida64remote.data

import com.example.aida64remote.model.ConnectionEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.math.min

class Aida64SseClient(
    private val client: OkHttpClient = defaultClient(),
) {
    fun connect(config: ConnectionConfig): Flow<ConnectionEvent> = flow {
        var attempt = 0
        var labels: Map<String, String> = emptyMap()

        while (currentCoroutineContext().isActive) {
            try {
                if (attempt == 0) {
                    emit(ConnectionEvent.Connecting)
                } else {
                    emit(ConnectionEvent.Reconnecting(attempt, null))
                }

                if (labels.isEmpty()) {
                    labels = fetchLabels(config)
                }

                val request = Request.Builder()
                    .url(config.sseUrl)
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("空响应")
                    emit(ConnectionEvent.Connected)
                    attempt = 0

                    BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)).use { reader ->
                        while (currentCoroutineContext().isActive) {
                            currentCoroutineContext().ensureActive()
                            val line = reader.readLine() ?: break
                            if (!line.startsWith("data:")) continue
                            val sensors = SensorParser.parse(line).map { item ->
                                item.copy(label = labels[item.id] ?: item.label)
                            }
                            if (sensors.isNotEmpty()) {
                                emit(ConnectionEvent.SensorsUpdated(sensors, labels))
                            }
                        }
                    }
                }

                throw IOException("SSE 连接已关闭")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempt += 1
                val message = e.message ?: e.javaClass.simpleName
                emit(ConnectionEvent.Reconnecting(attempt, message))
                val delayMs = min(15_000L, (1L shl min(attempt - 1, 4)) * 1_000L)
                delay(delayMs)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun fetchLabels(config: ConnectionConfig): Map<String, String> {
        return try {
            val request = Request.Builder()
                .url(config.baseUrl + "/")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyMap()
                val html = response.body?.string().orEmpty()
                HtmlLabelParser.parseLabels(html)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
