package com.example.aida64remote.data

data class ConnectionConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
) {
    val baseUrl: String get() = "http://$host:$port"
    val sseUrl: String get() = "$baseUrl/sse"

    companion object {
        const val DEFAULT_HOST = "192.168.50.23"
        const val DEFAULT_PORT = 35080
    }
}
