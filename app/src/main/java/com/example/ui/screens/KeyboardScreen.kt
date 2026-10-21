package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TechAmber
import com.example.ui.theme.TechRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun KeyboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val isCtrl by viewModel.isCtrlActive.collectAsState()
    val isAlt by viewModel.isAltActive.collectAsState()
    val isShift by viewModel.isShiftActive.collectAsState()
    val isWin by viewModel.isWinActive.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("keyboard_screen")
    ) {
        // Direct IME Text Sender Input
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Live Keyboard Input",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { newText ->
                            inputText = newText
                        },
                        placeholder = { Text("Type here to send text or press Enter...", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotEmpty()) {
                                    viewModel.sendCustomText(inputText)
                                    viewModel.sendKeyPress("enter")
                                    inputText = ""
                                }
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SlateBackground,
                            unfocusedContainerColor = SlateBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_live_typing")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { inputText = "" },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            if (inputText.isNotEmpty()) {
                                viewModel.sendCustomText(inputText)
                                inputText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("btn_send_typed_text")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sticky Modifier Keys (Ctrl, Alt, Shift, Win)
        Text(
            text = "MODIFIER KEYS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ModifierKeyButton(label = "Ctrl", isActive = isCtrl, modifier = Modifier.weight(1f)) {
                viewModel.toggleModifier("ctrl")
            }
            ModifierKeyButton(label = "Alt", isActive = isAlt, modifier = Modifier.weight(1f)) {
                viewModel.toggleModifier("alt")
            }
            ModifierKeyButton(label = "Shift", isActive = isShift, modifier = Modifier.weight(1f)) {
                viewModel.toggleModifier("shift")
            }
            ModifierKeyButton(label = "Win ❖", isActive = isWin, modifier = Modifier.weight(1f)) {
                viewModel.toggleModifier("win")
            }
            if (isCtrl || isAlt || isShift || isWin) {
                Surface(
                    onClick = { viewModel.resetModifiers() },
                    shape = RoundedCornerShape(10.dp),
                    color = TechRed.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TechRed),
                    modifier = Modifier.height(42.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Reset", color = TechRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Essential Navigation / Edit Keys
        Text(
            text = "ESSENTIAL PC KEYS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyButton(label = "Esc", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("esc") }
            KeyButton(label = "Tab", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("tab") }
            KeyButton(label = "Enter ↵", modifier = Modifier.weight(1.3f)) { viewModel.sendKeyPress("enter") }
            KeyButton(label = "Space", modifier = Modifier.weight(1.4f)) { viewModel.sendKeyPress("space") }
            KeyButton(label = "⌫ Del", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("backspace") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Windows 10 / 11 Quick Hotkeys
        Text(
            text = "WINDOWS 10 / 11 SHORTCUTS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HotkeyChip(label = "Win + D", desc = "Desktop") { viewModel.sendHotkey(listOf("win", "d")) }
            HotkeyChip(label = "Alt + Tab", desc = "Switch") { viewModel.sendHotkey(listOf("alt", "tab")) }
            HotkeyChip(label = "Win + E", desc = "Explorer") { viewModel.sendHotkey(listOf("win", "e")) }
            HotkeyChip(label = "Win + L", desc = "Lock PC") { viewModel.sendHotkey(listOf("win", "l")) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HotkeyChip(label = "Ctrl + C", desc = "Copy") { viewModel.sendHotkey(listOf("ctrl", "c")) }
            HotkeyChip(label = "Ctrl + V", desc = "Paste") { viewModel.sendHotkey(listOf("ctrl", "v")) }
            HotkeyChip(label = "Ctrl + Z", desc = "Undo") { viewModel.sendHotkey(listOf("ctrl", "z")) }
            HotkeyChip(label = "Ctrl + A", desc = "Select All") { viewModel.sendHotkey(listOf("ctrl", "a")) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HotkeyChip(label = "Ctrl+Shift+Esc", desc = "Task Mgr") { viewModel.sendHotkey(listOf("ctrl", "shift", "esc")) }
            HotkeyChip(label = "Win+Shift+S", desc = "Snip") { viewModel.sendHotkey(listOf("win", "shift", "s")) }
            HotkeyChip(label = "Alt + F4", desc = "Close App") { viewModel.sendHotkey(listOf("alt", "f4")) }
            HotkeyChip(label = "Ctrl + S", desc = "Save") { viewModel.sendHotkey(listOf("ctrl", "s")) }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // D-Pad and Navigation Cluster
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Directional Arrow D-Pad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateSurface)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.sendKeyPress("up") },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = CyanPrimary)
                }
                Row {
                    IconButton(
                        onClick = { viewModel.sendKeyPress("left") },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = CyanPrimary)
                    }
                    IconButton(
                        onClick = { viewModel.sendKeyPress("down") },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = CyanPrimary)
                    }
                    IconButton(
                        onClick = { viewModel.sendKeyPress("right") },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = CyanPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Navigation Keys: Page Up, Page Down, Home, End
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyButton(label = "Home", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("home") }
                    KeyButton(label = "End", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("end") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyButton(label = "Pg Up", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("pageup") }
                    KeyButton(label = "Pg Dn", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("pagedown") }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Function Keys (F1 - F12)
        Text(
            text = "FUNCTION KEYS (F1 - F12)",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..12).forEach { fNum ->
                Surface(
                    onClick = { viewModel.sendKeyPress("f$fNum") },
                    shape = RoundedCornerShape(8.dp),
                    color = SlateCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.size(width = 46.dp, height = 38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "F$fNum", color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PC Media Controls Deck
        Text(
            text = "MEDIA & VOLUME CONTROLS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateSurface)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaButton(icon = Icons.Default.VolumeMute, label = "Mute") { viewModel.client.sendMediaAction("mute") }
            MediaButton(icon = Icons.Default.VolumeDown, label = "Vol -") { viewModel.client.sendMediaAction("volume_down") }
            MediaButton(icon = Icons.Default.VolumeUp, label = "Vol +") { viewModel.client.sendMediaAction("volume_up") }
            MediaButton(icon = Icons.Default.FastRewind, label = "Prev") { viewModel.client.sendMediaAction("prev") }
            MediaButton(icon = Icons.Default.PlayArrow, label = "Play") { viewModel.client.sendMediaAction("play_pause") }
            MediaButton(icon = Icons.Default.FastForward, label = "Next") { viewModel.client.sendMediaAction("next") }
        }
    }
}

@Composable
private fun ModifierKeyButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) CyanPrimary else SlateCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) CyanPrimary else SlateBorder
        ),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isActive) SlateBackground else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = SlateCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HotkeyChip(
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = SlateSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.height(50.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MediaButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = CyanPrimary, modifier = Modifier.size(20.dp))
        }
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}
