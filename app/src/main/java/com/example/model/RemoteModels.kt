package com.example.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val ip: String, val port: Int) : ConnectionState
    data class Connected(
        val ip: String,
        val port: Int,
        val pcName: String = "Windows PC",
        val os: String = "Windows 11",
        val pingMs: Long = 8L
    ) : ConnectionState
    data class Error(val message: String) : ConnectionState
    data class DemoMode(
        val pcName: String = "Simulated Windows 11 Laptop",
        val pingMs: Long = 4L
    ) : ConnectionState
}

data class DiscoveredPc(
    val name: String,
    val ip: String,
    val port: Int,
    val os: String = "Windows",
    val lastSeen: Long = System.currentTimeMillis()
)

data class SavedDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 8765,
    val isFavorite: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)

data class ScreenFrameInfo(
    val byteArray: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScreenFrameInfo
        return byteArray.contentEquals(other.byteArray) && timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = byteArray.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
