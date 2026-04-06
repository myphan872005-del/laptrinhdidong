package com.ued.custommaps.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ued.custommaps.TrackingService
import com.ued.custommaps.models.CustomMarker
import com.ued.custommaps.models.MapStyle
import com.ued.custommaps.viewmodel.MapViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDetailScreen(mapId: String, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current

    // 🌟 THEO DÕI DỮ LIỆU TỪ FLOW
    val maps by viewModel.maps.collectAsState()
    val map = remember(maps) { maps.find { it.id == mapId } }

    // Log để kiểm tra Recomposition
    Log.d("DEBUG_APP", "UI: MapDetailScreen vẽ lại - ID: $mapId, isTracking: ${map?.isTracking}, Tọa độ: ${map?.polyline?.size}")

    if (map == null) {
        Log.e("DEBUG_APP", "UI: Không tìm thấy map ID: $mapId")
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    var showDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var markerTitle by remember { mutableStateOf("") }
    var markerDesc by remember { mutableStateOf("") }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        launcher.launch(perms.toTypedArray())
    }

    // Lắng nghe tín hiệu từ Service (Dùng Broadcast làm cú hích cuối cùng)
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("DEBUG_APP", "UI: Nhận được tín hiệu TRACKING_UPDATE từ Service")
                viewModel.loadMaps()
            }
        }
        val filter = android.content.IntentFilter("TRACKING_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(map.title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.updatePolyline(mapId, emptyList()) }) {
                        Icon(Icons.Default.DeleteSweep, null, tint = Color.Red)
                    }
                    IconButton(onClick = { showStyleMenu = true }) {
                        Icon(Icons.Default.Layers, null)
                    }
                    DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                        DropdownMenuItem(text = { Text("Bình thường") }, onClick = { viewModel.changeMapStyle(MapStyle.NORMAL); showStyleMenu = false })
                        DropdownMenuItem(text = { Text("Vệ tinh") }, onClick = { viewModel.changeMapStyle(MapStyle.SATELLITE); showStyleMenu = false })
                        DropdownMenuItem(text = { Text("Địa hình") }, onClick = { viewModel.changeMapStyle(MapStyle.TERRAIN); showStyleMenu = false })
                    }
                    IconButton(onClick = { if (hasPermission) moveToLocation(context, mapView) }, enabled = hasPermission) {
                        Icon(Icons.Default.MyLocation, null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(bottom = 60.dp),
                onClick = {
                    if (map.isTracking) {
                        Log.d("DEBUG_APP", "UI: Nhấn Dừng - Đang gửi lệnh cho Service")
                        context.startService(Intent(context, TrackingService::class.java).apply { action = "STOP_TRACKING" })
                        viewModel.updateTrackingStatus(mapId, false)
                    } else {
                        Log.d("DEBUG_APP", "UI: Nhấn Bắt đầu - Đang gửi lệnh cho Service")
                        val startIntent = Intent(context, TrackingService::class.java).apply { putExtra("MAP_ID", mapId) }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(startIntent)
                        else context.startService(startIntent)
                        viewModel.updateTrackingStatus(mapId, true)
                    }
                },
                containerColor = if (map.isTracking) Color(0xFFE57373) else Color(0xFF81C784),
                contentColor = Color.White,
                icon = { Icon(if (map.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, null) },
                text = { Text(if (map.isTracking) "Dừng ghi" else "Bắt đầu ghi") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OSMMapView(
                modifier = Modifier.fillMaxSize(),
                markers = map.markers,
                polyline = map.polyline,
                mapStyle = viewModel.mapStyle.value,
                onMapLongClick = { geoPoint ->
                    selectedLocation = geoPoint
                    showDialog = true
                },
                onMapReady = { mapView = it }
            )

            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "${map.markers.size} điểm dừng | ${map.polyline.size} tọa độ",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showDialog && selectedLocation != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Thêm địa điểm") },
            text = {
                Column {
                    OutlinedTextField(value = markerTitle, onValueChange = { markerTitle = it }, label = { Text("Tiêu đề") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = markerDesc, onValueChange = { markerDesc = it }, label = { Text("Mô tả") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (markerTitle.isNotBlank()) {
                        viewModel.addMarkerToMap(mapId, CustomMarker(latitude = selectedLocation!!.latitude, longitude = selectedLocation!!.longitude, title = markerTitle, description = markerDesc))
                        showDialog = false
                    }
                }) { Text("Lưu") }
            }
        )
    }
}

private fun moveToLocation(context: Context, mapView: MapView?) {
    val fused = LocationServices.getFusedLocationProviderClient(context)
    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            location?.let {
                mapView?.controller?.apply {
                    setZoom(18.0)
                    animateTo(GeoPoint(it.latitude, it.longitude))
                }
            }
        }
    }
}