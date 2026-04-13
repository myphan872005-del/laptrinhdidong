@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.ued.custommaps.network.NetworkConfig
import com.ued.custommaps.viewmodel.DiscoveryViewModel

@Composable
fun DiscoveryStopPointDetail(
    navController: NavController,
    viewModel: DiscoveryViewModel
) {
    val context = LocalContext.current
    val stopPoint = viewModel.selectedStopPoint.value

    if (stopPoint == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Không có dữ liệu điểm dừng!")
        }
        return
    }

    val sessionManager = remember { com.ued.custommaps.data.SessionManager(context) }
    val serverUrlFromSession by sessionManager.serverUrlFlow.collectAsState(initial = "")
    val currentBaseUrl = if (serverUrlFromSession.isNullOrBlank()) NetworkConfig.BASE_URL else serverUrlFromSession

    val note = stopPoint.note ?: "Không có ghi chú"
    val lat = stopPoint.latitude
    val lon = stopPoint.longitude
    val mediaList = stopPoint.media ?: emptyList()

    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chi tiết hành trình", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Quay lại") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)).verticalScroll(rememberScrollState())
        ) {
            // --- PHẦN 1: MEDIA TÍCH HỢP EXOPLAYER ---
            if (mediaList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mediaList) { media ->
                        val fileUri = media.fileUri
                        val type = media.mediaType
                        val fullUrl = NetworkConfig.getFullImageUrl(fileUri, currentBaseUrl ?: NetworkConfig.BASE_URL)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            modifier = Modifier
                                .fillParentMaxWidth(0.85f)
                                .fillMaxHeight()
                                .clickable {
                                    if (type == "VIDEO") fullScreenVideoUri = fileUri
                                    else fullScreenImageUrl = fullUrl
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fullUrl)
                                        .addHeader("ngrok-skip-browser-warning", "true")
                                        .decoderFactory(VideoFrameDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                if (type == "VIDEO") {
                                    Box(
                                        modifier = Modifier.size(60.dp).align(Alignment.Center).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(36.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("Không có hình/video", color = Color.Gray) }
                }
            }

            // --- PHẦN 2: THÔNG TIN GHI CHÚ VÀ TỌA ĐỘ BẤM ĐƯỢC ---
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Ghi chú của thành viên", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
                    Text(text = note, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🚀 NÚT BẤM TỌA ĐỘ GPS: BAY VỀ MAP
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            // Phát lệnh bay đến tọa độ này và lùi lại màn hình Map
                            viewModel.focusMapOn(lat, lon)
                            navController.popBackStack()
                        },
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp) // Thêm padding cho dễ bấm
                    ) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text("Tọa độ GPS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${String.format("%.5f", lat)}, ${String.format("%.5f", lon)}",
                            color = MaterialTheme.colorScheme.primary, // Đổi màu xanh để biết là bấm được
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ====================================================
        // 🚀 DIALOG 1: XEM ẢNH FULL MÀN HÌNH + ZOOM
        // ====================================================
        if (fullScreenImageUrl != null) {
            Dialog(
                onDismissRequest = { fullScreenImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                // Biến state cho pinch-to-zoom
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                // Đảm bảo mỗi lần mở ảnh mới là reset lại tỷ lệ
                LaunchedEffect(fullScreenImageUrl) {
                    scale = 1f
                    offset = Offset.Zero
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        // 🚀 THUẬT TOÁN BẮT CỬ CHỈ ZOOM & KÉO ẢNH
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f) // Zoom từ 1x đến 5x
                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fullScreenImageUrl)
                            .addHeader("ngrok-skip-browser-warning", "true")
                            .build(),
                        contentDescription = "Full Screen Image",
                        modifier = Modifier
                            .fillMaxSize()
                            // 🚀 ÁP DỤNG SCALE LÊN ẢNH
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )

                    IconButton(
                        onClick = { fullScreenImageUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Đóng", tint = Color.White)
                    }
                }
            }
        }

        // ====================================================
        // 🚀 DIALOG 2: TRÌNH PHÁT VIDEO BẰNG EXOPLAYER
        // ====================================================
        if (fullScreenVideoUri != null) {
            Dialog(
                onDismissRequest = { fullScreenVideoUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    ExoPlayerView(
                        videoUri = fullScreenVideoUri!!,
                        baseUrl = currentBaseUrl ?: ""
                    )

                    IconButton(
                        onClick = { fullScreenVideoUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Đóng", tint = Color.White)
                    }
                }
            }
        }
    }
}