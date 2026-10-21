package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TechRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun MicVoiceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
    }

    val isListeningVoice by viewModel.voiceDictationManager.isListening.collectAsState()
    val partialVoiceText by viewModel.voiceDictationManager.partialText.collectAsState()
    val lastTranscribed by viewModel.voiceDictationManager.lastTranscribed.collectAsState()
    val soundLevel by viewModel.voiceDictationManager.soundLevel.collectAsState()

    val isStreamingMic by viewModel.audioStreamer.isRecording.collectAsState()
    val micAmplitude by viewModel.audioStreamer.amplitude.collectAsState()

    var activeMode by remember { mutableStateOf("dictation") } // "dictation" or "relay"

    val animatedMicPulse by animateFloatAsState(
        targetValue = if (isListeningVoice) 1.0f + (soundLevel * 0.4f) else 1.0f,
        label = "mic_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mic_voice_screen")
    ) {
        // Mode Switcher (Voice Dictation vs PC Mic Relay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateSurface)
                .padding(4.dp)
        ) {
            Surface(
                onClick = { activeMode = "dictation" },
                shape = RoundedCornerShape(10.dp),
                color = if (activeMode == "dictation") CyanPrimary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Voice Dictation (Type to PC)",
                        color = if (activeMode == "dictation") SlateBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                onClick = { activeMode = "relay" },
                shape = RoundedCornerShape(10.dp),
                color = if (activeMode == "relay") CyanPrimary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Laptop Mic Stream",
                        color = if (activeMode == "relay") SlateBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasRecordPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MicNone,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Microphone Permission Needed",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Grant audio recording to use speech-to-text dictation and wireless laptop microphone.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (activeMode == "dictation") {
            // Voice Dictation Section
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Voice to PC Typing",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Speak into phone — words are typed directly onto your PC screen!",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Animated Glowing Mic Button
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(animatedMicPulse)
                            .clip(CircleShape)
                            .background(if (isListeningVoice) TechGreen.copy(alpha = 0.2f) else CyanContainer.copy(alpha = 0.4f))
                            .border(
                                width = 3.dp,
                                color = if (isListeningVoice) TechGreen else CyanPrimary,
                                shape = CircleShape
                            )
                            .clickable {
                                if (isListeningVoice) {
                                    viewModel.voiceDictationManager.stopListening()
                                } else {
                                    viewModel.voiceDictationManager.startListening()
                                }
                            }
                            .testTag("btn_toggle_voice_dictation"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListeningVoice) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isListeningVoice) TechGreen else CyanPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isListeningVoice) "Listening... (Speak now)" else "Tap Microphone to Speak",
                        color = if (isListeningVoice) TechGreen else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio level waveform bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(30.dp)
                    ) {
                        repeat(9) { index ->
                            val heightRatio = if (isListeningVoice) {
                                ((soundLevel * (1.2f - (index % 3) * 0.25f)).coerceIn(0.15f, 1f))
                            } else 0.15f

                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((26 * heightRatio).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isListeningVoice) CyanPrimary else SlateBorder)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Partial transcription feedback box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBackground)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = when {
                                partialVoiceText.isNotBlank() -> "\"$partialVoiceText...\""
                                lastTranscribed.isNotBlank() -> "Sent to PC: \"$lastTranscribed\""
                                isListeningVoice -> "Listening for words..."
                                else -> "Tap the mic and speak a phrase (e.g., 'Hello world', 'Open browser')."
                            },
                            color = if (partialVoiceText.isNotBlank()) CyanPrimary else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Dictation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.sendKeyPress("enter") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Enter ↵")
                }
                Button(
                    onClick = { viewModel.sendKeyPress("backspace") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Backspace ⌫")
                }
            }
        } else {
            // Laptop Mic Stream Section (Relay raw microphone to PC)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wireless Laptop Microphone",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Use your smartphone as a high-fidelity external microphone for your laptop",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isStreamingMic) TechGreen.copy(alpha = 0.2f) else SlateBackground)
                            .border(
                                width = 3.dp,
                                color = if (isStreamingMic) TechGreen else SlateBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                if (isStreamingMic) {
                                    viewModel.audioStreamer.stopStreaming()
                                } else {
                                    viewModel.audioStreamer.startStreaming()
                                }
                            }
                            .testTag("btn_toggle_mic_relay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isStreamingMic) Icons.Default.VolumeUp else Icons.Default.MicOff,
                            contentDescription = "Mic Stream",
                            tint = if (isStreamingMic) TechGreen else TextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isStreamingMic) "MIC ACTIVE • Streaming 16kHz PCM" else "MIC OFF • Tap to Broadcast",
                        color = if (isStreamingMic) TechGreen else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Real-time amplitude VU meter
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(36.dp)
                    ) {
                        repeat(12) { i ->
                            val active = (micAmplitude * 12) > i
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            !active -> SlateBorder
                                            i < 8 -> TechGreen
                                            i < 10 -> CyanPrimary
                                            else -> TechRed
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
