package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FloatingTypingBar
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TechAmber
import com.example.ui.theme.TechGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun TrackpadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isDragLock by viewModel.isDragLockActive.collectAsState()
    val isFloatingKeyboardOpen by viewModel.showFloatingKeyboard.collectAsState()
    val isPcFieldActive by viewModel.isPcTextFieldActive.collectAsState()
    var mouseSensitivity by remember { mutableFloatStateOf(viewModel.prefs.mouseSensitivity) }

    // Smart UI states: keep default screen clean & minimal
    var showQuickKeysSheet by remember { mutableStateOf(false) }
    var showSpeedSlider by remember { mutableStateOf(false) }
    var showBottomClickBar by remember { mutableStateOf(true) }
    var lastTapTimestamp by remember { mutableStateOf(0L) }

    // Multi-touch magnetic points (supports 1, 2, 3+ simultaneous touch fields!)
    var activeTouches by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isTouchActive by remember { mutableStateOf(false) }

    // Multi-finger gesture feedback HUD pill
    var gestureHudText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gestureHudText) {
        if (gestureHudText != null) {
            delay(1400)
            gestureHudText = null
        }
    }

    // Organic magnetic spring return animation
    val touchStrength by animateFloatAsState(
        targetValue = if (isTouchActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "magnetic_spring"
    )

    // Scroll indicator state
    var isScrollingEdge by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("trackpad_screen")
    ) {
        // ==========================================
        // 1. FULL-BLEED MAGNETIC DOTS TRACKPAD CANVAS
        // ==========================================
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            activeTouches = listOf(down.position)
                            isTouchActive = true

                            var isMoved = false
                            var maxPointerCount = 1
                            val initialDownTime = System.currentTimeMillis()

                            val startX = down.position.x
                            val edgeThreshold = canvasWidth - 44.dp.toPx()
                            val isInEdgeScroll = startX >= edgeThreshold

                            var initialMultiCenter = down.position
                            var prevPinchDistance = 0f
                            var isPinchActive = false
                            var hasTriggered3FingerAction = false

                            if (isInEdgeScroll) {
                                isScrollingEdge = true
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointers = event.changes.filter { it.pressed }

                                    if (pointers.isEmpty()) break

                                    maxPointerCount = maxOf(maxPointerCount, pointers.size)
                                    activeTouches = pointers.map { it.position }

                                    // --- 3 FINGER GESTURES (Windows 10/11 Precision Gestures) ---
                                    if (pointers.size == 3) {
                                        val curCenter = Offset(
                                            (pointers[0].position.x + pointers[1].position.x + pointers[2].position.x) / 3f,
                                            (pointers[0].position.y + pointers[1].position.y + pointers[2].position.y) / 3f
                                        )

                                        if (!hasTriggered3FingerAction) {
                                            val dy = curCenter.y - initialMultiCenter.y
                                            val dx = curCenter.x - initialMultiCenter.x

                                            if (dy < -45.dp.toPx()) {
                                                // 3-Finger Swipe UP -> Task View (Win + Tab)
                                                hasTriggered3FingerAction = true
                                                isMoved = true
                                                viewModel.onThreeFingerSwipeUp()
                                                gestureHudText = "3-Finger Swipe Up: Task View"
                                            } else if (dy > 45.dp.toPx()) {
                                                // 3-Finger Swipe DOWN -> Show Desktop (Win + D)
                                                hasTriggered3FingerAction = true
                                                isMoved = true
                                                viewModel.onThreeFingerSwipeDown()
                                                gestureHudText = "3-Finger Swipe Down: Show Desktop"
                                            } else if (dx < -45.dp.toPx()) {
                                                // 3-Finger Swipe LEFT -> Switch App (Alt + Tab)
                                                hasTriggered3FingerAction = true
                                                isMoved = true
                                                viewModel.onThreeFingerSwipeLeft()
                                                gestureHudText = "3-Finger Swipe Left: Switch Window"
                                            } else if (dx > 45.dp.toPx()) {
                                                // 3-Finger Swipe RIGHT -> Prev App (Alt + Shift + Tab)
                                                hasTriggered3FingerAction = true
                                                isMoved = true
                                                viewModel.onThreeFingerSwipeRight()
                                                gestureHudText = "3-Finger Swipe Right: Prev Window"
                                            }
                                        }

                                        pointers.forEach { it.consume() }
                                        continue
                                    }

                                    // --- 4 FINGER GESTURES ---
                                    if (pointers.size >= 4) {
                                        // 4-finger gesture
                                        pointers.forEach { it.consume() }
                                        continue
                                    }

                                    // --- 2 FINGER GESTURES (Scroll & Pinch-to-Zoom) ---
                                    if (pointers.size == 2) {
                                        val p0 = pointers[0]
                                        val p1 = pointers[1]

                                        // Calculate distance for pinch detection
                                        val currentDistance = sqrt(
                                            (p0.position.x - p1.position.x) * (p0.position.x - p1.position.x) +
                                            (p0.position.y - p1.position.y) * (p0.position.y - p1.position.y)
                                        )

                                        if (prevPinchDistance == 0f) {
                                            prevPinchDistance = currentDistance
                                            initialMultiCenter = (p0.position + p1.position) / 2f
                                        } else {
                                            val distDelta = currentDistance - prevPinchDistance
                                            if (abs(distDelta) > 18.dp.toPx()) {
                                                isPinchActive = true
                                                isMoved = true
                                                val zoomIn = distDelta > 0
                                                viewModel.onPinchZoom(zoomIn)
                                                gestureHudText = if (zoomIn) "Pinch: Zoom In" else "Pinch: Zoom Out"
                                                prevPinchDistance = currentDistance
                                            }
                                        }

                                        if (!isPinchActive) {
                                            val deltaY = (p0.positionChange().y + p1.positionChange().y) / 2f
                                            if (abs(deltaY) > 0.4f) {
                                                isMoved = true
                                                viewModel.onScroll(deltaY)
                                            }
                                        }

                                        pointers.forEach { it.consume() }
                                        continue
                                    }

                                    // --- 1 FINGER GESTURE (Cursor Move & Edge Scroll) ---
                                    val primary = pointers.firstOrNull { it.id == pointerId } ?: pointers.first()
                                    val change = primary.positionChange()

                                    if (change.x != 0f || change.y != 0f) {
                                        isMoved = true
                                        primary.consume()

                                        if (isInEdgeScroll) {
                                            viewModel.onScroll(change.y)
                                        } else {
                                            viewModel.onTrackpadMove(change.x, change.y)
                                        }
                                    }
                                }
                            } finally {
                                isTouchActive = false
                                isScrollingEdge = false

                                val duration = System.currentTimeMillis() - initialDownTime

                                // Tap detection based on finger count:
                                if (!isMoved && duration < 320) {
                                    when (maxPointerCount) {
                                        1 -> {
                                            // 1-Finger Tap: Left Click
                                            viewModel.onLeftClick()
                                            val now = System.currentTimeMillis()
                                            val isDouble = (now - lastTapTimestamp) < 320
                                            lastTapTimestamp = now
                                            viewModel.onTrackpadTapToType(isDoubleTap = isDouble)
                                        }
                                        2 -> {
                                            // 2-Finger Tap: Right Click
                                            viewModel.onRightClick()
                                            gestureHudText = "2-Finger Tap: Right Click"
                                        }
                                        3 -> {
                                            // 3-Finger Tap: Middle Click
                                            viewModel.onMiddleClick()
                                            gestureHudText = "3-Finger Tap: Middle Click"
                                        }
                                        4 -> {
                                            // 4-Finger Tap: Action Center
                                            viewModel.onFourFingerTap()
                                            gestureHudText = "4-Finger Tap: Action Center"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .testTag("magnetic_trackpad_canvas")
            ) {
                val gridSpacing = 32.dp.toPx()
                val cols = (size.width / gridSpacing).toInt() + 1
                val rows = (size.height / gridSpacing).toInt() + 1

                val startX = (size.width - (cols - 1) * gridSpacing) / 2f
                val startY = (size.height - (rows - 1) * gridSpacing) / 2f

                val maxAttractRadius = 145.dp.toPx()
                val maxAttractRadiusSq = maxAttractRadius * maxAttractRadius
                val maxPullDist = 24.dp.toPx()

                val baseDotRadius = 1.6.dp.toPx()
                val relaxedColor = Color(0xFF1E2D4A).copy(alpha = 0.45f)
                val neonCyan = Color(0xFF00E5FF)

                val touches = activeTouches
                val strength = touchStrength

                // Draw fingertip glowing magnetic aura around EVERY active finger
                if (touches.isNotEmpty() && strength > 0.01f) {
                    touches.forEach { touchPoint ->
                        // Soft ambient radial glow under finger
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonCyan.copy(alpha = 0.18f * strength),
                                    neonCyan.copy(alpha = 0.05f * strength),
                                    Color.Transparent
                                ),
                                center = touchPoint,
                                radius = maxAttractRadius * 0.95f
                            ),
                            radius = maxAttractRadius * 0.95f,
                            center = touchPoint
                        )

                        // Subtle pulsating magnetic ring
                        drawCircle(
                            color = neonCyan.copy(alpha = 0.35f * strength),
                            radius = (36.dp.toPx() + (1f - strength) * 12.dp.toPx()),
                            center = touchPoint,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                    }
                }

                // Iterate over dot grid with multi-point magnetic deflection
                for (c in 0 until cols) {
                    val x0 = startX + c * gridSpacing
                    for (r in 0 until rows) {
                        val y0 = startY + r * gridSpacing

                        var renderX = x0
                        var renderY = y0
                        var radius = baseDotRadius
                        var dotColor = relaxedColor

                        if (touches.isNotEmpty() && strength > 0.005f) {
                            // Find closest finger to this dot
                            var closestDistSq = Float.MAX_VALUE
                            var closestTouch: Offset? = null

                            for (touch in touches) {
                                val dx = touch.x - x0
                                val dy = touch.y - y0
                                val dSq = dx * dx + dy * dy
                                if (dSq < closestDistSq) {
                                    closestDistSq = dSq
                                    closestTouch = touch
                                }
                            }

                            if (closestTouch != null && closestDistSq < maxAttractRadiusSq) {
                                val dx = closestTouch.x - x0
                                val dy = closestTouch.y - y0
                                val dist = sqrt(closestDistSq).coerceAtLeast(0.001f)
                                val proximity = (1f - dist / maxAttractRadius).coerceIn(0f, 1f)
                                val pullFactor = proximity * proximity * strength

                                val pullDist = pullFactor * maxPullDist
                                renderX = x0 + (dx / dist) * pullDist
                                renderY = y0 + (dy / dist) * pullDist

                                radius = baseDotRadius + (proximity * 3.4.dp.toPx() * strength)

                                val alpha = 0.45f + (proximity * 0.5f * strength)
                                dotColor = neonCyan.copy(alpha = alpha.coerceIn(0f, 0.95f))

                                if (proximity > 0.4f) {
                                    drawCircle(
                                        color = neonCyan.copy(alpha = 0.18f * proximity * strength),
                                        radius = radius * 2.2f,
                                        center = Offset(renderX, renderY)
                                    )
                                }
                            }
                        }

                        // Draw the magnetic lattice dot
                        drawCircle(
                            color = dotColor,
                            radius = radius,
                            center = Offset(renderX, renderY)
                        )
                    }
                }

                // Subtle edge scroll guide accent on the right border
                val edgeGuideX = size.width - 3.dp.toPx()
                drawLine(
                    color = if (isScrollingEdge) neonCyan else Color(0xFF1E2D4A).copy(alpha = 0.3f),
                    start = Offset(edgeGuideX, size.height * 0.2f),
                    end = Offset(edgeGuideX, size.height * 0.8f),
                    strokeWidth = if (isScrollingEdge) 3.5.dp.toPx() else 1.5.dp.toPx()
                )
            }
        }

        // ==========================================
        // 2. LIVE MULTI-FINGER GESTURE HUD PILL
        // ==========================================
        AnimatedVisibility(
            visible = gestureHudText != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -30 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 54.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SlateCard.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.8f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gestureHudText ?: "",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 3. SMART FLOATING ISLAND (MINIMAL TOP PILL)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SlateCard.copy(alpha = 0.82f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder.copy(alpha = 0.8f)),
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("smart_top_capsule")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Speed Selector Capsule button
                    Surface(
                        onClick = { showSpeedSlider = !showSpeedSlider },
                        shape = RoundedCornerShape(14.dp),
                        color = if (showSpeedSlider) CyanContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = if (showSpeedSlider) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${"%.1f".format(mouseSensitivity)}x",
                                color = if (showSpeedSlider) CyanPrimary else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Drag Lock Toggle Badge
                    Surface(
                        onClick = { viewModel.toggleDragLock() },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDragLock) TechAmber else Color.Transparent,
                        modifier = Modifier.testTag("btn_minimal_drag_lock")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDragLock) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Drag Lock",
                                tint = if (isDragLock) SlateBackground else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDragLock) "Locked" else "Drag",
                                color = if (isDragLock) SlateBackground else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quick PC Keys Drawer Button
                    Surface(
                        onClick = { showQuickKeysSheet = !showQuickKeysSheet },
                        shape = RoundedCornerShape(14.dp),
                        color = if (showQuickKeysSheet) CyanContainer else Color.Transparent,
                        modifier = Modifier.testTag("btn_quick_keys_drawer")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Keys",
                                tint = if (showQuickKeysSheet) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keys",
                                color = if (showQuickKeysSheet) CyanPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Quick Soft Keyboard / Floating Type Button
                    Surface(
                        onClick = { viewModel.toggleFloatingKeyboard() },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isFloatingKeyboardOpen) CyanContainer else Color.Transparent,
                        modifier = Modifier.testTag("btn_toggle_floating_type")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Type",
                                tint = if (isFloatingKeyboardOpen) CyanPrimary else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Type",
                                color = if (isFloatingKeyboardOpen) CyanPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Full Screen / Toggle Bottom Buttons Button
                    IconButton(
                        onClick = { showBottomClickBar = !showBottomClickBar },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (showBottomClickBar) Icons.Default.TouchApp else Icons.Default.PanTool,
                            contentDescription = "Toggle Buttons",
                            tint = if (showBottomClickBar) CyanPrimary else TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. EXPANDABLE FLOATING SPEED SLIDER POPUP
        // ==========================================
        AnimatedVisibility(
            visible = showSpeedSlider,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateCard.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .width(260.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensitivity",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = mouseSensitivity,
                        onValueChange = {
                            mouseSensitivity = it
                            viewModel.prefs.mouseSensitivity = it
                        },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = SlateBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==========================================
        // 5. SMART TACTILE CLICK BAR (BOTTOM GLASS DOCK)
        // ==========================================
        AnimatedVisibility(
            visible = showBottomClickBar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 60 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 60 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder.copy(alpha = 0.7f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("smart_bottom_click_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Click Half
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.onLeftClick()
                            }
                            .testTag("btn_minimal_left_click"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LEFT CLICK",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Sleek Center Divider & Middle Click
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SlateCard)
                            .clickable { viewModel.onMiddleClick() }
                            .testTag("btn_minimal_middle_click"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MID",
                            color = CyanPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Right Click Half
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.onRightClick()
                            }
                            .testTag("btn_minimal_right_click"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RIGHT CLICK",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. SLIDE-UP QUICK PC SHORTCUTS SHEET
        // ==========================================
        AnimatedVisibility(
            visible = showQuickKeysSheet,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 200 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 200 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK PC SHORTCUTS",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = { showQuickKeysSheet = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 1: Common Keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MinimalPillKey("Esc", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("esc") }
                        MinimalPillKey("Tab", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("tab") }
                        MinimalPillKey("Win ❖", modifier = Modifier.weight(1.1f)) { viewModel.sendKeyPress("win") }
                        MinimalPillKey("Enter ↵", modifier = Modifier.weight(1.2f)) { viewModel.sendKeyPress("enter") }
                        MinimalPillKey("⌫ Del", modifier = Modifier.weight(1f)) { viewModel.sendKeyPress("backspace") }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Productivity Hotkeys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MinimalPillKey("Ctrl+C", modifier = Modifier.weight(1f)) { viewModel.sendHotkey(listOf("ctrl", "c")) }
                        MinimalPillKey("Ctrl+V", modifier = Modifier.weight(1f)) { viewModel.sendHotkey(listOf("ctrl", "v")) }
                        MinimalPillKey("Ctrl+Z", modifier = Modifier.weight(1f)) { viewModel.sendHotkey(listOf("ctrl", "z")) }
                        MinimalPillKey("Alt+Tab", modifier = Modifier.weight(1.1f)) { viewModel.sendHotkey(listOf("alt", "tab")) }
                        MinimalPillKey("Win+D", modifier = Modifier.weight(1f)) { viewModel.sendHotkey(listOf("win", "d")) }
                    }
                }
            }
        }

        // Smart Banner: PC Text Field Active
        AnimatedVisibility(
            visible = isPcFieldActive && !isFloatingKeyboardOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -30 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 54.dp)
        ) {
            Surface(
                onClick = { viewModel.openFloatingKeyboard() },
                shape = RoundedCornerShape(20.dp),
                color = SlateCard.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("banner_pc_field_active")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TechGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PC Text Field Active • Tap to Type",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 7. FLOATING SOFT KEYBOARD TYPING BAR
        // ==========================================
        FloatingTypingBar(
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MinimalPillKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = SlateSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
