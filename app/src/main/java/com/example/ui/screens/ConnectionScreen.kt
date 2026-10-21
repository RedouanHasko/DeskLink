package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.DiscoveredPc
import com.example.model.SavedDevice
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
import com.example.viewmodel.TransportMode

@Composable
fun ConnectionScreen(
    viewModel: MainViewModel,
    onOpenServerDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.effectiveConnectionState.collectAsState()
    val latencyMs by viewModel.client.latencyMs.collectAsState()
    val discoveredPcs by viewModel.lanDiscovery.discoveredPcs.collectAsState()
    val isScanning by viewModel.lanDiscovery.isScanning.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()

    val currentTransport by viewModel.transportMode.collectAsState()
    val pairedBtDevices by viewModel.bluetoothManager.pairedDevices.collectAsState()
    val isBtEnabled by viewModel.bluetoothManager.isBluetoothEnabled.collectAsState()

    var manualIp by remember { mutableStateOf(viewModel.prefs.lastIp) }
    var manualPort by remember { mutableStateOf(viewModel.prefs.lastPort.toString()) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.bluetoothManager.refreshPairedDevices()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
            .testTag("connection_screen")
    ) {
        // Hero Active Connection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentTransport == TransportMode.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Laptop,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (connectionState) {
                                    is ConnectionState.Connected -> (connectionState as ConnectionState.Connected).pcName
                                    is ConnectionState.Connecting -> "Connecting..."
                                    is ConnectionState.DemoMode -> "Demo Mode (Virtual PC)"
                                    is ConnectionState.Error -> "Connection Failed"
                                    ConnectionState.Disconnected -> "Not Connected"
                                },
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (connectionState) {
                                    is ConnectionState.Connected -> "${(connectionState as ConnectionState.Connected).ip} • ${(connectionState as ConnectionState.Connected).os}"
                                    is ConnectionState.Connecting -> "${(connectionState as ConnectionState.Connecting).ip}"
                                    is ConnectionState.DemoMode -> "Touchpad & Keyboard Practice Mode"
                                    is ConnectionState.Error -> (connectionState as ConnectionState.Error).message
                                    ConnectionState.Disconnected -> if (currentTransport == TransportMode.BLUETOOTH) "Select a Bluetooth PC below" else "Select a Wi-Fi PC or enter IP below"
                                },
                                color = when (connectionState) {
                                    is ConnectionState.Connected -> TechGreen
                                    is ConnectionState.Connecting -> TechAmber
                                    is ConnectionState.Error -> TechRed
                                    else -> TextSecondary
                                },
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Ping Latency badge
                    if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.DemoMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TechGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentTransport == TransportMode.BLUETOOTH) "Bluetooth" else "${latencyMs}ms",
                                    color = TechGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.DemoMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard, contentColor = TechRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TechRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Connection Instructions & App Settings Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📖 Connection Instructions & Settings",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Step-by-step pairing guide, firewall help & controls",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_open_instructions_settings")
                ) {
                    Text("Read Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Get Companion Server Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CyanContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Windows 10/11 Companion Server",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supports both Wi-Fi and Bluetooth SPP connections",
                        color = CyanPrimary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onOpenServerDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_get_server_guide")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Server Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // CONNECTION MODE SELECTOR: WI-FI vs BLUETOOTH
        // ==========================================
        Text(
            text = "SELECT CONNECTION METHOD",
            color = CyanPrimary,
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                onClick = { viewModel.setTransportMode(TransportMode.WIFI) },
                shape = RoundedCornerShape(10.dp),
                color = if (currentTransport == TransportMode.WIFI) CyanPrimary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (currentTransport == TransportMode.WIFI) SlateBackground else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wi-Fi / Hotspot",
                        color = if (currentTransport == TransportMode.WIFI) SlateBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                onClick = {
                    viewModel.setTransportMode(TransportMode.BLUETOOTH)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        viewModel.bluetoothManager.refreshPairedDevices()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                color = if (currentTransport == TransportMode.BLUETOOTH) CyanPrimary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (currentTransport == TransportMode.BLUETOOTH) SlateBackground else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bluetooth (Direct)",
                        color = if (currentTransport == TransportMode.BLUETOOTH) SlateBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (currentTransport == TransportMode.BLUETOOTH) {
            // ==========================================
            // BLUETOOTH PAIRING & CONNECT VIEW
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAIRED BLUETOOTH DEVICES",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            viewModel.bluetoothManager.refreshPairedDevices()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!isBtEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = TechAmber, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Bluetooth is Turned Off", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Enable Bluetooth on your phone and PC to connect directly.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            } else if (pairedBtDevices.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No Paired PC Found", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "1. Go to Windows Settings -> Bluetooth & Devices.\n2. Pair your phone with your PC.\n3. Return here and tap Refresh to connect!",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                pairedBtDevices.forEach { dev ->
                    Surface(
                        onClick = { viewModel.connectBluetooth(dev.address, dev.name) },
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = dev.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = dev.address, color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.connectBluetooth(dev.address, dev.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Connect", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

        } else {
            // ==========================================
            // WI-FI / HOTSPOT SECTION
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUTO-DETECTED ON WI-FI",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.lanDiscovery.startScan() },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (discoveredPcs.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No PC detected yet on this Wi-Fi network",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Start desklink_server.py on your PC or connect manually below.",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                discoveredPcs.forEach { pc ->
                    Surface(
                        onClick = { viewModel.connectTo(pc.ip, pc.port, pc.name) },
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = pc.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "${pc.ip}:${pc.port} • ${pc.os}", color = CyanPrimary, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.connectTo(pc.ip, pc.port, pc.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Connect", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Connection Form
            Text(
                text = "DIRECT IP CONNECTION",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Laptop IP Address", fontSize = 12.sp) },
                            placeholder = { Text("e.g. 192.168.1.150") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SlateBackground,
                                unfocusedContainerColor = SlateBackground
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(2.5f)
                                .testTag("input_manual_ip")
                        )

                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SlateBackground,
                                unfocusedContainerColor = SlateBackground
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_manual_port")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val port = manualPort.toIntOrNull() ?: 8765
                                viewModel.connectTo(manualIp.trim(), port)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SlateBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("btn_connect_manual")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect to PC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.startDemoMode() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_practice_demo_mode")
                        ) {
                            Text("Practice Mode", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Saved Devices List
            if (savedDevices.isNotEmpty()) {
                Text(
                    text = "SAVED COMPUTERS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                savedDevices.forEach { dev ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.connectToSaved(dev) }
                            ) {
                                Text(text = dev.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "${dev.ip}:${dev.port}", color = TextSecondary, fontSize = 11.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { viewModel.connectToSaved(dev) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateSurface, contentColor = CyanPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deleteSavedDevice(dev.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
