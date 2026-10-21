package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ServerScriptDialog
import com.example.ui.components.TopStatusBar
import com.example.ui.screens.ClipboardScreen
import com.example.ui.screens.ConnectionScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.KeyboardScreen
import com.example.ui.screens.MicVoiceScreen
import com.example.ui.screens.ScreenMirrorScreen
import com.example.ui.screens.TrackpadScreen
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeskLinkTheme
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppTab
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeskLinkTheme(darkTheme = true) {
                val showSplash by viewModel.showSplashScreen.collectAsState()
                Crossfade(targetState = showSplash, label = "splash_crossfade") { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onAnimationFinished = { viewModel.dismissSplash() }
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isMirrorFullScreen by viewModel.isMirrorFullScreen.collectAsState()
    val connectionState by viewModel.effectiveConnectionState.collectAsState()
    val latencyMs by viewModel.client.latencyMs.collectAsState()
    val actionLog by viewModel.client.actionLog.collectAsState()
    val showServerDialog by viewModel.showServerDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

    // Handle back button: first exit fullscreen mirror if active, otherwise return to Trackpad
    BackHandler(enabled = isMirrorFullScreen) {
        viewModel.setMirrorFullScreen(false)
    }

    BackHandler(enabled = !isMirrorFullScreen && currentTab != AppTab.TRACKPAD) {
        viewModel.selectTab(AppTab.TRACKPAD)
    }

    Scaffold(
        modifier = if (isMirrorFullScreen) {
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        } else {
            Modifier
                .fillMaxSize()
                .background(SlateBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        },
        topBar = {
            if (!isMirrorFullScreen) {
                TopStatusBar(
                    connectionState = connectionState,
                    latencyMs = latencyMs,
                    actionLog = actionLog,
                    onOpenServerScript = { viewModel.setShowServerDialog(true) },
                    onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                    onConnectClick = { viewModel.selectTab(AppTab.CONNECT) },
                    onDisconnectClick = { viewModel.disconnect() }
                )
            }
        },
        bottomBar = {
            if (!isMirrorFullScreen) {
                NavigationBar(
                    containerColor = SlateSurface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_nav_bar")
                ) {
                    val navItems = listOf(
                        Triple(AppTab.TRACKPAD, "Trackpad", Icons.Default.Mouse),
                        Triple(AppTab.KEYBOARD, "Keyboard", Icons.Default.Keyboard),
                        Triple(AppTab.SCREEN, "Screen", Icons.Default.DesktopWindows),
                        Triple(AppTab.VOICE_MIC, "Voice", Icons.Default.Mic),
                        Triple(AppTab.CLIPBOARD, "Clipboard", Icons.Default.ContentPaste),
                        Triple(AppTab.CONNECT, "Connect", Icons.Default.Sensors)
                    )

                    navItems.forEach { (tab, label, icon) ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanPrimary,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = CyanContainer
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMirrorFullScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { target ->
                when (target) {
                    AppTab.TRACKPAD -> TrackpadScreen(viewModel = viewModel)
                    AppTab.KEYBOARD -> KeyboardScreen(viewModel = viewModel)
                    AppTab.SCREEN -> ScreenMirrorScreen(viewModel = viewModel)
                    AppTab.VOICE_MIC -> MicVoiceScreen(viewModel = viewModel)
                    AppTab.CLIPBOARD -> ClipboardScreen(viewModel = viewModel)
                    AppTab.CONNECT -> ConnectionScreen(
                        viewModel = viewModel,
                        onOpenServerDialog = { viewModel.setShowServerDialog(true) },
                        onOpenSettings = { viewModel.setShowSettingsDialog(true) }
                    )
                }
            }
        }
    }

    if (showServerDialog) {
        ServerScriptDialog(
            onDismiss = { viewModel.setShowServerDialog(false) }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowSettingsDialog(false) },
            onOpenServerScript = {
                viewModel.setShowSettingsDialog(false)
                viewModel.setShowServerDialog(true)
            }
        )
    }
}
