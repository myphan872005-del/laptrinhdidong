@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.ViewGroup
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
fun MapDetailScreen(mapId: String, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val journeyId = mapId.toLongOrNull() ?: -1L

    val trackPoints by viewModel.getTrackPoints(journeyId).observeAsState(initial = emptyList())
    val stopPoints by viewModel.getStopPoints(journeyId).observeAsState(initial = emptyList())
    val allJourneys by viewModel.journeys.observeAsState(initial = emptyList())
    val currentJourney = allJourneys.find { it.id == journeyId }
    val isTracking = viewModel.isTracking.value

    var showStopList by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var stopNote by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedStopIds = remember { mutableStateListOf<Long>() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { selectedMediaUris = it }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 4.0
            setHorizontalMapRepetitionEnabled(false)
            setVerticalMapRepetitionEnabled(false)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    var isMapCentered by remember(mapView) { mutableStateOf(false) }

    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { enableMyLocation() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { mapView.onResume(); locationOverlay.enableMyLocation() }
            else if (event == Lifecycle.Event.ON_PAUSE) { mapView.onPause(); locationOverlay.disableMyLocation() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); mapView.onDetach() }
    }

    DisposableEffect(stopPoints) {
        if (stopPoints.isNotEmpty()) viewModel.loadBitmapsForStopPoints(context, stopPoints)
        onDispose { }
    }

    // --- LOGIC NHẢY ĐẾN ĐIỂM DỪNG TỪ MÀN CHI TIẾT ---
    LaunchedEffect(viewModel.focusLocation.value) {
        viewModel.focusLocation.value?.let { loc ->
            mapView.controller.animateTo(GeoPoint(loc.first, loc.second))
            mapView.controller.setZoom(19.0) // Zoom sâu vào luôn cho rõ
            viewModel.focusLocation.value = null // Nhảy xong thì reset lệnh
            isMapCentered = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(currentJourney?.title ?: "Hành trình") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    Button(onClick = {
                        viewModel.toggleTracking()
                        val intent = Intent(context, TrackingService::class.java).apply { putExtra("JOURNEY_ID", journeyId); if (isTracking) action = "STOP" }
                        if (!isTracking) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
                    }, colors = ButtonDefaults.buttonColors(containerColor = if(isTracking) Color.Red else MaterialTheme.colorScheme.primary)) {
                        Icon(if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, null); Text(if (isTracking) " Dừng" else " Ghi")
                    }
                    Button(onClick = { showStopDialog = true }) { Icon(Icons.Default.AddLocation, null); Text(" Check-in") }
                    IconButton(onClick = { showStopList = true }) { Icon(Icons.Default.FormatListBulleted, null) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView.overlays.add(locationOverlay); mapView },
                update = { m ->
                    m.overlays.removeAll { it is Polyline || it is Marker }
                    trackPoints.groupBy { it.segmentId }.forEach { (_, pts) ->
                        if (pts.size >= 2) m.overlays.add(Polyline().apply { setPoints(pts.map { GeoPoint(it.latitude, it.longitude) }); outlinePaint.color = android.graphics.Color.RED; outlinePaint.strokeWidth = 10f })
                    }

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
                            setOnMarkerClickListener { _, _ -> navController.navigate("stop_detail/${item.stopPoint.id}"); true }
                        }
                        m.overlays.add(marker)
                    }

                    if (!isMapCentered) {
                        val currentStartLat = currentJourney?.startLat ?: 0.0
                        if (currentStartLat != 0.0) { m.controller.setZoom(16.0); m.controller.setCenter(GeoPoint(currentStartLat, currentJourney!!.startLon)); isMapCentered = true }
                        else { val myLoc = locationOverlay.myLocation; if (myLoc != null && myLoc.latitude != 0.0) { m.controller.setZoom(16.0); m.controller.setCenter(myLoc); viewModel.updateJourneyStartLocation(currentJourney!!, myLoc.latitude, myLoc.longitude); isMapCentered = true } else { m.controller.setZoom(6.0); m.controller.setCenter(GeoPoint(16.0, 108.0)) } }
                    }
                    m.invalidate()
                }
            )
            SmallFloatingActionButton(onClick = { locationOverlay.myLocation?.let { mapView.controller.animateTo(it); mapView.controller.setZoom(18.0) } }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.MyLocation, null) }
        }
    }

    // Các phần Dialog & BottomSheet vẫn giữ nguyên
    if (showStopDialog) {
        AlertDialog(onDismissRequest = { showStopDialog = false }, title = { Text("Check-in") }, text = { Column { OutlinedTextField(value = stopNote, onValueChange = { stopNote = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); Button(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Text("Chọn media (${selectedMediaUris.size})") } } }, confirmButton = { Button(onClick = { locationOverlay.myLocation?.let { viewModel.addStopPointWithMedia(context, journeyId, it.latitude, it.longitude, stopNote, selectedMediaUris); showStopDialog = false; stopNote = ""; selectedMediaUris = emptyList() } }) { Text("Lưu") } })
    }

    if (showStopList) {
        ModalBottomSheet(onDismissRequest = { showStopList = false; isSelectMode = false; selectedStopIds.clear() }) {
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(if (isSelectMode) "Đã chọn ${selectedStopIds.size}" else "Điểm dừng", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { if (!isSelectMode) isSelectMode = true else { if (selectedStopIds.size == stopPoints.size) selectedStopIds.clear() else { selectedStopIds.clear(); selectedStopIds.addAll(stopPoints.map { it.stopPoint.id }) } } }) { Text(if (!isSelectMode) "Chọn" else "Tất cả") }
                }
                LazyColumn(Modifier.weight(1f, false).padding(vertical = 8.dp)) {
                    items(stopPoints) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectMode) Checkbox(checked = selectedStopIds.contains(item.stopPoint.id), onCheckedChange = { if (it) selectedStopIds.add(item.stopPoint.id) else selectedStopIds.remove(item.stopPoint.id) })
                            StopListItem(item) { if (isSelectMode) { if (selectedStopIds.contains(item.stopPoint.id)) selectedStopIds.remove(item.stopPoint.id) else selectedStopIds.add(item.stopPoint.id) } else { showStopList = false; navController.navigate("stop_detail/${item.stopPoint.id}") } }
                        }
                    }
                }
                if (isSelectMode && selectedStopIds.isNotEmpty()) {
                    Button(onClick = { viewModel.deleteSelectedStopPoints(selectedStopIds.toList(), stopPoints); selectedStopIds.clear(); isSelectMode = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Icon(Icons.Default.Delete, null); Text(" Xóa mục đã chọn") }
                }
            }
        }
    }
}

@Composable
fun StopListItem(item: StopPointWithMedia, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) { if (item.mediaList.isNotEmpty()) AsyncImage(model = item.mediaList.first().fileUri, contentDescription = null, contentScale = ContentScale.Crop) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.stopPoint.note.ifBlank { "Điểm dừng" }, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()).format(Date(item.stopPoint.timestamp)), style = MaterialTheme.typography.bodySmall) }
            Badge { Text("${item.mediaList.size}") }
        }
    }
}

fun getBitmapFromVector(context: Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}