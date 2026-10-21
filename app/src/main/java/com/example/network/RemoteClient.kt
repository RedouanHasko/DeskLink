package com.example.network

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class RemoteClient(
    private val scope: CoroutineScope
) {
    private val TAG = "DeskLinkClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive
        .writeTimeout(4, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var currentWebSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var isManualDisconnect = false
    private var currentHost = ""
    private var currentPort = 8765

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _screenFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 2)
    val screenFrames: SharedFlow<ByteArray> = _screenFrames.asSharedFlow()

    private val _clipboardUpdates = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val clipboardUpdates: SharedFlow<String> = _clipboardUpdates.asSharedFlow()

    private val _actionLog = MutableStateFlow<String>("Ready to connect")
    val actionLog: StateFlow<String> = _actionLog.asStateFlow()

    private val _isTextFieldFocused = MutableStateFlow(false)
    val isTextFieldFocused: StateFlow<Boolean> = _isTextFieldFocused.asStateFlow()

    // Virtual cursor coordinates for demo mode
    val demoCursorX = MutableStateFlow(0.5f)
    val demoCursorY = MutableStateFlow(0.5f)
    val demoLastKey = MutableStateFlow("")

    fun connect(ip: String, port: Int = 8765) {
        disconnect()
        isManualDisconnect = false
        currentHost = ip
        currentPort = port

        _connectionState.value = ConnectionState.Connecting(ip, port)
        _actionLog.value = "Connecting to ws://$ip:$port..."

        val url = "ws://$ip:$port"
        val request = Request.Builder().url(url).build()

        currentWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened to $ip:$port")
                _connectionState.value = ConnectionState.Connected(
                    ip = ip,
                    port = port,
                    pcName = "Windows PC ($ip)",
                    os = "Windows 10/11",
                    pingMs = 12L
                )
                _actionLog.value = "Connected to $ip:$port"
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                if (data.isNotEmpty()) {
                    // Header byte 1 = JPEG stream
                    if (data[0] == 1.toByte() && data.size > 1) {
                        val jpegBytes = data.copyOfRange(1, data.size)
                        _screenFrames.tryEmit(jpegBytes)
                    } else {
                        _screenFrames.tryEmit(data)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed")
                stopPingLoop()
                if (!isManualDisconnect) {
                    _connectionState.value = ConnectionState.Disconnected
                    _actionLog.value = "Disconnected from server"
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                stopPingLoop()
                _connectionState.value = ConnectionState.Error(
                    t.message ?: "Failed to connect to $ip:$port. Ensure server is running."
                )
                _actionLog.value = "Connection error: ${t.localizedMessage}"
            }
        })
    }

    fun startDemoMode() {
        disconnect()
        _connectionState.value = ConnectionState.DemoMode()
        _actionLog.value = "Demo Mode: Connected to Simulated PC"
        _latencyMs.value = 4L
        startDemoScreenEmitter()
    }

    private var demoScreenJob: Job? = null
    private fun startDemoScreenEmitter() {
        demoScreenJob?.cancel()
        demoScreenJob = scope.launch(Dispatchers.Default) {
            val width = 640
            val height = 360
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
            val taskbarPaint = Paint().apply { color = Color.rgb(30, 41, 59) }
            val windowPaint = Paint().apply { color = Color.rgb(33, 50, 77) }
            val textPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                textSize = 28f
                isAntiAlias = true
            }
            val accentPaint = Paint().apply {
                color = Color.rgb(0, 229, 255)
                textSize = 20f
                isAntiAlias = true
            }

            while (isActive && _connectionState.value is ConnectionState.DemoMode) {
                // Draw simulated desktop
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                // Simulated app window
                canvas.drawRoundRect(60f, 40f, 580f, 290f, 16f, 16f, windowPaint)
                canvas.drawText("Windows 11 Desktop (Live Stream Preview)", 80f, 90f, textPaint)
                canvas.drawText("Move trackpad or press keys to test control!", 80f, 140f, textPaint)
                canvas.drawText("Last Key: ${demoLastKey.value.ifEmpty { "None" }}", 80f, 190f, accentPaint)
                canvas.drawText("Cursor: (${(demoCursorX.value * 1920).toInt()}, ${(demoCursorY.value * 1080).toInt()})", 80f, 230f, textPaint)

                // Simulated taskbar
                canvas.drawRect(0f, (height - 40).toFloat(), width.toFloat(), height.toFloat(), taskbarPaint)
                // Simulated start icon
                canvas.drawCircle(40f, (height - 20).toFloat(), 12f, accentPaint)

                // Draw cursor
                val cx = demoCursorX.value * width
                val cy = demoCursorY.value * height
                val cursorPaint = Paint().apply { color = Color.CYAN }
                canvas.drawCircle(cx, cy, 7f, cursorPaint)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                _screenFrames.tryEmit(stream.toByteArray())

                delay(100) // 10 fps for demo preview
            }
        }
    }

    private fun handleTextMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "server_info" -> {
                    val pcName = json.optString("name", "Windows PC")
                    val os = json.optString("os", "Windows 11")
                    val current = _connectionState.value
                    if (current is ConnectionState.Connected) {
                        _connectionState.value = current.copy(pcName = pcName, os = os)
                    }
                }
                "pong" -> {
                    val sentTime = json.optLong("timestamp", 0)
                    if (sentTime > 0) {
                        val latency = (System.currentTimeMillis() - sentTime).coerceAtLeast(1)
                        _latencyMs.value = latency
                        val current = _connectionState.value
                        if (current is ConnectionState.Connected) {
                            _connectionState.value = current.copy(pingMs = latency)
                        }
                    }
                }
                "clipboard_update" -> {
                    val clip = json.optString("text", "")
                    _clipboardUpdates.tryEmit(clip)
                }
                "input_focus" -> {
                    val isText = json.optBoolean("is_text_field", false)
                    _isTextFieldFocused.value = isText
                    if (isText) {
                        _actionLog.value = "PC Text Field Active"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: $text", e)
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                sendPing()
                delay(3000)
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    fun checkInputFocus() {
        sendJson(JSONObject().apply {
            put("type", "check_focus")
        })
    }

    fun disconnect() {
        isManualDisconnect = true
        stopPingLoop()
        demoScreenJob?.cancel()
        try {
            currentWebSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        currentWebSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _latencyMs.value = 0L
        _isTextFieldFocused.value = false
        _actionLog.value = "Disconnected"
    }

    // --- Remote Control Actions ---

    fun sendMouseMove(dx: Float, dy: Float) {
        if (_connectionState.value is ConnectionState.DemoMode) {
            val nx = (demoCursorX.value + dx * 0.002f).coerceIn(0f, 1f)
            val ny = (demoCursorY.value + dy * 0.002f).coerceIn(0f, 1f)
            demoCursorX.value = nx
            demoCursorY.value = ny
            return
        }
        val json = JSONObject().apply {
            put("type", "mouse_move")
            put("dx", dx.toDouble())
            put("dy", dy.toDouble())
        }
        sendJson(json)
    }

    fun sendMouseMoveToNorm(normX: Float, normY: Float) {
        if (_connectionState.value is ConnectionState.DemoMode) {
            demoCursorX.value = normX.coerceIn(0f, 1f)
            demoCursorY.value = normY.coerceIn(0f, 1f)
            return
        }
        val json = JSONObject().apply {
            put("type", "mouse_move_to_norm")
            put("x", normX.toDouble())
            put("y", normY.toDouble())
        }
        sendJson(json)
    }

    fun sendMouseClick(button: String = "left", action: String = "click") {
        _actionLog.value = "Mouse: $button $action"
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "mouse_click")
            put("button", button)
            put("action", action)
        }
        sendJson(json)
    }

    fun sendMouseScroll(dx: Float = 0f, dy: Float) {
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "mouse_scroll")
            put("dx", dx.toDouble())
            put("dy", dy.toDouble())
        }
        sendJson(json)
    }

    fun sendKeyPress(key: String) {
        _actionLog.value = "Key: $key"
        demoLastKey.value = key
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "key_press")
            put("key", key)
        }
        sendJson(json)
    }

    fun sendKeyDown(key: String) {
        _actionLog.value = "KeyDown: $key"
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "key_down")
            put("key", key)
        }
        sendJson(json)
    }

    fun sendKeyUp(key: String) {
        _actionLog.value = "KeyUp: $key"
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "key_up")
            put("key", key)
        }
        sendJson(json)
    }

    fun sendHotkey(keys: List<String>) {
        _actionLog.value = "Hotkey: ${keys.joinToString("+")}"
        demoLastKey.value = keys.joinToString("+")
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "hotkey")
            val arr = JSONArray()
            keys.forEach { arr.put(it) }
            put("keys", arr)
        }
        sendJson(json)
    }

    fun sendTypeText(text: String) {
        _actionLog.value = "Type text: \"$text\""
        demoLastKey.value = text
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "type_text")
            put("text", text)
        }
        sendJson(json)
    }

    fun sendMediaAction(action: String) {
        _actionLog.value = "Media: $action"
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "media")
            put("action", action)
        }
        sendJson(json)
    }

    fun sendSystemPower(action: String) {
        _actionLog.value = "Power: $action"
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "system_power")
            put("action", action)
        }
        sendJson(json)
    }

    fun sendClipboardSet(text: String) {
        _actionLog.value = "Set PC clipboard (${text.length} chars)"
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", "clipboard_set")
            put("text", text)
        }
        sendJson(json)
    }

    fun requestClipboardGet() {
        if (_connectionState.value is ConnectionState.DemoMode) {
            _clipboardUpdates.tryEmit("Demo simulated PC clipboard text")
            return
        }
        val json = JSONObject().apply {
            put("type", "clipboard_get")
        }
        sendJson(json)
    }

    fun requestScreenStream(start: Boolean, quality: Int = 55, scale: Float = 0.5f) {
        if (_connectionState.value is ConnectionState.DemoMode) return
        val json = JSONObject().apply {
            put("type", if (start) "screen_request" else "screen_stop")
            put("quality", quality)
            put("scale", scale.toDouble())
        }
        sendJson(json)
    }

    private fun sendPing() {
        if (_connectionState.value is ConnectionState.DemoMode) {
            _latencyMs.value = (3..7).random().toLong()
            return
        }
        val json = JSONObject().apply {
            put("type", "ping")
            put("timestamp", System.currentTimeMillis())
        }
        sendJson(json)
    }

    private fun sendJson(json: JSONObject) {
        val str = json.toString()
        currentWebSocket?.send(str)
    }
}
