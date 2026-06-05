package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HandshakeHistory
import com.example.data.SniProfile
import com.example.viewmodel.HandshakeUiState
import com.example.viewmodel.VpnViewModel
import kotlinx.coroutines.launch

sealed class ScreenTab(val title: String, val iconFilled: androidx.compose.ui.graphics.vector.ImageVector, val iconOutlined: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : ScreenTab("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Handshake : ScreenTab("Diagnostics", Icons.Filled.NetworkCheck, Icons.Outlined.NetworkCheck)
    object Profiles : ScreenTab("Profiles", Icons.Filled.HistoryEdu, Icons.Outlined.HistoryEdu)
    object History : ScreenTab("History", Icons.Filled.History, Icons.Outlined.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf<ScreenTab>(ScreenTab.Dashboard) }

    val tabs = listOf(
        ScreenTab.Dashboard,
        ScreenTab.Handshake,
        ScreenTab.Profiles,
        ScreenTab.History
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Security App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VanetSpoofing",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                                contentDescription = tab.title,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ScreenTab.Dashboard -> DashboardTab(viewModel)
                ScreenTab.Handshake -> HandshakeTab(viewModel)
                ScreenTab.Profiles -> ProfilesTab(viewModel)
                ScreenTab.History -> HistoryTab(viewModel)
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: VpnViewModel) {
    val isRunning by viewModel.isProxyRunning.collectAsStateWithLifecycle()
    val port by viewModel.proxyPort.collectAsStateWithLifecycle()
    val spoofSni by viewModel.proxyHostSni.collectAsStateWithLifecycle()
    val logs by viewModel.proxyLogs.collectAsStateWithLifecycle()

    var inputPort by remember { mutableStateOf(port.toString()) }
    var inputSni by remember { mutableStateOf(spoofSni) }
    var expandedHelp by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proxy_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
                                     else MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Sensors else Icons.Filled.SensorsOff,
                            contentDescription = "Status indicator",
                            modifier = Modifier.size(40.dp),
                            tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isRunning) "SPOOF PROXY RUNNING" else "SPOOF PROXY IDLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isRunning) "Routing local client traffic dynamically through Localhost Proxy"
                               else "Connect external clients or browsers to route SNI handshakes",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val targetPort = inputPort.toIntOrNull() ?: 8080
                            viewModel.toggleProxy(targetPort, inputSni)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("toggle_proxy_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Stop Proxy" else "Start Proxy"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Stop SNI Proxy Server" else "Start SNI Proxy Server",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Proxy Server Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = inputSni,
                        onValueChange = { inputSni = it },
                        label = { Text("Global Overridden SNI Target") },
                        placeholder = { Text("e.g. www.google.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.AltRoute, contentDescription = "SNI") },
                        enabled = !isRunning
                    )

                    OutlinedTextField(
                        value = inputPort,
                        onValueChange = { inputPort = it },
                        label = { Text("Local Bound Port") },
                        placeholder = { Text("8080") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Filled.SettingsEthernet, contentDescription = null) },
                        enabled = !isRunning
                    )
                }
            }
        }

        // Live Log Terminal
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030A12)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isRunning) Color(0xFF02F896) else Color(0xFFFF2A7A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROXY TERMINAL LOG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8895A5)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearProxyLogs() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear terminal logs",
                                tint = Color(0xFF8895A5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Terminal Idle.\nPress Start to begin telemetry capture.",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5B6B),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    color = if (log.contains("error", ignoreCase = true)) Color(0xFFFF2A7A)
                                            else if (log.contains("started", ignoreCase = true)) Color(0xFF02F896)
                                            else Color(0xFFE3E8EC),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Integration Help Details
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedHelp = !expandedHelp },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = "How to use", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "How to test Local Proxy Server?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = if (expandedHelp) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle instructions"
                        )
                    }

                    if (expandedHelp) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "1. Verify the proxy is active (start proxy above).\n" +
                                   "2. On your testing host (Android or external computer on the same Wi-Fi), go to LAN Settings.\n" +
                                   "3. Bind HTTP proxy Host address to: Your Android App Local IP (or 127.0.0.1 for local testing).\n" +
                                   "4. Bind Port to the port entered above (default: 8080).\n" +
                                   "5. Try loaded targets. Outbound TLS handshakes will automatically route using $spoofSni spoof headers, routing safely around restrictive CDNs.",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandshakeTab(viewModel: VpnViewModel) {
    val state by viewModel.handshakeState.collectAsStateWithLifecycle()

    var inputHost by remember { mutableStateOf("google.com") }
    var inputPort by remember { mutableStateOf("443") }
    var inputSni by remember { mutableStateOf("www.wikipedia.org") }
    var trustAll by remember { mutableStateOf(true) }

    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "TLS Handshake Spoof Tester",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Manually negotiate TLS handshakes to verify host SNI routing, detect censorship endpoints, and discover bypassed ports.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Tester Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputHost,
                        onValueChange = { inputHost = it },
                        label = { Text("Target Host Destination") },
                        placeholder = { Text("e.g. google.com or IP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Computer, contentDescription = null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputPort,
                            onValueChange = { inputPort = it },
                            label = { Text("Port") },
                            placeholder = { Text("443") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Filled.SettingsEthernet, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = inputSni,
                            onValueChange = { inputSni = it },
                            label = { Text("Spoofed SNI Extension") },
                            placeholder = { Text("www.bing.com") },
                            modifier = Modifier.weight(2.0f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.AltRoute, contentDescription = null) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Bypass SSL Trust Verification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Permit self-signed or invalid cert handshakes",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = trustAll,
                            onCheckedChange = { trustAll = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val portInt = inputPort.toIntOrNull() ?: 443
                            viewModel.performHandshakeCheck(inputHost, portInt, inputSni, trustAll)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("test_handshake_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = state !is HandshakeUiState.Loading
                    ) {
                        if (state is HandshakeUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Bolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Handshake Test", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Interactive Result Status
        item {
            AnimatedContent(
                targetState = state,
                label = "Result anim"
            ) { targetState ->
                when (targetState) {
                    is HandshakeUiState.Idle -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "No execution history details active.\nInput details and tap simulation above to begin diagnostic validation.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is HandshakeUiState.Loading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Performing Client Hello TCP/TLS Negotiation...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bypassing firewalls with custom SNI host headers",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    is HandshakeUiState.Success -> {
                        val result = targetState.result
                        HandshakeResultsCard(result = result, onCopy = { text ->
                            clipboard.setText(AnnotatedString(text))
                        })
                    }
                    is HandshakeUiState.Error -> {
                        targetState.partialResult?.let { partial ->
                            HandshakeResultsCard(result = partial, onCopy = { text ->
                                clipboard.setText(AnnotatedString(text))
                            })
                        } ?: Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "SSL Handshake Fault",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = targetState.message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HandshakeResultsCard(
    result: HandshakeHistory,
    onCopy: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSuccess) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (result.isSuccess) Color(0xFF02F896) else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.isSuccess) "TLS Handshake Negotiated" else "Negotiation Failed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (result.isSuccess) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "${result.latencyMs} ms",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultRow(label = "Target Host", value = "${result.targetHost}:${result.targetPort}")
                ResultRow(label = "Injected spoofed SNI", value = result.spoofedSni)
                
                if (result.isSuccess) {
                    ResultRow(label = "Verified Protocol", value = result.protocol ?: "Unknown")
                    ResultRow(label = "Cipher Suite Suite", value = result.cipherSuite ?: "Unknown", isMonospace = true)
                    ResultRow(label = "Subject Issuer Name", value = result.certIssuer ?: "Unknown")
                    ResultRow(label = "Certificate Holder Subject", value = result.certSubject ?: "Unknown")
                } else {
                    ResultRow(label = "Raw Exception Fault", value = result.errorMessage ?: "Timeout / Closed Socket", error = true)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onCopy(
                        "Host: ${result.targetHost}:${result.targetPort}\n" +
                        "Spoofed SNI: ${result.spoofedSni}\n" +
                        "Success: ${result.isSuccess}\n" +
                        "Latency: ${result.latencyMs}ms\n" +
                        "Protocol: ${result.protocol}\n" +
                        "Exception: ${result.errorMessage}"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Diagnostic Telemetry Profile", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, isMonospace: Boolean = false, error: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(2.2f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProfilesTab(viewModel: VpnViewModel) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("443") }
    var sniInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.WebAssetOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No saved profiles found.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Saved SNI Spoof Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Speedily launch template tests with your customized DNS bypass & spoof configurations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(profiles) { profile ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteProfile(profile)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete preset profile",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Host: ${profile.targetHost}:${profile.targetPort}") },
                                    leadingIcon = { Icon(Icons.Filled.Computer, null, modifier = Modifier.size(14.dp)) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text("SNI: ${profile.spoofedSni}") },
                                    leadingIcon = { Icon(Icons.Filled.AltRoute, null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            if (profile.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = profile.notes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.performHandshakeCheck(
                                        profile.targetHost,
                                        profile.targetPort,
                                        profile.spoofedSni,
                                        true
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Execute Handshake Spoof Test", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Add empty padding spacing so FAB won't block items
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Float Actions FAB to open Sheet Creator
        FloatingActionButton(
            onClick = {
                nameInput = ""
                hostInput = ""
                portInput = "443"
                sniInput = ""
                notesInput = ""
                showCreateDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_profile_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add custom profile preset")
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Spoof Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Config Name") },
                            placeholder = { Text("e.g. Tehran CDN Bypass") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("Target Host") },
                            placeholder = { Text("1.1.1.1 or example.com") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { portInput = it },
                                label = { Text("Port") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = sniInput,
                                onValueChange = { sniInput = it },
                                label = { Text("Spoofed SNI Extension") },
                                placeholder = { Text("snidomain.corp") },
                                modifier = Modifier.weight(2.2f)
                            )
                        }

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Notes & details") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && hostInput.isNotBlank() && sniInput.isNotBlank()) {
                                viewModel.saveProfile(
                                    SniProfile(
                                        name = nameInput,
                                        targetHost = hostInput,
                                        targetPort = portInput.toIntOrNull() ?: 443,
                                        spoofedSni = sniInput,
                                        notes = notesInput
                                    )
                                )
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Save Preset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryTab(viewModel: VpnViewModel) {
    val history by viewModel.histories.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Handshake Telemetry Log History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${history.size} historical scans secured in device SQL storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            IconButton(
                onClick = { viewModel.clearAllHistory() },
                enabled = history.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = "Clear absolute history database",
                    tint = if (history.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.HistoryToggleOff,
                        contentDescription = "Zero history items found",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "History logs empty. Begin checks in test diagnostics.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isSuccess) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                             else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                        contentDescription = null,
                                        tint = if (item.isSuccess) Color(0xFF02F896) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${item.targetHost}:${item.targetPort}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                val sfd = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                                Text(
                                    text = sfd.format(java.util.Date(item.timestamp)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Spoofed SNI: ${item.spoofedSni}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (item.errorMessage != null) {
                                        Text(
                                            text = "Fault: ${item.errorMessage}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Badge(
                                    containerColor = if (item.isSuccess) Color(0xFF02F896).copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                    contentColor = if (item.isSuccess) Color(0xFF02F896) else MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = if (item.isSuccess) "${item.latencyMs}ms" else "FAIL",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
