package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class BtDeviceItem(
    val name: String,
    val address: String,
    val isBonded: Boolean = true
)

class BluetoothRemoteManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "DeskLinkBluetooth"
    // Standard RFCOMM Serial Port Profile (SPP) UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null

    private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BtDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BtDeviceItem>> = _pairedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    init {
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _pairedDevices.value = emptyList()
            return
        }

        try {
            val bonded = bluetoothAdapter.bondedDevices
            val list = bonded?.map { device ->
                BtDeviceItem(
                    name = device.name ?: "Unknown PC (${device.address})",
                    address = device.address,
                    isBonded = true
                )
            } ?: emptyList()
            _pairedDevices.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paired Bluetooth devices", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(macAddress: String, deviceName: String = "Windows PC (Bluetooth)") {
        disconnect()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is not enabled on this device")
            return
        }

        _connectionState.value = ConnectionState.Connecting(macAddress, 0)

        scope.launch(Dispatchers.IO) {
            try {
                val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
                bluetoothAdapter.cancelDiscovery()

                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()

                socket = s
                outputStream = s.outputStream
                inputStream = s.inputStream

                _connectionState.value = ConnectionState.Connected(
                    ip = macAddress,
                    port = 0,
                    pcName = deviceName,
                    os = "Windows (Bluetooth SPP)",
                    pingMs = 9L
                )

                // Start reader loop
                readJob = launch {
                    val buffer = ByteArray(1024)
                    while (isActive) {
                        try {
                            val bytesRead = inputStream?.read(buffer) ?: -1
                            if (bytesRead > 0) {
                                val text = String(buffer, 0, bytesRead)
                                _incomingMessages.tryEmit(text)
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth connection failed: ${e.message}", e)
                _connectionState.value = ConnectionState.Error(
                    "Bluetooth pairing failed: ${e.localizedMessage ?: "Ensure companion server is listening on Bluetooth"}"
                )
                disconnect()
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendJson(json: JSONObject) {
        val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
        scope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(payload)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth send error: ${e.message}")
            }
        }
    }

    // Remote actions over Bluetooth
    fun sendMouseMove(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "mouse_move")
            put("dx", dx.toDouble())
            put("dy", dy.toDouble())
        }
        sendJson(json)
    }

    fun sendMouseClick(button: String = "left", action: String = "click") {
        val json = JSONObject().apply {
            put("type", "mouse_click")
            put("button", button)
            put("action", action)
        }
        sendJson(json)
    }

    fun sendMouseScroll(dx: Float = 0f, dy: Float) {
        val json = JSONObject().apply {
            put("type", "mouse_scroll")
            put("dx", dx.toDouble())
            put("dy", dy.toDouble())
        }
        sendJson(json)
    }

    fun sendKeyPress(key: String) {
        val json = JSONObject().apply {
            put("type", "key_press")
            put("key", key)
        }
        sendJson(json)
    }

    fun sendKeyDown(key: String) {
        val json = JSONObject().apply {
            put("type", "key_down")
            put("key", key)
        }
        sendJson(json)
    }

    fun sendKeyUp(key: String) {
        val json = JSONObject().apply {
            put("type", "key_up")
            put("key", key)
        }
        sendJson(json)
    }

    fun sendHotkey(keys: List<String>) {
        val json = JSONObject().apply {
            put("type", "hotkey")
            val arr = JSONArray()
            keys.forEach { arr.put(it) }
            put("keys", arr)
        }
        sendJson(json)
    }

    fun sendTypeText(text: String) {
        val json = JSONObject().apply {
            put("type", "type_text")
            put("text", text)
        }
        sendJson(json)
    }

    fun sendMediaAction(action: String) {
        val json = JSONObject().apply {
            put("type", "media")
            put("action", action)
        }
        sendJson(json)
    }

    fun sendClipboardSet(text: String) {
        val json = JSONObject().apply {
            put("type", "clipboard_set")
            put("text", text)
        }
        sendJson(json)
    }

    fun requestClipboardGet() {
        val json = JSONObject().apply {
            put("type", "clipboard_get")
        }
        sendJson(json)
    }
}
