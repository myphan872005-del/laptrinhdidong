@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.annotation.SuppressLint
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
import com.ued.custommaps.models.DiscoveryStopPoint
import com.ued.custommaps.viewmodel.DiscoveryUiState
import com.ued.custommaps.viewmodel.DiscoveryViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun DiscoveryDetailScreen(postId: Int, navController: NavController, viewModel: DiscoveryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Tìm bài đăng cụ thể từ danh sách đã load trong ViewModel
    val post = (uiState as? DiscoveryUiState.Success)?.data?.find { it.postId == postId }
    val payload = post?.payload
    val journeyInfo = payload?.journey
    val trackPoints = payload?.trackPoints ?: emptyList()
    val stopPoints = payload?.stopPoints ?: emptyList()

    var showStopList by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 4.0
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
                title = { Text(journeyInfo?.title ?: "Chi tiết hành trình") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showStopList = true }) { Icon(Icons.Default.FormatListBulleted, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = { m ->
                    m.overlays.clear()

                    // 1. Vẽ đường đi (Polyline) - Màu xanh dương cho khác biệt với bản đồ cá nhân
                    if (trackPoints.isNotEmpty()) {
                        val polyline = Polyline().apply {
                            setPoints(trackPoints.map { GeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = android.graphics.Color.BLUE
                            outlinePaint.strokeWidth = 10f
                        }
                        m.overlays.add(polyline)
                    }

                    // 2. Vẽ các điểm dừng (Markers)
                    stopPoints.forEach { item ->
                        val marker = Marker(m).apply {
                            position = GeoPoint(item.latitude, item.longitude)
                            title = item.note ?: "Điểm dừng"

                            // Vì là ảnh từ Server, tạm thời dùng Icon mặc định của hệ thống
                            val iconId = if (item.media?.any { it.mediaType == "IMAGE" } == true)
                                android.R.drawable.ic_menu_camera else android.R.drawable.ic_menu_mylocation
                            icon = ContextCompat.getDrawable(context, iconId)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            setOnMarkerClickListener { _, _ ->
                                // Có thể hiển thị một Dialog nhỏ hoặc BottomSheet xem ảnh tại đây
                                showStopList = true
                                true
                            }
                        }
                        m.overlays.add(marker)
                    }

                    // 3. Tự động căn giữa vào điểm bắt đầu của hành trình
                    if (journeyInfo != null) {
                        m.controller.setZoom(15.0)
                        m.controller.setCenter(GeoPoint(journeyInfo.startLat, journeyInfo.startLon))
                    }
                    m.invalidate()
                }
            )
        }
    }

    // Danh sách điểm dừng (Chỉ xem)
    if (showStopList) {
        ModalBottomSheet(onDismissRequest = { showStopList = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text("Các điểm dừng chân", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(stopPoints) { item ->
                        DiscoveryStopListItem(item)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryStopListItem(item: DiscoveryStopPoint) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                // Load ảnh từ URL Server (Nghĩa là file_uri lúc này là đường dẫn http)
                if (item.media?.isNotEmpty() == true) {
                    AsyncImage(
                        model = item.media.first().fileUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.note?.ifBlank { "Điểm dừng" } ?: "Điểm dừng", fontWeight = FontWeight.Bold)
            }
            Badge { Text("${item.media?.size ?: 0}") }
        }
    }
}