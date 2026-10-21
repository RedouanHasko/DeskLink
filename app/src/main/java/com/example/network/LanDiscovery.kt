package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.DiscoveredPc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class LanDiscovery(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "DeskLinkLanDiscovery"
    private val DISCOVERY_PORT = 8765

    private val _discoveredPcs = MutableStateFlow<List<DiscoveredPc>>(emptyList())
    val discoveredPcs: StateFlow<List<DiscoveredPc>> = _discoveredPcs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true

        acquireMulticastLock()

        scanJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 2500

                // Launch listening job
                val listenJob = launch {
                    val buffer = ByteArray(2048)
                    while (isActive) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val text = String(packet.data, 0, packet.length).trim()
                            val senderIp = packet.address.hostAddress ?: continue

                            if (text.startsWith("DESKLINK_OFFER:")) {
                                val jsonStr = text.substringAfter("DESKLINK_OFFER:")
                                val json = JSONObject(jsonStr)
                                val name = json.optString("name", "Windows PC")
                                val os = json.optString("os", "Windows 11")
                                val port = json.optInt("port", 8765)

                                val pc = DiscoveredPc(
                                    name = name,
                                    ip = senderIp,
                                    port = port,
                                    os = os
                                )
                                addDiscovered(pc)
                            }
                        } catch (_: Exception) {
                            // Timeout or read failure
                        }
                    }
                }

                // Send discovery broadcasts periodically
                val discoverMsg = "DESKLINK_DISCOVER".toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val sendPacket = DatagramPacket(discoverMsg, discoverMsg.size, broadcastAddr, DISCOVERY_PORT)

                repeat(3) {
                    if (!isActive) return@repeat
                    try {
                        socket.send(sendPacket)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send broadcast", e)
                    }
                    delay(1200)
                }

                delay(2000)
                listenJob.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}")
            } finally {
                socket?.close()
                _isScanning.value = false
                releaseMulticastLock()
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        releaseMulticastLock()
    }

    private fun addDiscovered(pc: DiscoveredPc) {
        val current = _discoveredPcs.value.toMutableList()
        val index = current.indexOfFirst { it.ip == pc.ip }
        if (index != -1) {
            current[index] = pc
        } else {
            current.add(pc)
        }
        _discoveredPcs.value = current
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("DeskLinkMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
    }
}
