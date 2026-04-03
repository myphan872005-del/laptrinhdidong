@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ued.custommaps.TrackingService
import com.ued.custommaps.data.*
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
    val journeyId = mapId

    // Quan sát dữ liệu từ Room
    val trackPoints by viewModel.getTrackPoints(journeyId).observeAsState(initial = emptyList())
    val stopPoints by viewModel.getStopPoints(journeyId).observeAsState(initial = emptyList())
    val allJourneys by viewModel.journeys.observeAsState(initial = emptyList())

    val currentJourney = allJourneys.find { it.id == journeyId }
    val isTracking = viewModel.isTracking.value

    // States cho UI
    var showStopList by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    var stopNote by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedStopIds = remember { mutableStateListOf<Long>() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { selectedMediaUris = it }

    // Cấu hình MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 4.0
            setHorizontalMapRepetitionEnabled(false)
            setVerticalMapRepetitionEnabled(false)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    var isMapCentered by remember { mutableStateOf(false) }

    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }

    // Quản lý vòng đời MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    locationOverlay.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    locationOverlay.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Load Bitmaps cho Markers
    LaunchedEffect(stopPoints) {
        if (stopPoints.isNotEmpty()) {
            viewModel.loadBitmapsForStopPoints(context, stopPoints)
        }
    }

    // Điều hướng camera khi có yêu cầu Focus
    LaunchedEffect(viewModel.focusLocation.value) {
        viewModel.focusLocation.value?.let { loc ->
            mapView.controller.animateTo(GeoPoint(loc.first, loc.second))
            mapView.controller.setZoom(19.0)
            viewModel.focusLocation.value = null
            isMapCentered = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentJourney?.title ?: "Hành trình") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Chia sẻ",
                            tint = if (currentJourney?.isSynced == true)
                                MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    // NÚT GHI (TRACKING): Đã sửa logic gọi Service chuẩn
                    Button(
                        onClick = { viewModel.toggleTracking(context, journeyId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(isTracking) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                        Text(if (isTracking) " Dừng" else " Ghi")
                    }

                    Button(onClick = { showStopDialog = true }) {
                        Icon(Icons.Default.AddLocation, null)
                        Text(" Check-in")
                    }

                    IconButton(onClick = { showStopList = true }) {
                        Icon(Icons.Default.FormatListBulleted, null)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    if (!mapView.overlays.contains(locationOverlay)) {
                        mapView.overlays.add(locationOverlay)
                    }
                    mapView
                },
                update = { m ->
                    m.overlays.removeAll { it is Polyline || it is Marker }

                    // Vẽ đường đi (Track Points)
                    trackPoints.groupBy { it.segmentId }.forEach { (_, pts) ->
                        if (pts.size >= 2) {
                            val line = Polyline().apply {
                                setPoints(pts.map { GeoPoint(it.latitude, it.longitude) })
                                outlinePaint.color = android.graphics.Color.RED
                                outlinePaint.strokeWidth = 10f
                            }
                            m.overlays.add(line)
                        }
                    }

                    // Vẽ điểm dừng (Markers)
                    stopPoints.forEach { item ->
                        val marker = Marker(m).apply {
                            position = GeoPoint(item.stopPoint.latitude, item.stopPoint.longitude)
                            title = item.stopPoint.note

                            val cachedBitmap = viewModel.stopPointBitmaps[item.stopPoint.id]
                            if (cachedBitmap != null) {
                                icon = BitmapDrawable(context.resources, cachedBitmap)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            } else {
                                val fallbackIconId = when {
                                    item.mediaList.any { it.mediaType == "IMAGE" } -> android.R.drawable.ic_menu_camera
                                    item.mediaList.any { it.mediaType == "VIDEO" } -> android.R.drawable.ic_menu_slideshow
                                    else -> android.R.drawable.ic_menu_mylocation
                                }
                                icon = ContextCompat.getDrawable(context, fallbackIconId)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            setOnMarkerClickListener { _, _ ->
                                navController.navigate("stop_detail/${item.stopPoint.id}")
                                true
                            }
                        }
                        m.overlays.add(marker)
                    }

                    // Center camera logic
                    if (!isMapCentered) {
                        val startLat = currentJourney?.startLat ?: 0.0
                        if (startLat != 0.0) {
                            m.controller.setZoom(16.0)
                            m.controller.setCenter(GeoPoint(startLat, currentJourney!!.startLon))
                            isMapCentered = true
                        } else {
                            locationOverlay.myLocation?.let { myLoc ->
                                m.controller.setZoom(16.0)
                                m.controller.setCenter(myLoc)
                                viewModel.updateJourneyStartLocation(currentJourney!!, myLoc.latitude, myLoc.longitude)
                                isMapCentered = true
                            } ?: run {
                                m.controller.setZoom(6.0)
                                m.controller.setCenter(GeoPoint(16.0, 108.0))
                            }
                        }
                    }
                    m.invalidate()
                }
            )

            SmallFloatingActionButton(
                onClick = {
                    locationOverlay.myLocation?.let {
                        mapView.controller.animateTo(it)
                        mapView.controller.setZoom(18.0)
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) { Icon(Icons.Default.MyLocation, null) }
        }
    }

    // --- DIALOGS ---
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Chia sẻ hành trình") },
            text = { Text("Đăng hành trình này lên cộng đồng Khám phá?") },
            confirmButton = {
                Button(onClick = {
                    showShareDialog = false
                    currentJourney?.let { journey ->
                        viewModel.syncJourneyToServer(journey) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Chia sẻ") }
            },
            dismissButton = { TextButton(onClick = { showShareDialog = false }) { Text("Hủy") } }
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Check-in") },
            text = {
                Column {
                    OutlinedTextField(value = stopNote, onValueChange = { stopNote = it }, label = { Text("Ghi chú") })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }) { Text("Chọn media (${selectedMediaUris.size})") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    locationOverlay.myLocation?.let {
                        viewModel.addStopPointWithMedia(context, journeyId, it.latitude, it.longitude, stopNote, selectedMediaUris)
                        showStopDialog = false
                        stopNote = ""
                        selectedMediaUris = emptyList()
                    }
                }) { Text("Lưu") }
            }
        )
    }

    if (showStopList) {
        ModalBottomSheet(onDismissRequest = { showStopList = false; isSelectMode = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(if (isSelectMode) "Đã chọn ${selectedStopIds.size}" else "Điểm dừng", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        if (!isSelectMode) isSelectMode = true
                        else {
                            if (selectedStopIds.size == stopPoints.size) selectedStopIds.clear()
                            else { selectedStopIds.clear(); selectedStopIds.addAll(stopPoints.map { it.stopPoint.id }) }
                        }
                    }) { Text(if (!isSelectMode) "Chọn" else "Tất cả") }
                }

                LazyColumn(Modifier.weight(1f, false)) {
                    items(stopPoints) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectMode) {
                                Checkbox(
                                    checked = selectedStopIds.contains(item.stopPoint.id),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedStopIds.add(item.stopPoint.id)
                                        else selectedStopIds.remove(item.stopPoint.id)
                                    }
                                )
                            }
                            StopListItem(item) {
                                if (isSelectMode) {
                                    if (selectedStopIds.contains(item.stopPoint.id)) selectedStopIds.remove(item.stopPoint.id)
                                    else selectedStopIds.add(item.stopPoint.id)
                                } else {
                                    showStopList = false
                                    navController.navigate("stop_detail/${item.stopPoint.id}")
                                }
                            }
                        }
                    }
                }

                if (isSelectMode && selectedStopIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.deleteSelectedStopPoints(selectedStopIds.toList(), stopPoints)
                            selectedStopIds.clear()
                            isSelectMode = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Icon(Icons.Default.Delete, null); Text(" Xóa mục đã chọn") }
                }
            }
        }
    }
}

@Composable
fun StopListItem(item: StopPointWithMedia, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                if (item.mediaList.isNotEmpty()) {
                    AsyncImage(
                        model = item.mediaList.first().fileUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.stopPoint.note.ifBlank { "Điểm dừng không tên" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                        .format(Date(item.stopPoint.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("${item.mediaList.size}")
            }
        }
    }
}