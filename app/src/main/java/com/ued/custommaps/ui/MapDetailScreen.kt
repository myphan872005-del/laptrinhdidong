package com.ued.custommaps.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ued.custommaps.TrackingService
import com.ued.custommaps.data.StopPointEntity
import com.ued.custommaps.viewmodel.MapViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDetailScreen(mapId: String, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val journeyId = mapId.toLongOrNull() ?: -1L

    // Lấy dữ liệu từ ViewModel
    val allJourneys by viewModel.journeys.observeAsState(initial = emptyList())
    val currentJourney = allJourneys.find { it.id == journeyId }
    val trackPoints by viewModel.getTrackPoints(journeyId).observeAsState(initial = emptyList())
    val stopPoints by viewModel.getStopPoints(journeyId).observeAsState(initial = emptyList())
    val isTracking = viewModel.isTracking.value

    // Tham chiếu Map
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // State UI
    var showStopDialog by remember { mutableStateOf(false) }
    var stopNote by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showStopList by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    // Quản lý Lifecycle để tránh tốn pin/rò rỉ bộ nhớ
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapViewInstance?.onResume()
                    locationOverlayRef?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapViewInstance?.onPause()
                    locationOverlayRef?.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewInstance?.onDetach() // Quan trọng để giải phóng tài nguyên
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentJourney?.title ?: "Chi tiết bản đồ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = {
                        viewModel.toggleTracking()
                        val intent = Intent(context, TrackingService::class.java).apply {
                            putExtra("JOURNEY_ID", journeyId)
                            putExtra("SEGMENT_ID", viewModel.currentSegmentId.value)
                            if (isTracking) action = "STOP"
                        }
                        if (!isTracking) context.startForegroundService(intent)
                        else context.stopService(intent)
                    }) {
                        Icon(if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Text(if (isTracking) " Tạm dừng" else " Bắt đầu ghi")
                    }

                    Button(onClick = { showStopDialog = true }) {
                        Icon(Icons.Default.AddLocation, null)
                        Text(" Thêm điểm")
                    }

                    FilledTonalIconButton(onClick = { showStopList = true }) {
                        Icon(Icons.Default.List, null)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        val arrowIcon = getBitmapFromVector(ctx, android.R.drawable.ic_menu_mylocation)
                        if (arrowIcon != null) {
                            overlay.setPersonIcon(arrowIcon)
                            overlay.setDirectionArrow(arrowIcon, arrowIcon)
                        }

                        overlay.enableMyLocation()
                        overlays.add(overlay)
                        locationOverlayRef = overlay
                        mapViewInstance = this

                        currentJourney?.let {
                            controller.setZoom(17.5)
                            controller.setCenter(GeoPoint(it.startLat, it.startLon))
                        }
                    }
                },
                update = { mapView ->
                    // 1. Vẽ Polyline
                    mapView.overlays.removeAll { it is Polyline }
                    trackPoints.groupBy { it.segmentId }.forEach { (_, points) ->
                        if (points.size >= 2) {
                            val polyline = Polyline().apply {
                                setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
                                outlinePaint.color = android.graphics.Color.RED
                                outlinePaint.strokeWidth = 10f
                            }
                            mapView.overlays.add(polyline)
                        }
                    }

                    // 2. Vẽ Markers (Điểm dừng)
                    mapView.overlays.removeAll { it is Marker }
                    stopPoints.forEach { stop ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(stop.latitude, stop.longitude)
                            title = stop.note
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            snippet = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()).format(Date(stop.timestamp))
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )

            // Nút My Location
            Surface(
                modifier = Modifier.padding(16.dp).size(56.dp).align(Alignment.TopEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                IconButton(onClick = {
                    locationOverlayRef?.myLocation?.let {
                        mapViewInstance?.controller?.animateTo(it)
                        mapViewInstance?.controller?.setZoom(18.5)
                    }
                }) {
                    Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Dialog Thêm Điểm Dừng
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Lưu dấu chân") },
            text = {
                Column {
                    OutlinedTextField(
                        value = stopNote,
                        onValueChange = { stopNote = it },
                        label = { Text("Bạn đang thấy gì?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Text(if (selectedImageUri == null) " Thêm ảnh kỷ niệm" else " Đã chọn 1 ảnh")
                    }
                    selectedImageUri?.let { uri ->
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).align(Alignment.CenterHorizontally),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val myLoc = locationOverlayRef?.myLocation
                    if (myLoc != null) {
                        viewModel.addStopPoint(journeyId, myLoc.latitude, myLoc.longitude, stopNote, selectedImageUri?.toString())
                        Toast.makeText(context, "Đã ghim điểm dừng!", Toast.LENGTH_SHORT).show()
                        // Reset state
                        showStopDialog = false
                        stopNote = ""
                        selectedImageUri = null
                    } else {
                        Toast.makeText(context, "Chờ GPS bắt tín hiệu đã Hoan ơi!", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Ghim ngay") }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Hủy") }
            }
        )
    }

    // StopList Bottom Sheet
    if (showStopList) {
        ModalBottomSheet(onDismissRequest = { showStopList = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Nhật ký hành trình", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (stopPoints.isEmpty()) {
                    Text("Chưa có điểm dừng nào.", modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally), color = Color.Gray)
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(stopPoints) { stop ->
                            StopListItem(stop = stop) {
                                mapViewInstance?.controller?.animateTo(GeoPoint(stop.latitude, stop.longitude))
                                mapViewInstance?.controller?.setZoom(19.0)
                                showStopList = false
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StopListItem(stop: StopPointEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (stop.imagePath != null) {
                AsyncImage(
                    model = stop.imagePath,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.size(64.dp).background(Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ImageNotSupported, null, tint = Color.Gray)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stop.note.ifBlank { "Không có ghi chú" }, fontWeight = FontWeight.Bold)
                Text(SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(stop.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

fun getBitmapFromVector(context: android.content.Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}