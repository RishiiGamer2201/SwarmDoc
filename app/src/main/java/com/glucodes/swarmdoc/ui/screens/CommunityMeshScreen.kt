package com.glucodes.swarmdoc.ui.screens
import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.MeshViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CommunityMeshScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MeshViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val isSyncing by viewModel.isSyncing.collectAsState()
    val clusters by viewModel.clusters.collectAsState()
    val uniqueDevices by viewModel.uniqueDeviceCount.collectAsState()
    val lastSync by viewModel.lastSyncTime.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val epidemicTrend by viewModel.epidemicTrend.collectAsState()

    val scrollState = rememberScrollState()
    
    // BLE permissions
    val blePermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (blePermissions.allPermissionsGranted) {
            viewModel.startBleScan()
        } else {
            blePermissions.launchMultiplePermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        // Top Bar with back button
        TopAppBar(
            title = { Text(strings.communityHealth, fontWeight = FontWeight.Bold, color = ForestGreen) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top section Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(ForestGreen)
            ) {
                VillageMap(
                    isSyncing = isSyncing,
                    activeClusters = clusters.isNotEmpty()
                )
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(strings.villageHealthMemory, style = MaterialTheme.typography.displaySmall, color = White, fontWeight = FontWeight.Bold)
                    if (!blePermissions.allPermissionsGranted) {
                        Text("BLE permissions required for real sync.", color = TurmericGold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

        Spacer(modifier = Modifier.height(24.dp))

        // Sync Status Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(strings.meshNetworkStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${discoveredDevices.size} ${strings.peersDiscovered} (${uniqueDevices} ${strings.totalMemory})", style = MaterialTheme.typography.bodyMedium, color = SageGreen)
                    }
                    val timeStr = if (lastSync != null) SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastSync!!)) else "Never"
                    Text("${strings.lastSync}: $timeStr", style = MaterialTheme.typography.labelSmall, color = WarmGrey)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.triggerRealSync() },
                    modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = TurmericGold, contentColor = CharcoalBlack), shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSyncing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Merging Protocol...", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(16.dp))
                            CircularProgressIndicator(color = CharcoalBlack, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Icon(Icons.Rounded.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (discoveredDevices.isEmpty()) "Simulate Sync (No Peers)" else strings.syncActivePeers, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 7-Day Epidemic Trend
        if (epidemicTrend.isNotEmpty()) {
            Text(strings.sevenDayEpidemicTimeline, style = MaterialTheme.typography.titleLarge, color = CharcoalBlack, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    epidemicTrend.forEach { (condition, counts) ->
                        Text(condition, style = MaterialTheme.typography.labelMedium, color = WarmGrey, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        EpidemicBarChart(data = counts)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Clusters
        Text(strings.activeSymptomClusters, style = MaterialTheme.typography.titleLarge, color = CharcoalBlack, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))

        if (clusters.isEmpty()) {
            Text(strings.noClustersDetected, color = WarmGrey, modifier = Modifier.padding(horizontal = 24.dp))
        } else {
            clusters.forEach { cluster ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(if (cluster.isCritical) CoralRed.copy(alpha = 0.2f) else WarmAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${cluster.caseCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (cluster.isCritical) CoralRed else WarmAmber)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cluster.condition, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                            if (cluster.isCritical) Text(strings.outbreakThresholdCrossed, style = MaterialTheme.typography.bodySmall, color = CoralRed)
                        }
                        Icon(if (cluster.isCritical) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, contentDescription = null, tint = if (cluster.isCritical) CoralRed else SageGreen, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun EpidemicBarChart(data: List<Int>) {
    val maxCount = maxOf(1, data.maxOrNull() ?: 1)
    
    Row(modifier = Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        data.forEachIndexed { index, count ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                val heightPercent = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(heightPercent).background(if (count >= 3) CoralRed else TurmericGold, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                Text(if (index == 6) "Today" else "-${6-index}d", style = MaterialTheme.typography.bodySmall, color = WarmGrey, modifier = Modifier.padding(top=4.dp))
            }
        }
    }
}

@Composable
fun VillageMap(isSyncing: Boolean, activeClusters: Boolean) {
    val workerPosition = remember { LatLng(19.8134, 85.8312) } // Puri, Odisha
    val clusterPosition = remember { LatLng(19.8250, 85.8200) }
    val peerPosition = remember { LatLng(19.8100, 85.8400) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var showMapIssueHint by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(workerPosition, 14f)
    }

    LaunchedEffect(Unit) {
        delay(7000)
        if (!isMapLoaded) showMapIssueHint = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL,
                isBuildingEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true
            ),
            onMapLoaded = { isMapLoaded = true }
        ) {
            Marker(
                state = MarkerState(position = workerPosition),
                title = "You (ASHA Worker)"
            )

            if (activeClusters) {
                Circle(
                    center = clusterPosition,
                    radius = 1000.0,
                    fillColor = Color(0x32FF0000),
                    strokeColor = Color(0x64FF0000),
                    strokeWidth = 4f
                )
                Marker(
                    state = MarkerState(position = clusterPosition),
                    title = "Cluster Hub"
                )
            }

            if (isSyncing) {
                Polyline(
                    points = listOf(workerPosition, peerPosition),
                    color = Color(0xFFFFC857),
                    width = 6f
                )
            }
        }

        if (!isMapLoaded) {
            CircularProgressIndicator(
                color = TurmericGold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (showMapIssueHint && !isMapLoaded) {
            Text(
                text = "Map tiles not loading. Check Google Maps key restrictions (package + SHA-1).",
                color = White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            )
        }
    }
}

/**
 * Fallback Canvas-based map when network is not available.
 * Shows a schematic village layout with worker position and cluster zones.
 */
@Composable
fun OfflineMapFallback(isSyncing: Boolean, activeClusters: Boolean) {
    val pulseAnim = rememberInfiniteTransition(label = "mapPulse")
    val pulseRadius by pulseAnim.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "radius"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2

            // Background grid (roads)
            val gridColor = TurmericGold.copy(alpha = 0.15f)
            for (i in 1..4) {
                drawLine(gridColor, Offset(w * i / 5, 0f), Offset(w * i / 5, h), strokeWidth = 2f)
                drawLine(gridColor, Offset(0f, h * i / 5), Offset(w, h * i / 5), strokeWidth = 2f)
            }

            // Village boundary
            drawRect(
                color = TurmericGold.copy(alpha = 0.3f),
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.8f, h * 0.8f),
                style = Stroke(width = 3f)
            )

            // Worker position (center)
            drawCircle(TurmericGold, radius = 20f * pulseRadius, center = Offset(cx, cy), alpha = 0.3f)
            drawCircle(TurmericGold, radius = 10f, center = Offset(cx, cy))

            // Cluster zone (if active)
            if (activeClusters) {
                val clusterCenter = Offset(cx - w * 0.2f, cy - h * 0.15f)
                drawCircle(CoralRed.copy(alpha = 0.15f), radius = 80f, center = clusterCenter)
                drawCircle(CoralRed, radius = 80f, center = clusterCenter, style = Stroke(width = 2f))
                drawCircle(CoralRed, radius = 8f, center = clusterCenter)
            }

            // Peer worker dots
            val peerPositions = listOf(
                Offset(cx + w * 0.15f, cy - h * 0.1f),
                Offset(cx - w * 0.1f, cy + h * 0.2f),
                Offset(cx + w * 0.25f, cy + h * 0.15f),
            )
            peerPositions.forEach { pos ->
                drawCircle(SageGreen, radius = 6f, center = pos)
            }

            // Sync lines
            if (isSyncing) {
                peerPositions.forEach { pos ->
                    drawLine(
                        TurmericGold.copy(alpha = 0.6f),
                        start = Offset(cx, cy),
                        end = pos,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Offline badge
        Text(
            "📍 Offline Map View",
            style = MaterialTheme.typography.labelSmall,
            color = White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
    }
}
