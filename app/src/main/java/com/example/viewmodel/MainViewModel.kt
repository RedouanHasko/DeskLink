package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.model.ConnectionState
import com.example.model.DiscoveredPc
import com.example.model.SavedDevice
import com.example.network.AudioStreamer
import com.example.network.BluetoothRemoteManager
import com.example.network.LanDiscovery
import com.example.network.RemoteClient
import com.example.network.VoiceDictationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab(val title: String) {
    TRACKPAD("Trackpad"),
    KEYBOARD("Keyboard"),
    SCREEN("Screen"),
    VOICE_MIC("Voice & Mic"),
    CLIPBOARD("Clipboard"),
    CONNECT("Connect")
}

enum class TransportMode {
    WIFI,
    BLUETOOTH
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val prefs = PreferencesManager(context)
    val client = RemoteClient(viewModelScope)
    val lanDiscovery = LanDiscovery(context, viewModelScope)
    val bluetoothManager = BluetoothRemoteManager(context, viewModelScope)

    // Splash start animation visibility
    val showSplashScreen = MutableStateFlow(true)

    // Active Transport Mode: Wi-Fi vs Bluetooth
    val transportMode = MutableStateFlow(TransportMode.WIFI)

    // Current Tab
    private val _currentTab = MutableStateFlow(AppTab.TRACKPAD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Screen Stream Bitmap
    private val _currentScreenBitmap = MutableStateFlow<Bitmap?>(null)
    val currentScreenBitmap: StateFlow<Bitmap?> = _currentScreenBitmap.asStateFlow()

    // Screen quality settings
    private val _screenQuality = MutableStateFlow(55)
    val screenQuality: StateFlow<Int> = _screenQuality.asStateFlow()

    private val _screenScale = MutableStateFlow(0.5f)
    val screenScale: StateFlow<Float> = _screenScale.asStateFlow()

    private val _isScreenStreamingActive = MutableStateFlow(false)
    val isScreenStreamingActive: StateFlow<Boolean> = _isScreenStreamingActive.asStateFlow()

    // Modifier keys state
    val isCtrlActive = MutableStateFlow(false)
    val isAltActive = MutableStateFlow(false)
    val isShiftActive = MutableStateFlow(false)
    val isWinActive = MutableStateFlow(false)
    val isDragLockActive = MutableStateFlow(false)

    // Clipboard
    private val _phoneClipboardText = MutableStateFlow("")
    val phoneClipboardText: StateFlow<String> = _phoneClipboardText.asStateFlow()

    private val _pcClipboardText = MutableStateFlow("")
    val pcClipboardText: StateFlow<String> = _pcClipboardText.asStateFlow()

    private val _clipboardHistory = MutableStateFlow<List<String>>(emptyList())
    val clipboardHistory: StateFlow<List<String>> = _clipboardHistory.asStateFlow()

    // Saved devices
    private val _savedDevices = MutableStateFlow<List<SavedDevice>>(prefs.getSavedDevices())
    val savedDevices: StateFlow<List<SavedDevice>> = _savedDevices.asStateFlow()

    // Dialog states
    private val _showServerDialog = MutableStateFlow(false)
    val showServerDialog: StateFlow<Boolean> = _showServerDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // Screen Mirror Fullscreen State
    private val _isMirrorFullScreen = MutableStateFlow(false)
    val isMirrorFullScreen: StateFlow<Boolean> = _isMirrorFullScreen.asStateFlow()

    // Floating Soft Keyboard Bar state
    private val _showFloatingKeyboard = MutableStateFlow(false)
    val showFloatingKeyboard: StateFlow<Boolean> = _showFloatingKeyboard.asStateFlow()

    val isPcTextFieldActive: StateFlow<Boolean> = client.isTextFieldFocused

    // Unified Connection State combining Wi-Fi and Bluetooth
    val effectiveConnectionState: StateFlow<ConnectionState> = combine(
        transportMode,
        client.connectionState,
        bluetoothManager.connectionState
    ) { mode, wifiState, btState ->
        if (mode == TransportMode.BLUETOOTH) btState else wifiState
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    // Haptics
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Voice Dictation
    val voiceDictationManager = VoiceDictationManager(context) { transcribedText ->
        if (transcribedText.isNotBlank()) {
            sendCustomText("$transcribedText ")
            triggerHaptic(40)
        }
    }

    // Audio mic streamer
    val audioStreamer = AudioStreamer(viewModelScope) { audioChunk ->
        // Chunks stream over connection
    }

    init {
        // Collect screen frames
        viewModelScope.launch(Dispatchers.Default) {
            client.screenFrames.collect { bytes ->
                try {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        _currentScreenBitmap.value = bmp
                    }
                } catch (_: Exception) {}
            }
        }

        // Collect clipboard updates from PC
        viewModelScope.launch {
            client.clipboardUpdates.collect { text ->
                if (text.isNotBlank()) {
                    _pcClipboardText.value = text
                    addToClipboardHistory(text)
                }
            }
        }

        // Collect PC text field focus events to auto-open keyboard
        viewModelScope.launch {
            client.isTextFieldFocused.collect { isFocused ->
                if (isFocused && prefs.autoOpenKeyboardOnTextField) {
                    _showFloatingKeyboard.value = true
                    triggerHaptic(30)
                }
            }
        }

        // Initial scan for PCs on local network
        lanDiscovery.startScan()
        refreshPhoneClipboard()
    }

    fun toggleFloatingKeyboard() {
        triggerHaptic(20)
        _showFloatingKeyboard.value = !_showFloatingKeyboard.value
    }

    fun openFloatingKeyboard() {
        triggerHaptic(20)
        _showFloatingKeyboard.value = true
    }

    fun closeFloatingKeyboard() {
        _showFloatingKeyboard.value = false
    }

    fun onTrackpadTapToType(isDoubleTap: Boolean = false) {
        if (prefs.autoOpenKeyboardOnClick || isDoubleTap) {
            openFloatingKeyboard()
        }
    }

    fun sendDirectChar(text: String) {
        if (text.isNotEmpty()) {
            triggerHaptic(12)
            if (transportMode.value == TransportMode.BLUETOOTH) {
                bluetoothManager.sendTypeText(text)
            } else {
                client.sendTypeText(text)
            }
        }
    }

    fun sendBackspace() {
        sendKeyPress("backspace")
    }

    fun sendEnter() {
        sendKeyPress("enter")
    }

    fun sendTab() {
        sendKeyPress("tab")
    }

    fun sendSpace() {
        sendKeyPress("space")
    }

    fun dismissSplash() {
        showSplashScreen.value = false
    }

    fun setTransportMode(mode: TransportMode) {
        transportMode.value = mode
        if (mode == TransportMode.BLUETOOTH) {
            bluetoothManager.refreshPairedDevices()
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab != AppTab.SCREEN && _isMirrorFullScreen.value) {
            _isMirrorFullScreen.value = false
        }
        if (tab == AppTab.SCREEN && !_isScreenStreamingActive.value) {
            startScreenStream()
        }
    }

    fun setMirrorFullScreen(fullScreen: Boolean) {
        triggerHaptic(20)
        _isMirrorFullScreen.value = fullScreen
    }

    fun toggleMirrorFullScreen() {
        triggerHaptic(20)
        _isMirrorFullScreen.value = !_isMirrorFullScreen.value
    }

    fun setShowServerDialog(show: Boolean) {
        _showServerDialog.value = show
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun connectTo(ip: String, port: Int = 8765, name: String = "Windows PC") {
        transportMode.value = TransportMode.WIFI
        prefs.lastIp = ip
        prefs.lastPort = port
        val dev = SavedDevice(
            id = UUID.nameUUIDFromBytes(ip.toByteArray()).toString(),
            name = name,
            ip = ip,
            port = port,
            lastConnected = System.currentTimeMillis()
        )
        prefs.saveDevice(dev)
        _savedDevices.value = prefs.getSavedDevices()
        client.connect(ip, port)
    }

    fun connectBluetooth(address: String, name: String) {
        transportMode.value = TransportMode.BLUETOOTH
        bluetoothManager.connect(address, name)
    }

    fun connectToSaved(device: SavedDevice) {
        connectTo(device.ip, device.port, device.name)
    }

    fun startDemoMode() {
        transportMode.value = TransportMode.WIFI
        client.startDemoMode()
    }

    fun disconnect() {
        stopScreenStream()
        client.disconnect()
        bluetoothManager.disconnect()
    }

    // --- Trackpad and Mouse Actions ---

    fun onTrackpadMove(dx: Float, dy: Float) {
        val sens = prefs.mouseSensitivity
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendMouseMove(dx * sens, dy * sens)
        } else {
            client.sendMouseMove(dx * sens, dy * sens)
        }
    }

    fun onLeftClick() {
        triggerHaptic(25)
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendMouseClick("left", "click")
        } else {
            client.sendMouseClick("left", "click")
        }
    }

