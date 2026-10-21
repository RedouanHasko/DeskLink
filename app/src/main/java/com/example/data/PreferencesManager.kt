package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SavedDevice
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("desklink_prefs", Context.MODE_PRIVATE)

    var mouseSensitivity: Float
        get() = prefs.getFloat("mouse_sensitivity", 1.2f)
        set(value) = prefs.edit().putFloat("mouse_sensitivity", value).apply()

    var scrollSensitivity: Float
        get() = prefs.getFloat("scroll_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("scroll_sensitivity", value).apply()

    var invertScroll: Boolean
        get() = prefs.getBoolean("invert_scroll", false)
        set(value) = prefs.edit().putBoolean("invert_scroll", value).apply()

    var tapToClick: Boolean
        get() = prefs.getBoolean("tap_to_click", true)
        set(value) = prefs.edit().putBoolean("tap_to_click", value).apply()

    var twoFingerRightClick: Boolean
        get() = prefs.getBoolean("two_finger_right_click", true)
        set(value) = prefs.edit().putBoolean("two_finger_right_click", value).apply()

    var edgeScrollEnabled: Boolean
        get() = prefs.getBoolean("edge_scroll_enabled", true)
        set(value) = prefs.edit().putBoolean("edge_scroll_enabled", value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    var autoClipboardSync: Boolean
        get() = prefs.getBoolean("auto_clipboard_sync", false)
        set(value) = prefs.edit().putBoolean("auto_clipboard_sync", value).apply()

    var autoOpenKeyboardOnTextField: Boolean
        get() = prefs.getBoolean("auto_open_keyboard", true)
        set(value) = prefs.edit().putBoolean("auto_open_keyboard", value).apply()

    var autoOpenKeyboardOnClick: Boolean
        get() = prefs.getBoolean("auto_open_keyboard_click", false)
        set(value) = prefs.edit().putBoolean("auto_open_keyboard_click", value).apply()

    var defaultScreenQuality: String
        get() = prefs.getString("default_screen_quality", "Balanced") ?: "Balanced"
        set(value) = prefs.edit().putString("default_screen_quality", value).apply()

    var lastIp: String
        get() = prefs.getString("last_ip", "192.168.1.100") ?: "192.168.1.100"
        set(value) = prefs.edit().putString("last_ip", value).apply()

    var lastPort: Int
        get() = prefs.getInt("last_port", 8765)
        set(value) = prefs.edit().putInt("last_port", value).apply()

    fun resetToDefaults() {
        prefs.edit()
            .putFloat("mouse_sensitivity", 1.2f)
            .putFloat("scroll_sensitivity", 1.0f)
            .putBoolean("invert_scroll", false)
            .putBoolean("tap_to_click", true)
            .putBoolean("two_finger_right_click", true)
            .putBoolean("edge_scroll_enabled", true)
            .putBoolean("haptic_feedback", true)
            .putBoolean("auto_clipboard_sync", false)
            .putBoolean("auto_open_keyboard", true)
            .putBoolean("auto_open_keyboard_click", false)
            .putString("default_screen_quality", "Balanced")
            .apply()
    }

    fun getSavedDevices(): List<SavedDevice> {
        val jsonStr = prefs.getString("saved_devices", null) ?: return defaultDevices()
        val list = mutableListOf<SavedDevice>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedDevice(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        ip = obj.getString("ip"),
                        port = obj.getInt("port"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        lastConnected = obj.optLong("lastConnected", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
            return defaultDevices()
        }
        return list.ifEmpty { defaultDevices() }
    }

    fun saveDevice(device: SavedDevice) {
        val current = getSavedDevices().toMutableList()
        val index = current.indexOfFirst { it.id == device.id || it.ip == device.ip }
        if (index != -1) {
            current[index] = device
        } else {
            current.add(0, device)
        }
        persistDevices(current)
    }

    fun deleteDevice(deviceId: String) {
        val current = getSavedDevices().filter { it.id != deviceId }
        persistDevices(current)
    }

    private fun persistDevices(list: List<SavedDevice>) {
        val array = JSONArray()
        for (d in list) {
            val obj = JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("ip", d.ip)
                put("port", d.port)
                put("isFavorite", d.isFavorite)
                put("lastConnected", d.lastConnected)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_devices", array.toString()).apply()
    }

    private fun defaultDevices(): List<SavedDevice> {
        return listOf(
            SavedDevice(
                id = "dev_default_1",
                name = "My Windows Laptop",
                ip = "192.168.1.100",
                port = 8765,
                isFavorite = true
            )
        )
    }
}
