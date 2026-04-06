@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.decode.VideoFrameDecoder
import coil.imageLoader // 🚀 Cần để load Bitmap cho Marker
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation // 🚀 Bo tròn ảnh Marker
import com.ued.custommaps.R
import com.ued.custommaps.models.DiscoveryStopPoint
import com.ued.custommaps.network.NetworkConfig
import com.ued.custommaps.viewmodel.DiscoveryUiState
import com.ued.custommaps.viewmodel.DiscoveryViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun DiscoveryDetailScreen(postId: Long, navController: NavController, viewModel: DiscoveryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val sessionManager = remember { com.ued.custommaps.data.SessionManager(context) }
    val currentBaseUrl by sessionManager.serverUrlFlow.collectAsState(initial = "")
    val baseUrl: String = currentBaseUrl.takeIf { !it.isNullOrBlank() } ?: NetworkConfig.BASE_URL

    val post = (uiState as? DiscoveryUiState.Success)?.data?.find { it.postId.toLong() == postId }
    val payload = post?.payload
    val journeyInfo = payload?.journey
    val trackPoints = payload?.trackPoints ?: emptyList()
    val stopPoints = payload?.stopPoints ?: emptyList()

    var showStopList by remember { mutableStateOf(false) }
    var isMapInitialized by remember { mutableStateOf(false) }

    // 🚀 LƯU TRỮ BITMAP CHO MARKER
    val imageLoader = context.imageLoader
    val stopPointBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    // 🚀 TỰ ĐỘNG TẢI THUMBNAIL CHO TỪNG ĐIỂM DỪNG
    LaunchedEffect(stopPoints, baseUrl) {
        stopPoints.forEach { sp ->
            val thumbUrl = sp.thumbnailUri ?: sp.media?.firstOrNull()?.fileUri
            val fullUrl = NetworkConfig.getFullImageUrl(thumbUrl, baseUrl)

            if (fullUrl.isNotBlank() && !stopPointBitmaps.containsKey(sp.hashCode())) {
                val request = ImageRequest.Builder(context)
                    .data(fullUrl)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .size(150, 150) // Kích thước Marker
                    .transformations(CircleCropTransformation()) // Bo tròn
                    .target { drawable ->
                        (drawable as? BitmapDrawable)?.bitmap?.let {
                            stopPointBitmaps[sp.hashCode()] = it
                        }
                    }
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isVerticalMapRepetitionEnabled = false
            isHorizontalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0)
            minZoomLevel = 4.0
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    val focusLoc = viewModel.focusLocation.value
    LaunchedEffect(focusLoc) {
        focusLoc?.let { (lat, lon) ->
            // Bay mượt mà đến tọa độ và zoom to lên
            mapView.controller.animateTo(GeoPoint(lat, lon), 18.0, 1000L)

            // Xóa tín hiệu để lần sau không bị bay nhầm
            viewModel.clearFocusLocation()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mapView.onResume()
            else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); mapView.onDetach() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(journeyInfo?.title ?: "Hành trình cộng đồng", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { showStopList = true }) { Icon(Icons.Default.FormatListBulleted, null, tint = MaterialTheme.colorScheme.primary) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = { m ->
                    m.overlays.clear()

                    if (trackPoints.isNotEmpty()) {
                        val polyline = Polyline(m).apply {
                            setPoints(trackPoints.map { GeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = android.graphics.Color.parseColor("#FF4081")
                            outlinePaint.strokeWidth = 12f
                        }
                        m.overlays.add(polyline)
                    }

                    stopPoints.forEach { item ->
                        val marker = Marker(m).apply {
                            position = GeoPoint(item.latitude, item.longitude)
                            title = item.note ?: "Điểm check-in"

                            // 🚀 DÙNG ẢNH ĐÃ TẢI LÀM MARKER
                            val cachedBitmap = stopPointBitmaps[item.hashCode()]
                            if (cachedBitmap != null) {
                                icon = BitmapDrawable(context.resources, cachedBitmap)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            } else {
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }

                            setOnMarkerClickListener { _, _ ->
                                showStopList = true
                                true
                            }
                        }
                        m.overlays.add(marker)
                    }

                    if (!isMapInitialized && journeyInfo != null) {
                        m.controller.setZoom(16.0)
                        m.controller.setCenter(GeoPoint(journeyInfo.startLat, journeyInfo.startLon))
                        isMapInitialized = true
                    }
                    m.invalidate()
                }
            )
        }
    }

    if (showStopList) {
        ModalBottomSheet(onDismissRequest = { showStopList = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text("Các điểm dừng chân", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                if (stopPoints.isEmpty()) {
                    Text("Hành trình này không có điểm dừng nào sếp ạ.", color = Color.Gray)
                } else {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(stopPoints) { item ->
                            DiscoveryStopListItem(
                                item = item,
                                baseUrl = baseUrl,
                                onClick = {
                                    viewModel.setSelectedStopPoint(item)
                                    navController.navigate("discovery_stop_detail")
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
@Composable
fun DiscoveryStopListItem(item: DiscoveryStopPoint, baseUrl: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail nhỏ bên trái
            Box(Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)).background(Color.LightGray)) {
                val thumbUrl = item.thumbnailUri ?: (item.media?.firstOrNull()?.fileUri)

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(NetworkConfig.getFullImageUrl(thumbUrl, baseUrl)) // 🚀 FIX XỌC XANH
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .crossfade(true)
                        .error(R.drawable.ic_launcher_background)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (item.note.isNullOrBlank()) "Điểm dừng chân" else item.note,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${item.media?.size ?: 0} hình ảnh/video",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}