package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MainViewModel

@Composable
fun FloatingTypingBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isVisible by viewModel.showFloatingKeyboard.collectAsState()
    val isPcFieldActive by viewModel.isPcTextFieldActive.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var textInput by remember { mutableStateOf(TextFieldValue("")) }

    // When made visible, request focus and show soft keyboard immediately
    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            textInput = TextFieldValue("")
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 100 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 100 }),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("floating_typing_bar")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.98f)),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isPcFieldActive) CyanPrimary else SlateBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Header row: status badge, quick keys, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPcFieldActive) TechGreen else CyanPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPcFieldActive) "PC TEXT FIELD FOCUSED" else "DIRECT TYPING",
                            color = if (isPcFieldActive) CyanPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Quick micro-keys
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypingMicroKey(label = "⇥ Tab") { viewModel.sendTab() }
                        TypingMicroKey(label = "↵ Enter") { viewModel.sendEnter() }
                        TypingMicroKey(label = "⌫ Del") { viewModel.sendBackspace() }

                        IconButton(
                            onClick = { viewModel.closeFloatingKeyboard() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Keyboard",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive text field that streams typed characters to the PC in real time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { newValue ->
                            val oldStr = textInput.text
                            val newStr = newValue.text

                            if (newStr.length > oldStr.length) {
                                // Characters typed or pasted -> send difference immediately
                                val added = newStr.substring(oldStr.length)
                                viewModel.sendDirectChar(added)
                            } else if (newStr.length < oldStr.length) {
                                // Backspace pressed
                                val deletedCount = oldStr.length - newStr.length
                                repeat(deletedCount) {
                                    viewModel.sendBackspace()
                                }
                            }
                            textInput = newValue
                        },
                        placeholder = {
                            Text(
                                text = "Type here or use voice to type on PC...",
                                color = TextTertiary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                viewModel.sendEnter()
                                textInput = TextFieldValue("")
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = SlateSurface,
                            unfocusedContainerColor = SlateSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("floating_typing_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Voice Dictation Button
                    Surface(
                        onClick = {
                            if (viewModel.voiceDictationManager.isListening.value) {
                                viewModel.voiceDictationManager.stopListening()
                            } else {
                                viewModel.voiceDictationManager.startListening()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (viewModel.voiceDictationManager.isListening.value) CyanPrimary else CyanContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Dictate",
                                tint = if (viewModel.voiceDictationManager.isListening.value) SlateSurface else CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Send / Enter Button
                    Surface(
                        onClick = {
                            viewModel.sendEnter()
                            textInput = TextFieldValue("")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Enter",
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick shortcuts bar (Ctrl+A, Ctrl+C, Ctrl+V, Undo, Esc)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypingPillAction("Ctrl+V (Paste)") { viewModel.sendHotkey(listOf("ctrl", "v")) }
                    TypingPillAction("Ctrl+A (Select All)") { viewModel.sendHotkey(listOf("ctrl", "a")) }
                    TypingPillAction("Ctrl+C (Copy)") { viewModel.sendHotkey(listOf("ctrl", "c")) }
                    TypingPillAction("Ctrl+Z (Undo)") { viewModel.sendHotkey(listOf("ctrl", "z")) }
                    TypingPillAction("Esc") { viewModel.sendKeyPress("esc") }
                    TypingPillAction("Clear Input") { textInput = TextFieldValue("") }
                }
            }
        }
    }
}

@Composable
private fun TypingMicroKey(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.height(24.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TypingPillAction(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = SlateSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder.copy(alpha = 0.8f)),
        modifier = Modifier.height(28.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
