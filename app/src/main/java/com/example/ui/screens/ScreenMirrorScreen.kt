package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.ui.components.FloatingTypingBar
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class TouchControlMode {
    DIRECT_TOUCH, // TeamViewer/AnyDesk style: touch & drag directly maps cursor to point on PC
    TRACKPAD,     // Relative delta movement
    DRAG_LOCK     // Holds left mouse button down while dragging
}

@Composable
fun ScreenMirrorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val bitmap by viewModel.currentScreenBitmap.collectAsState()
    val isStreaming by viewModel.isScreenStreamingActive.collectAsState()
    val connectionState by viewModel.effectiveConnectionState.collectAsState()
    val latencyMs by viewModel.client.latencyMs.collectAsState()
    val isFullScreen by viewModel.isMirrorFullScreen.collectAsState()
    val isTextFieldFocused by viewModel.isPcTextFieldActive.collectAsState()

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }

    var selectedQuality by remember { mutableStateOf("Balanced") }
    var touchMode by remember { mutableStateOf(TouchControlMode.DIRECT_TOUCH) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var showFullscreenHint by remember { mutableStateOf(false) }

    // Pointer visualization on touch
    var touchPointerPos by remember { mutableStateOf<Offset?>(null) }
    var isDraggingPointer by remember { mutableStateOf(false) }

    // Handle Back Button in Full Screen: cleanly exit full screen mode
    BackHandler(enabled = isFullScreen) {
        viewModel.setMirrorFullScreen(false)
    }

    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            showFullscreenHint = true
            delay(3500)
            showFullscreenHint = false
        }
    }

    // Helper to calculate exact normalized PC coordinates (0.0 to 1.0)
    // accounting for aspect ratio letterboxing / pillarboxing, zoom scale, and pan offset
    fun calculateNormalizedCoords(touchOffset: Offset): Pair<Float, Float>? {
        if (imageContainerSize.width <= 0 || imageContainerSize.height <= 0) return null
        val bmp = bitmap
        val containerW = imageContainerSize.width.toFloat()
        val containerH = imageContainerSize.height.toFloat()

        // Un-zoom and un-pan relative to container center
        val centerDx = touchOffset.x - containerW / 2f
        val centerDy = touchOffset.y - containerH / 2f
        val unzoomedDx = (centerDx - panOffset.x) / zoomScale
        val unzoomedDy = (centerDy - panOffset.y) / zoomScale
        val localX = containerW / 2f + unzoomedDx
        val localY = containerH / 2f + unzoomedDy

        // Determine rendered image frame inside container (ContentScale.Fit)
        val (renderedW, renderedH, offsetX, offsetY) = if (bmp != null && bmp.width > 0 && bmp.height > 0) {
            val aspectBmp = bmp.width.toFloat() / bmp.height.toFloat()
            val aspectContainer = containerW / containerH
            if (aspectContainer > aspectBmp) {
                val rH = containerH
                val rW = containerH * aspectBmp
                val offX = (containerW - rW) / 2f
                val offY = 0f
                listOf(rW, rH, offX, offY)
            } else {
                val rW = containerW
                val rH = containerW / aspectBmp
                val offX = 0f
                val offY = (containerH - rH) / 2f
                listOf(rW, rH, offX, offY)
            }
        } else {
            listOf(containerW, containerH, 0f, 0f)
        }

        val imgX = localX - offsetX
        val imgY = localY - offsetY

        val normX = (imgX / renderedW).coerceIn(0f, 1f)
        val normY = (imgY / renderedH).coerceIn(0f, 1f)
        return Pair(normX, normY)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullScreen) Color.Black else SlateBackground)
            .testTag("screen_mirror_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) 0.dp else 10.dp)
        ) {
            // Header Controls (Visible only in Normal Mode)
            if (!isFullScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateSurface)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        QualityChip(
                            label = "Speed",
                            isSelected = selectedQuality == "Speed",
                            onClick = {
                                selectedQuality = "Speed"
                                viewModel.setScreenQuality(quality = 40, scale = 0.35f)
                            }
                        )
                        QualityChip(
                            label = "Balanced",
                            isSelected = selectedQuality == "Balanced",
                            onClick = {
                                selectedQuality = "Balanced"
                                viewModel.setScreenQuality(quality = 55, scale = 0.55f)
                            }
                        )
                        QualityChip(
                            label = "HD",
                            isSelected = selectedQuality == "HD",
                            onClick = {
                                selectedQuality = "HD"
                                viewModel.setScreenQuality(quality = 75, scale = 0.85f)
                            }
                        )
                    }

                    // Mode indicator badge (Offline LAN / Bluetooth)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateCard)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isStreaming) TechGreen else TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStreaming) "LIVE • ${latencyMs}ms" else "OFFLINE READY",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Action buttons (Fullscreen, Keyboard, Reset Zoom, Play/Stop)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (zoomScale > 1.05f) {
                            IconButton(
                                onClick = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.FitScreen, contentDescription = "Reset Zoom", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Full Screen Toggle Button
                        IconButton(
                            onClick = { viewModel.setMirrorFullScreen(true) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyanContainer, RoundedCornerShape(8.dp))
                                .testTag("btn_toggle_fullscreen")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full Screen",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick Floating Soft Keyboard
                        IconButton(
                            onClick = { viewModel.toggleFloatingKeyboard() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(SlateCard, RoundedCornerShape(8.dp))
                                .testTag("btn_mirror_open_keyboard")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Open Keyboard",
                                tint = if (isTextFieldFocused) TechGreen else CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isStreaming) viewModel.stopScreenStream() else viewModel.startScreenStream()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(if (isStreaming) SlateCard else CyanPrimary, RoundedCornerShape(8.dp))
                                .testTag("btn_toggle_screen_stream")
                        ) {
                            Icon(
                                imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Stream",
                                tint = if (isStreaming) TextPrimary else SlateBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Shared PC Screen Canvas (TeamViewer Touch & Gesture Remote Control)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(if (isFullScreen) 0.dp else 1.dp, SlateBorder, if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp))
                    .onGloballyPositioned { coordinates ->
                        imageContainerSize = coordinates.size
                    }
                    // 1. Pinch-to-zoom and two-finger pan/scroll gestures
                    .pointerInput(imageContainerSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1f) {
                                zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                            }
                            if (zoomScale > 1f) {
                                val maxPanX = (imageContainerSize.width * (zoomScale - 1f)) / 2f
                                val maxPanY = (imageContainerSize.height * (zoomScale - 1f)) / 2f
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                    y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                )
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    }
                    // 2. Direct Touch, Drag Pointer Tracking & Mouse Events (TeamViewer Remote Style)
                    .pointerInput(imageContainerSize, touchMode, zoomScale, panOffset) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downOffset = down.position
                            val coords = calculateNormalizedCoords(downOffset)
                            touchPointerPos = downOffset
                            isDraggingPointer = true

                            if (coords != null) {
                                viewModel.client.sendMouseMoveToNorm(coords.first, coords.second)
                                if (touchMode == TouchControlMode.DRAG_LOCK) {
                                    viewModel.client.sendMouseClick("left", "down")
                                }
                            }

                            var isDrag = false
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) {
                                    val currentPos = change.position
                                    touchPointerPos = currentPos
                                    val moveDelta = currentPos - downOffset
                                    if (moveDelta.getDistance() > 10f) {
                                        isDrag = true
                                    }

                                    if (touchMode == TouchControlMode.TRACKPAD) {
                                        val positionChange = change.positionChange()
                                        viewModel.client.sendMouseMove(positionChange.x * 1.5f, positionChange.y * 1.5f)
                                    } else {
                                        val currentCoords = calculateNormalizedCoords(currentPos)
                                        if (currentCoords != null) {
                                            viewModel.client.sendMouseMoveToNorm(currentCoords.first, currentCoords.second)
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            // Pointer release
                            isDraggingPointer = false
                            if (touchMode == TouchControlMode.DRAG_LOCK) {
                                viewModel.client.sendMouseClick("left", "up")
                            }
                            touchPointerPos = null
                        }
                    }
                    // 3. Tap, Double Tap, and Long Press Gestures
                    .pointerInput(imageContainerSize, zoomScale, panOffset) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val coords = calculateNormalizedCoords(tapOffset)
                                if (coords != null) {
                                    viewModel.triggerHaptic(20)
                                    viewModel.client.sendMouseMoveToNorm(coords.first, coords.second)
                                    viewModel.client.sendMouseClick("left", "click")

                                    // Automatic keyboard popup on text field click
                                    if (viewModel.prefs.autoOpenKeyboardOnClick || viewModel.prefs.autoOpenKeyboardOnTextField) {
                                        viewModel.client.checkInputFocus()
                                        if (viewModel.prefs.autoOpenKeyboardOnClick) {
                                            viewModel.openFloatingKeyboard()
                                        }
                                    }
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                val coords = calculateNormalizedCoords(tapOffset)
                                if (coords != null) {
                                    viewModel.triggerHaptic(30)
                                    viewModel.client.sendMouseMoveToNorm(coords.first, coords.second)
                                    viewModel.client.sendMouseClick("left", "double")
                                }
                            },
                            onLongPress = { tapOffset ->
                                val coords = calculateNormalizedCoords(tapOffset)
                                if (coords != null) {
                                    viewModel.triggerHaptic(50)
                                    viewModel.client.sendMouseMoveToNorm(coords.first, coords.second)
                                    viewModel.client.sendMouseClick("right", "click")
                                }
                            }
                        )
                    }
                    .testTag("screen_canvas_box"),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Mirrored PC Screen Stream",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    )

                    // Touch Target Crosshair / Glow Indicator
                    touchPointerPos?.let { pos ->
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(pos.x.roundToInt() - 20, pos.y.roundToInt() - 20) }
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.25f))
                                .border(1.5.dp, CyanPrimary, CircleShape)
                        )
                    }
                } else {
                    // Ready / Connecting state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        if (isStreaming) {
                            CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Connecting to PC Screen Stream...",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Local Wi-Fi or Bluetooth • Zero Internet Required",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Local Remote Desktop Ready",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TeamViewer-style touch control • Works 100% offline without internet",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (connectionState is ConnectionState.Disconnected) {
                                        viewModel.startDemoMode()
                                    }
                                    viewModel.startScreenStream()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Start Live Screen Stream", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Full Screen Mode Overlay Controls
                if (isFullScreen) {
                    // Semi-transparent floating control pill on top right
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showOverlayControls,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateBackground.copy(alpha = 0.85f))
                                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Touch Mode Selector
                            IconButton(
                                onClick = {
                                    touchMode = when (touchMode) {
                                        TouchControlMode.DIRECT_TOUCH -> TouchControlMode.TRACKPAD
                                        TouchControlMode.TRACKPAD -> TouchControlMode.DRAG_LOCK
                                        TouchControlMode.DRAG_LOCK -> TouchControlMode.DIRECT_TOUCH
                                    }
                                    viewModel.triggerHaptic(15)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = when (touchMode) {
                                        TouchControlMode.DIRECT_TOUCH -> Icons.Default.TouchApp
                                        TouchControlMode.TRACKPAD -> Icons.Default.Mouse
                                        TouchControlMode.DRAG_LOCK -> Icons.Default.Lock
                                    },
                                    contentDescription = "Touch Mode",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quick Soft Keyboard
                            IconButton(
                                onClick = { viewModel.toggleFloatingKeyboard() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Keyboard",
                                    tint = if (isTextFieldFocused) TechGreen else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Reset Zoom (if zoomed)
                            if (zoomScale > 1.05f) {
                                IconButton(
                                    onClick = {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.FitScreen, contentDescription = "Fit Screen", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Exit Full Screen
                            IconButton(
                                onClick = { viewModel.setMirrorFullScreen(false) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyanContainer, RoundedCornerShape(8.dp))
                                    .testTag("btn_exit_fullscreen")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Full Screen",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Transient Full Screen Hint Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showFullscreenHint,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SlateSurface.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Full Screen Active • Swipe back to exit",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Quick Bar (In Normal Mode)
            if (!isFullScreen) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Click
                    Button(
                        onClick = { viewModel.onLeftClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text("Left Click", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Touch Mode Switcher Chip
                    Button(
                        onClick = {
                            touchMode = when (touchMode) {
                                TouchControlMode.DIRECT_TOUCH -> TouchControlMode.TRACKPAD
                                TouchControlMode.TRACKPAD -> TouchControlMode.DRAG_LOCK
                                TouchControlMode.DRAG_LOCK -> TouchControlMode.DIRECT_TOUCH
                            }
                            viewModel.triggerHaptic(15)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (touchMode == TouchControlMode.DRAG_LOCK) CyanContainer else SlateCard,
                            contentColor = if (touchMode == TouchControlMode.DRAG_LOCK) CyanPrimary else TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = when (touchMode) {
                                TouchControlMode.DIRECT_TOUCH -> Icons.Default.TouchApp
                                TouchControlMode.TRACKPAD -> Icons.Default.Mouse
                                TouchControlMode.DRAG_LOCK -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (touchMode) {
                                TouchControlMode.DIRECT_TOUCH -> "Direct"
                                TouchControlMode.TRACKPAD -> "Trackpad"
                                TouchControlMode.DRAG_LOCK -> "Drag Lock"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Enter Key
                    Button(
                        onClick = { viewModel.sendKeyPress("enter") },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = CyanPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text("Enter", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Right Click
                    Button(
                        onClick = { viewModel.onRightClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text("Right Click", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Floating Soft Keyboard Bar for Direct Typing on Mirrored PC Screen
            FloatingTypingBar(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QualityChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CyanPrimary else Color.Transparent,
        modifier = Modifier.height(30.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) SlateBackground else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
