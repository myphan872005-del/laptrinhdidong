@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ued.custommaps.R
import com.ued.custommaps.network.NetworkConfig
import com.ued.custommaps.viewmodel.MapViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.mylocation.*
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun MapDetailScreen(mapId: Long, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 🚀 ĐẢM BẢO BASE URL LÀ STRING
    val baseUrlState by viewModel.currentBaseUrl
    val currentBaseUrl: String = baseUrlState ?: NetworkConfig.BASE_URL

    // 🎯 QUAN SÁT DỮ LIỆU
    val trackPoints by viewModel.getTrackPoints(mapId).observeAsState(initial = emptyList())
    val stopPoints by viewModel.getStopPoints(mapId).observeAsState(initial = emptyList())
    val allJourneys by viewModel.journeys.observeAsState(initial = emptyList())
    val currentJourney = allJourneys.find { it.id == mapId }
    val isTracking = viewModel.isTracking.value
    val mediaForPublish by viewModel.getMediaForPublish(mapId).observeAsState(initial = emptyList())

    // 🛠️ STATES UI
    var showStopList by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showPublishBottomSheet by remember { mutableStateOf(false) }

    // 🔥 BIẾN QUYẾT ĐỊNH: Đã định vị lần đầu xong chưa (Chống bay ra biển)
    var isMapInitialized by remember { mutableStateOf(false) }

    var stopNote by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var publishTitle by remember { mutableStateOf(currentJourney?.title ?: "") }
    var selectedThumbUrl by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { selectedMediaUris = it }

    // =========================================================
    // 🔍 🚀 KHU VỰC SOI LOG: MEDIA_DEBUG
    // =========================================================
    LaunchedEffect(stopPoints) {
        if (stopPoints.isNotEmpty()) {
            Log.d("MEDIA_DEBUG", "================================================")
            Log.d("MEDIA_DEBUG", "📍 KIỂM TRA MEDIA CỦA HÀNH TRÌNH ID: $mapId")
            stopPoints.forEach { item ->
                Log.d("MEDIA_DEBUG", "--- ĐIỂM DỪNG ID: ${item.stopPoint.id} ---")
                Log.d("MEDIA_DEBUG", "📸 Files: ${item.mediaList.map { it.fileUri }}")
            }
            Log.d("MEDIA_DEBUG", "================================================")
        }
    }

    // Khởi tạo Map
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isVerticalMapRepetitionEnabled = false
            isHorizontalMapRepetitionEnabled = false
            controller.setZoom(17.0)
        }
    }

    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            disableFollowLocation() // 🚀 QUAN TRỌNG: Cấm overlay tự giật camera
            setPersonIcon((ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation) as BitmapDrawable).bitmap)
        }
    }

    // =========================================================
    // 🚀 HỆ THỐNG ĐỀ PHÒNG "BAY RA BIỂN" (Chỉ Camera)
    // =========================================================

    // 1. Lệnh Focus từ màn hình Detail quay về
    val focusLoc by viewModel.focusLocation
    LaunchedEffect(focusLoc) {
        focusLoc?.let { (lat, lon) ->
            if (lat != 0.0 && lon != 0.0) {
                mapView.controller.animateTo(GeoPoint(lat, lon))
                mapView.controller.setZoom(18.0)
                isMapInitialized = true
            }
            viewModel.focusLocation.value = null
        }
    }

    // 2. Định vị lần đầu (Initial Jump) - Chỉ chạy 1 lần duy nhất khi có dữ liệu hợp lệ
    LaunchedEffect(trackPoints, locationOverlay.myLocation) {
        if (!isMapInitialized) {
            if (trackPoints.isNotEmpty()) {
                val last = trackPoints.last()
                if (last.latitude != 0.0 && last.longitude != 0.0) {
                    mapView.controller.setCenter(GeoPoint(last.latitude, last.longitude))
                    isMapInitialized = true
                }
            } else if (locationOverlay.myLocation != null) {
                val gps = locationOverlay.myLocation
                if (gps.latitude != 0.0) {
                    mapView.controller.setCenter(gps)
                    isMapInitialized = true
                }
            }
        }
    }

    LaunchedEffect(stopPoints) {
        if (stopPoints.isNotEmpty()) viewModel.loadBitmapsForStopPoints(context, stopPoints)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapView.onResume()
                viewModel.syncTrackingState(context)
            }
            else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentJourney?.title ?: "Hành trình", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        publishTitle = currentJourney?.title ?: ""
                        showPublishBottomSheet = true
                    }) { Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary) }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.toggleTracking(context, mapId) },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isTracking) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (isTracking) Icons.Default.Stop else Icons.Default.RadioButtonChecked, null)
                        Text(if (isTracking) " Dừng" else " Chạy")
                    }
                    FilledTonalButton(onClick = { showStopDialog = true }) {
                        Icon(Icons.Default.AddAPhoto, null)
                        Text(" Check-in")
                    }
                    IconButton(onClick = { showStopList = true }) {
                        Icon(Icons.Default.ListAlt, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    if (mapView.overlays.isEmpty()) mapView.overlays.add(locationOverlay)
                    mapView
                },
                update = { m ->
                    // 🚀 CHỈ VẼ - TUYỆT ĐỐI KHÔNG DÙNG CONTROLLER Ở ĐÂY
                    m.overlays.removeAll { it is Polyline || it is Marker }
                    m.overlays.add(locationOverlay)

                    // Vẽ Polyline (Đường đi)
                    trackPoints.groupBy { it.segmentId }.forEach { (_, pts) ->
                        if (pts.size >= 2) {
                            val line = Polyline(m).apply {
                                setPoints(pts.map { GeoPoint(it.latitude, it.longitude) })
                                outlinePaint.color = android.graphics.Color.parseColor("#4285F4")
                                outlinePaint.strokeWidth = 12f
                            }
                            m.overlays.add(line)
                        }
                    }

                    // Vẽ Markers (Điểm dừng)
                    stopPoints.forEach { item ->
                        val marker = Marker(m).apply {
                            position = GeoPoint(item.stopPoint.latitude, item.stopPoint.longitude)
                            title = item.stopPoint.note ?: "Điểm dừng"

                            val cachedBitmap = viewModel.stopPointBitmaps[item.stopPoint.id]
                            if (cachedBitmap != null) {
                                icon = BitmapDrawable(context.resources, cachedBitmap)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            } else {
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }

                            setOnMarkerClickListener { _, _ ->
                                navController.navigate("stop_detail/${item.stopPoint.id}")
                                true
                            }
                        }
                        m.overlays.add(marker)
                    }
                    m.invalidate()
                }
            )

            // Nút My Location (Focus tức thì)
            SmallFloatingActionButton(
                onClick = {
                    locationOverlay.myLocation?.let {
                        mapView.controller.animateTo(it)
                        mapView.controller.setZoom(18.0)
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                containerColor = Color.White
            ) { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }

    // --- SHEET DANH SÁCH CHECK-IN (ĐÃ BỔ SUNG TÍNH NĂNG XÓA) ---
    if (showStopList) {
        ModalBottomSheet(onDismissRequest = { showStopList = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {

                // 1. DÒNG HEADER: Tiêu đề + Nút Xóa tất cả
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Danh sách Check-in",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Chỉ hiện nút Xóa Tất Cả nếu có dữ liệu
                    if (stopPoints.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.deleteAllStopPoints(mapId)
                                showStopList = false // Đóng sheet sau khi xóa sạch
                            }
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa tất cả", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 2. DANH SÁCH CHI TIẾT
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(stopPoints) { item ->
                        ListItem(
                            headlineContent = { Text(item.stopPoint.note.ifBlank { "Không ghi chú" }) },
                            leadingContent = {
                                val tUrl = item.stopPoint.thumbnailUri ?: item.mediaList.firstOrNull()?.fileUri
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(NetworkConfig.getFullImageUrl(tUrl, currentBaseUrl)) // 🚀 FIX XỌC XANH
                                        .addHeader("ngrok-skip-browser-warning", "true")
                                        .error(R.drawable.ic_launcher_background)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            // 🚀 THÊM NÚT XÓA TỪNG ĐIỂM VÀO ĐÂY (NẰM BÊN PHẢI CÙNG)
                            trailingContent = {
                                IconButton(
                                    onClick = { viewModel.deleteStopPoint(item.stopPoint.id, mapId) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa Check-in",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                showStopList = false
                                mapView.controller.animateTo(GeoPoint(item.stopPoint.latitude, item.stopPoint.longitude))
                                mapView.controller.setZoom(18.5)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- SHEET CHIA SẺ ---
    if (showPublishBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showPublishBottomSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding()) {
                Text("Chia sẻ hành trình", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = publishTitle, onValueChange = { publishTitle = it }, label = { Text("Tiêu đề") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mediaForPublish) { media ->
                        Box(
                            Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (selectedThumbUrl == media.fileUri) 4.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .clickable { selectedThumbUrl = media.fileUri }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(NetworkConfig.getFullImageUrl(media.fileUri, currentBaseUrl)) // 🚀 FIX XỌC XANH
                                    .addHeader("ngrok-skip-browser-warning", "true")
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        currentJourney?.let { journey ->
                            viewModel.publishJourneyToDiscovery(journey, publishTitle, selectedThumbUrl) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) showPublishBottomSheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Đăng lên Cộng đồng") }
            }
        }
    }

    // --- DIALOG CHECK-IN ---
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Lưu điểm dừng chân", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = stopNote, onValueChange = { stopNote = it }, label = { Text("Bạn đang nghĩ gì?") })
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) {
                        Text("Chọn ảnh/Video (${selectedMediaUris.size})")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    locationOverlay.myLocation?.let {
                        viewModel.addStopPointWithMedia(context, mapId, it.latitude, it.longitude, stopNote, selectedMediaUris)
                        showStopDialog = false
                        stopNote = ""; selectedMediaUris = emptyList()
                    } ?: Toast.makeText(context, "Đang chờ GPS sếp ơi...", Toast.LENGTH_SHORT).show()
                }) { Text("Lưu điểm") }
            }
        )
    }
}