    fun onRightClick() {
        triggerHaptic(45)
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendMouseClick("right", "click")
        } else {
            client.sendMouseClick("right", "click")
        }
    }

    fun onMiddleClick() {
        triggerHaptic(35)
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendMouseClick("middle", "click")
        } else {
            client.sendMouseClick("middle", "click")
        }
    }

    fun onScroll(dy: Float) {
        val sens = prefs.scrollSensitivity
        val direction = if (prefs.invertScroll) 1f else -1f
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendMouseScroll(dy = dy * direction * sens)
        } else {
            client.sendMouseScroll(dy = dy * direction * sens)
        }
    }

    fun toggleDragLock() {
        triggerHaptic(30)
        val newState = !isDragLockActive.value
        isDragLockActive.value = newState
        val action = if (newState) "down" else "up"
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendMouseClick("left", action)
        } else {
            client.sendMouseClick("left", action)
        }
    }

    // --- Multi-Finger Windows Gestures ---

    fun onThreeFingerSwipeUp() {
        triggerHaptic(35)
        sendHotkey(listOf("win", "tab"))
    }

    fun onThreeFingerSwipeDown() {
        triggerHaptic(35)
        sendHotkey(listOf("win", "d"))
    }

    fun onThreeFingerSwipeLeft() {
        triggerHaptic(30)
        sendHotkey(listOf("alt", "tab"))
    }

    fun onThreeFingerSwipeRight() {
        triggerHaptic(30)
        sendHotkey(listOf("alt", "shift", "tab"))
    }

    fun onPinchZoom(zoomIn: Boolean) {
        triggerHaptic(15)
        sendHotkey(listOf("ctrl", if (zoomIn) "add" else "subtract"))
    }

    fun onFourFingerTap() {
        triggerHaptic(40)
        sendHotkey(listOf("win", "a"))
    }

    // --- Keyboard Actions ---

    fun sendKeyPress(key: String) {
        triggerHaptic(20)
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendKeyPress(key)
        } else {
            client.sendKeyPress(key)
        }
    }

    fun sendHotkey(keys: List<String>) {
        triggerHaptic(35)
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendHotkey(keys)
        } else {
            client.sendHotkey(keys)
        }
    }

    fun sendCustomText(text: String) {
        if (text.isNotEmpty()) {
            triggerHaptic(20)
            if (transportMode.value == TransportMode.BLUETOOTH) {
                bluetoothManager.sendTypeText(text)
            } else {
                client.sendTypeText(text)
            }
        }
    }

    fun toggleModifier(mod: String) {
        triggerHaptic(25)
        val targetMod = mod.lowercase()
        when (targetMod) {
            "ctrl" -> {
                val state = !isCtrlActive.value
                isCtrlActive.value = state
                if (state) sendKeyDown("ctrl") else sendKeyUp("ctrl")
            }
            "alt" -> {
                val state = !isAltActive.value
                isAltActive.value = state
                if (state) sendKeyDown("alt") else sendKeyUp("alt")
            }
            "shift" -> {
                val state = !isShiftActive.value
                isShiftActive.value = state
                if (state) sendKeyDown("shift") else sendKeyUp("shift")
            }
            "win" -> {
                val state = !isWinActive.value
                isWinActive.value = state
                if (state) sendKeyDown("win") else sendKeyUp("win")
            }
        }
    }

    private fun sendKeyDown(key: String) {
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendKeyDown(key)
        } else {
            client.sendKeyDown(key)
        }
    }

    private fun sendKeyUp(key: String) {
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.sendKeyUp(key)
        } else {
            client.sendKeyUp(key)
        }
    }

    fun resetModifiers() {
        if (isCtrlActive.value) { sendKeyUp("ctrl"); isCtrlActive.value = false }
        if (isAltActive.value) { sendKeyUp("alt"); isAltActive.value = false }
        if (isShiftActive.value) { sendKeyUp("shift"); isShiftActive.value = false }
        if (isWinActive.value) { sendKeyUp("win"); isWinActive.value = false }
    }

    // --- Screen Streaming ---

    fun startScreenStream() {
        _isScreenStreamingActive.value = true
        client.requestScreenStream(true, _screenQuality.value, _screenScale.value)
    }

    fun stopScreenStream() {
        _isScreenStreamingActive.value = false
        client.requestScreenStream(false)
    }

    fun setScreenQuality(quality: Int, scale: Float) {
        _screenQuality.value = quality
        _screenScale.value = scale
        if (_isScreenStreamingActive.value) {
            client.requestScreenStream(true, quality, scale)
        }
    }

    // --- Clipboard Sync ---

    fun refreshPhoneClipboard() {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            _phoneClipboardText.value = clip
        } catch (_: Exception) {}
    }

    fun sendPhoneClipboardToPc() {
        refreshPhoneClipboard()
        val text = _phoneClipboardText.value
        if (text.isNotBlank()) {
            triggerHaptic(30)
            if (transportMode.value == TransportMode.BLUETOOTH) {
                bluetoothManager.sendClipboardSet(text)
            } else {
                client.sendClipboardSet(text)
            }
            addToClipboardHistory(text)
        }
    }

    fun requestPcClipboard() {
        triggerHaptic(20)
        if (transportMode.value == TransportMode.BLUETOOTH) {
            bluetoothManager.requestClipboardGet()
        } else {
            client.requestClipboardGet()
        }
    }

    fun copyPcClipboardToPhone(text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("DeskLink", text)
            cm?.setPrimaryClip(clip)
            _phoneClipboardText.value = text
            triggerHaptic(30)
        } catch (_: Exception) {}
    }

    private fun addToClipboardHistory(item: String) {
        val cur = _clipboardHistory.value.toMutableList()
        cur.remove(item)
        cur.add(0, item)
        _clipboardHistory.value = cur.take(15)
    }

    fun triggerHaptic(durationMs: Long = 20) {
        if (!prefs.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun deleteSavedDevice(id: String) {
        prefs.deleteDevice(id)
        _savedDevices.value = prefs.getSavedDevices()
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
        bluetoothManager.disconnect()
        lanDiscovery.stopScan()
        voiceDictationManager.destroy()
        audioStreamer.stopStreaming()
    }
}
