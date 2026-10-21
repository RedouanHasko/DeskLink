package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TechAmber
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TechRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenServerScript: () -> Unit
) {
    val context = LocalContext.current

    // Local mutable state reflecting preferences
    var mouseSens by remember { mutableFloatStateOf(viewModel.prefs.mouseSensitivity) }
    var scrollSens by remember { mutableFloatStateOf(viewModel.prefs.scrollSensitivity) }
    var invertScroll by remember { mutableStateOf(viewModel.prefs.invertScroll) }
    var tapToClick by remember { mutableStateOf(viewModel.prefs.tapToClick) }
    var twoFingerRightClick by remember { mutableStateOf(viewModel.prefs.twoFingerRightClick) }
    var haptics by remember { mutableStateOf(viewModel.prefs.hapticFeedback) }
    var autoClip by remember { mutableStateOf(viewModel.prefs.autoClipboardSync) }
    var autoOpenKeyboard by remember { mutableStateOf(viewModel.prefs.autoOpenKeyboardOnTextField) }
    var autoOpenKeyboardClick by remember { mutableStateOf(viewModel.prefs.autoOpenKeyboardOnClick) }

    // Section expansion toggles
    var showFaqFirewall by remember { mutableStateOf(false) }
    var showFaqHotspot by remember { mutableStateOf(false) }
    var showFaqIp by remember { mutableStateOf(false) }
    var showFaqKeyboard by remember { mutableStateOf(true) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Setup Guide, 1 = App Controls, 2 = Troubleshooting

    fun copyCommand(cmd: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Command", cmd)
        cm?.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: $cmd", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .testTag("settings_dialog"),
            color = SlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Settings & Setup Guide",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instructions & Customization",
                                color = CyanPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateCard)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingTabChip("Setup Guide", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) {
                        selectedTab = 0
                    }
                    SettingTabChip("Controls", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) {
                        selectedTab = 1
                    }
                    SettingTabChip("FAQ & Help", isSelected = selectedTab == 2, modifier = Modifier.weight(1f)) {
                        selectedTab = 2
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // ==========================================
                        // TAB 0: STEP-BY-STEP CONNECTION INSTRUCTIONS
                        // ==========================================
                        Text(
                            text = "HOW TO CONNECT TO YOUR PC (5 STEPS)",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // STEP 1
                        StepCard(
                            stepNumber = "1",
                            title = "Connect to Same Wi-Fi or Hotspot",
                            desc = "Ensure your phone and your Windows laptop are on the same Wi-Fi network.",
                            badge = "⚡ Hotspot gives lowest ~4ms ping"
                        ) {
                            Text(
                                text = "💡 Pro Tip: On Windows 10/11, enable 'Mobile Hotspot' in Settings and connect your phone to it directly. This guarantees direct connection without router interference!",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // STEP 2
                        StepCard(
                            stepNumber = "2",
                            title = "Install Python Dependencies on PC",
                            desc = "Open PowerShell or Command Prompt on your PC and run this single command:",
                            badge = "One-time setup"
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateBackground)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "pip install --default-timeout=1000 websockets pyautogui pillow",
                                        color = TechGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { copyCommand("pip install --default-timeout=1000 websockets pyautogui pillow") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanPrimary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 Tip: On Windows, mouse & keyboard works natively even without pyautogui! Only 'websockets' is required.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // STEP 3
                        StepCard(
                            stepNumber = "3",
                            title = "Run the Companion Server Script",
                            desc = "Run the companion Python server on your PC:",
                            badge = "Free & Zero Bloat"
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateBackground)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "python desklink_server.py",
                                        color = TechGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { copyCommand("python desklink_server.py") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanPrimary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = onOpenServerScript,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanContainer, contentColor = CyanPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Get & Copy Python Script", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // STEP 4
                        StepCard(
                            stepNumber = "4",
                            title = "Allow Windows Defender Firewall",
                            desc = "If Windows Firewall prompts 'Allow Python on your network?', choose 'Private Networks' and click Allow.",
                            badge = "Important"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // STEP 5
                        StepCard(
                            stepNumber = "5",
                            title = "Tap Connect in the App",
                            desc = "Go to the 'Connect' tab in the app. Your PC will appear automatically in the list. Tap 'Connect' or enter the IP address printed in your PC terminal!",
                            badge = "Instant Pairing"
                        )

                    } else if (selectedTab == 1) {
                        // ==========================================
                        // TAB 1: CONTROLS & SENSITIVITY CUSTOMIZATION
                        // ==========================================
                        Text(
                            text = "SUPPORTED MULTI-FINGER GESTURES",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Windows 10/11 Precision Gestures",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                GestureItemRow("1 Finger", "Drag to move cursor • Tap to Left Click")
                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 6.dp))
                                GestureItemRow("2 Fingers Tap", "Right Click")
                                GestureItemRow("2 Fingers Drag", "Smooth Scroll (Vertical & Horizontal)")
                                GestureItemRow("2 Fingers Pinch", "Zoom In / Out (Ctrl + Scroll)")
                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 6.dp))
                                GestureItemRow("3 Fingers Tap", "Middle Click (Open in new tab)")
                                GestureItemRow("3 Fingers Swipe Up", "Task View (Win + Tab)")
                                GestureItemRow("3 Fingers Swipe Down", "Show Desktop (Win + D)")
                                GestureItemRow("3 Fingers Swipe Left/Right", "Switch Windows (Alt + Tab)")
                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 6.dp))
                                GestureItemRow("4 Fingers Tap", "Windows Action Center (Win + A)")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "MOUSE & TRACKPAD SETTINGS",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Pointer Speed
                                Text(
                                    text = "Cursor Sensitivity (${"%.1f".format(mouseSens)}x)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = mouseSens,
                                    onValueChange = {
                                        mouseSens = it
                                        viewModel.prefs.mouseSensitivity = it
                                    },
                                    valueRange = 0.5f..3.0f,
                                    colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Scroll Speed
                                Text(
                                    text = "Scroll Wheel Sensitivity (${"%.1f".format(scrollSens)}x)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = scrollSens,
                                    onValueChange = {
                                        scrollSens = it
                                        viewModel.prefs.scrollSensitivity = it
                                    },
                                    valueRange = 0.5f..2.5f,
                                    colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Invert Scroll
                                SettingToggleRow(
                                    title = "Invert Scroll Direction",
                                    desc = "Reverse natural scrolling direction",
                                    checked = invertScroll,
                                    onCheckedChange = {
                                        invertScroll = it
                                        viewModel.prefs.invertScroll = it
                                    }
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Tap to Click
                                SettingToggleRow(
                                    title = "Tap to Left-Click",
                                    desc = "Single finger tap acts as Left Click",
                                    checked = tapToClick,
                                    onCheckedChange = {
                                        tapToClick = it
                                        viewModel.prefs.tapToClick = it
                                    }
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Two-finger right click
                                SettingToggleRow(
                                    title = "Two-Finger Right Click",
                                    desc = "Tapping with two fingers triggers Right Click",
                                    checked = twoFingerRightClick,
                                    onCheckedChange = {
                                        twoFingerRightClick = it
                                        viewModel.prefs.twoFingerRightClick = it
                                    }
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Haptic Feedback
                                SettingToggleRow(
                                    title = "Haptic Vibration Feedback",
                                    desc = "Vibrate on trackpad clicks and keystrokes",
                                    checked = haptics,
                                    onCheckedChange = {
                                        haptics = it
                                        viewModel.prefs.hapticFeedback = it
                                    }
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Auto Clipboard Sync
                                SettingToggleRow(
                                    title = "Auto Clipboard Sync",
                                    desc = "Automatically sync copied text to PC",
                                    checked = autoClip,
                                    onCheckedChange = {
                                        autoClip = it
                                        viewModel.prefs.autoClipboardSync = it
                                    }
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Auto-Open Keyboard on PC Text Field
                                SettingToggleRow(
                                    title = "Auto-Open Keyboard on PC Text Fields",
                                    desc = "Automatically pop up phone keyboard when clicking an input box or text cursor on Windows",
                                    checked = autoOpenKeyboard,
                                    onCheckedChange = {
                                        autoOpenKeyboard = it
                                        viewModel.prefs.autoOpenKeyboardOnTextField = it
                                    }
                                )

                                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 10.dp))

                                // Auto-Open Keyboard on Screen Tap
                                SettingToggleRow(
                                    title = "Auto-Open Keyboard on Screen Mirror Tap",
                                    desc = "Summon soft keyboard immediately whenever you tap on the mirrored PC desktop",
                                    checked = autoOpenKeyboardClick,
                                    onCheckedChange = {
                                        autoOpenKeyboardClick = it
                                        viewModel.prefs.autoOpenKeyboardOnClick = it
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Reset to defaults button
                        OutlinedButton(
                            onClick = {
                                viewModel.prefs.resetToDefaults()
                                mouseSens = viewModel.prefs.mouseSensitivity
                                scrollSens = viewModel.prefs.scrollSensitivity
                                invertScroll = viewModel.prefs.invertScroll
                                tapToClick = viewModel.prefs.tapToClick
                                twoFingerRightClick = viewModel.prefs.twoFingerRightClick
                                haptics = viewModel.prefs.hapticFeedback
                                autoClip = viewModel.prefs.autoClipboardSync
                                autoOpenKeyboard = viewModel.prefs.autoOpenKeyboardOnTextField
                                autoOpenKeyboardClick = viewModel.prefs.autoOpenKeyboardOnClick
                                Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset All Controls to Default")
                        }

                    } else {
                        // ==========================================
                        // TAB 2: TROUBLESHOOTING & COMMON QUESTIONS
                        // ==========================================
                        Text(
                            text = "FREQUENTLY ASKED QUESTIONS & TROUBLESHOOTING",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // FAQ 0: Auto Keyboard on PC Fields
                        FaqAccordion(
                            title = "How does auto-keyboard typing work on PC?",
                            isExpanded = showFaqKeyboard,
                            onToggle = { showFaqKeyboard = !showFaqKeyboard }
                        ) {
                            Text(
                                text = "DeskLink coordinates Windows text fields with your phone:\n\n1. Windows Caret Detection: When you click into a search bar, browser URL box, or document on Windows, the companion server detects the active text cursor (I-Beam caret) and signals your phone.\n2. Automatic Keyboard Popup: Your phone's keyboard springs up instantly with a live typing bar.\n3. Direct Realtime Typing: As you type (or use Voice Dictation), every character, Backspace, Tab, and Enter is streamed directly into the PC field!\n4. You can also tap the 'Type' button on the Trackpad or Screen Mirror anytime.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // FAQ 1: Can't find PC?
                        FaqAccordion(
                            title = "App cannot detect PC automatically?",
                            isExpanded = showFaqIp,
                            onToggle = { showFaqIp = !showFaqIp }
                        ) {
                            Text(
                                text = "Some Wi-Fi routers disable UDP broadcasts ('AP Isolation' or 'Client Isolation'). You can connect directly:\n\n1. Look at your PC terminal window where 'desklink_server.py' is running; it displays your PC's IP address (e.g. 192.168.1.150).\n2. On the 'Connect' tab in the app, type that IP into the Direct IP Connection field and tap 'Connect to PC'.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // FAQ 2: Firewall
                        FaqAccordion(
                            title = "Windows Defender Firewall blocking connection?",
                            isExpanded = showFaqFirewall,
                            onToggle = { showFaqFirewall = !showFaqFirewall }
                        ) {
                            Text(
                                text = "To unblock:\n1. Open Windows Search -> type 'Allow an app through Windows Firewall'.\n2. Click 'Change settings'.\n3. Find 'Python' in the list and ensure both 'Private' and 'Public' checkboxes are checked.\n4. Click OK and restart the python server.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // FAQ 3: Mobile Hotspot for Lowest Ping
                        FaqAccordion(
                            title = "How to get ultra-low latency (<5ms)?",
                            isExpanded = showFaqHotspot,
                            onToggle = { showFaqHotspot = !showFaqHotspot }
                        ) {
                            Text(
                                text = "For gaming-grade response without Wi-Fi jitter:\n\n1. On Windows 10/11, search for 'Mobile Hotspot' in Settings and turn it ON.\n2. On your phone, connect to your laptop's hotspot network.\n3. The app will connect directly peer-to-peer with zero router lag!",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // About info
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "DeskLink Remote Console v1.0",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "100% Secure & Private local WebSocket connection. No third-party accounts, cloud relays, or data collection.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Action Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingTabChip(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) CyanPrimary else Color.Transparent,
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (isSelected) SlateBackground else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    desc: String,
    badge: String? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = SlateBackground,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanContainer
                    ) {
                        Text(
                            text = badge,
                            color = CyanPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                extraContent()
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SlateBackground,
                checkedTrackColor = CyanPrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SlateBackground
            )
        )
    }
}

@Composable
private fun FaqAccordion(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun GestureItemRow(
    gesture: String,
    action: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SlateSurface
        ) {
            Text(
                text = gesture,
                color = CyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = action,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
