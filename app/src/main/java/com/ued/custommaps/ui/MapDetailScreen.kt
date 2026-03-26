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
import coil.compose.AsyncImage // Vitamin cho StopList
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

    // Data từ Room
    val allJourneys by viewModel.journeys.observeAsState(initial = emptyList())
    val currentJourney = allJourneys.find { it.id == journeyId }
    val trackPoints by viewModel.getTrackPoints(journeyId).observeAsState(initial = emptyList())
    val stopPoints by viewModel.getStopPoints(journeyId).observeAsState(initial = emptyList())

    val isTracking = viewModel.isTracking.value

    // Tham chiếu điều khiển Map
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Trạng thái UI
    var showStopDialog by remember { mutableStateOf(false) }
    var stopNote by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showStopList by remember { mutableStateOf(false) }

    // --- LAUNCHER LẤY ẢNH TỪ GALLERY ---
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    // Quản lý vòng đời Map
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { mapViewInstance?.onResume(); locationOverlayRef?.enableMyLocation() }
                Lifecycle.Event.ON_PAUSE -> { mapViewInstance?.onPause(); locationOverlayRef?.disableMyLocation() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentJourney?.title ?: "Bản đồ") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    // Nút Ghi/Dừng
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
                        Text(if (isTracking) " Tạm dừng" else " Ghi")
                    }

                    // Nút Thêm điểm dừng
                    Button(onClick = { showStopDialog = true }) { Icon(Icons.Default.AddLocation, null); Text(" Thêm điểm") }

                    // Nút Mở StopList
                    FilledTonalIconButton(onClick = { showStopList = true }) { Icon(Icons.Default.List, null) }
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

                        // --- 1. CHỐT ICON MŨI TÊN TAM GIÁC (VL Tam Giác) ---
                        // Lấy icon mũi tên mặc định, ép cho cả trạng thái đứng yên và di chuyển
                        val arrowIcon = getBitmapFromVector(ctx, android.R.drawable.ic_menu_mylocation)
                        if (arrowIcon != null) {
                            overlay.setPersonIcon(arrowIcon) // Đứng yên cũng hiện mũi tên
                            overlay.setDirectionArrow(arrowIcon, arrowIcon) // Di chuyển hiện mũi tên
                        }

                        overlay.enableMyLocation()
                        overlays.add(overlay)
                        locationOverlayRef = overlay
                        mapViewInstance = this

                        currentJourney?.let { controller.setZoom(17.5); controller.setCenter(GeoPoint(it.startLat, it.startLon)) }
                    }
                },
                update = { mapView ->
                    // --- 2. VẼ POLYLINE THEO SEGMENT (Chống đường kẻ ma) ---
                    if (trackPoints.isNotEmpty()) {
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
                    }

                    // --- 3. HIỂN THỊ ĐIỂM DỪNG (MARKER) TRÊN MAP ---
                    // Xóa ghim cũ, vẽ ghim mới
                    mapView.overlays.removeAll { it is Marker }
                    stopPoints.forEach { stop ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(stop.latitude, stop.longitude)
                            title = stop.note
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            // Tạo cửa sổ thông tin xịn (Phase 2)
                            snippet = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(stop.timestamp))

                            // Load ảnh thumbnail vào InfoWindow nếu có (Cần config InfoWindow phức tạp hơn, Phase 2)
                        }
                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate() // Ép vẽ lại
                }
            )

            // Nút định vị nhanh
            Surface(modifier = Modifier.padding(16.dp).size(56.dp).align(Alignment.TopEnd), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp) {
                IconButton(onClick = {
                    locationOverlayRef?.myLocation?.let { mapViewInstance?.controller?.animateTo(it); mapViewInstance?.controller?.setZoom(18.5) }
                }) { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }

    // --- 4. DIALOG THÊM ĐIỂM DỪNG (GALLERY) ---
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Thêm điểm dừng mới") },
            text = {
                Column {
                    OutlinedTextField(value = stopNote, onValueChange = { stopNote = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    // Nút mở Gallery
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Text(if (selectedImageUri == null) " Chọn ảnh từ Gallery" else " Đã chọn ảnh")
                    }

                    // Hiển thị thumbnail ảnh đã chọn
                    selectedImageUri?.let { uri ->
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).align(Alignment.CenterHorizontally),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val myLoc = locationOverlayRef?.myLocation
                    if (myLoc != null) {
                        viewModel.addStopPoint(journeyId, myLoc.latitude, myLoc.longitude, stopNote, selectedImageUri)
                        showStopDialog = false; stopNote = ""; selectedImageUri = null
                    }
                }) { Text("Lưu điểm") }
            }
        )
    }

    // --- 5. STOPLIST (BOTTOM SHEET QUẢN LÝ ĐIỂM DỪNG) ---
    if (showStopList) {
        ModalBottomSheet(
            onDismissRequest = { showStopList = false },
            sheetState = sheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Danh sách điểm dừng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                if (stopPoints.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có điểm dừng nào được tạo.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(stopPoints) { stop ->
                            StopListItem(
                                stop = stop,
                                onClick = {
                                    // Bấm vào là bay về điểm đó trên Map
                                    mapViewInstance?.controller?.animateTo(GeoPoint(stop.latitude, stop.longitude))
                                    mapViewInstance?.controller?.setZoom(18.5)
                                    // Đóng sheet
                                    showStopList = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp)) // Padding dưới
            }
        }
    }
}

// Komponent hiển thị 1 item trong StopList
@Composable
fun StopListItem(stop: StopPointEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Ảnh thumbnail
            if (stop.imagePath != null) {
                AsyncImage(
                    model = stop.imagePath,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.size(60.dp).background(Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Note và thời gian
            Column(Modifier.weight(1f)) {
                Text(stop.note, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                val time = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(stop.timestamp))
                Text(time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

// Hàm hỗ trợ lấy Bitmap (giữ nguyên)
fun getBitmapFromVector(context: android.content.Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).also { drawable.setBounds(0, 0, it.width, it.height); drawable.draw(it) }
    return bitmap
}