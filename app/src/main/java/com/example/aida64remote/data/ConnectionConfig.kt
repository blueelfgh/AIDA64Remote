package com.example.aida64remote.data

data class ConnectionConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_AIDA_PORT,
    val serviceType: ServiceType = ServiceType.Aida64,
) {
    val baseUrl: String get() = "http://$host:$port"
    val sseUrl: String get() = "$baseUrl/sse"
    val lhmSnapshotUrl: String get() = "$baseUrl/api/snapshot"
    val lhmHealthUrl: String get() = "$baseUrl/api/health"

    companion object {
        const val DEFAULT_HOST = "192.168.50.23"
        const val DEFAULT_AIDA_PORT = 35080
        const val DEFAULT_LHM_PORT = 18080
        const val DEFAULT_PORT = DEFAULT_AIDA_PORT
    }
}
