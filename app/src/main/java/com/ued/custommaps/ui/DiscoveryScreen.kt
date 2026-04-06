@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.ued.custommaps.R
import com.ued.custommaps.models.DiscoveryPost
import com.ued.custommaps.network.NetworkConfig
import com.ued.custommaps.viewmodel.DiscoveryUiState
import com.ued.custommaps.viewmodel.DiscoveryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiscoveryScreen(
    navController: NavController,
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val sessionManager = remember { com.ued.custommaps.data.SessionManager(context) }
    val serverUrlFromSession by sessionManager.serverUrlFlow.collectAsState(initial = "")

    val currentBaseUrl = if (serverUrlFromSession.isNullOrBlank()) {
        NetworkConfig.BASE_URL
    } else {
        serverUrlFromSession!!
    }

    LaunchedEffect(currentBaseUrl) {
        Log.d("DISCOVERY_DEBUG", "🔗 Link Ngrok đang dùng: $currentBaseUrl")
        viewModel.fetchDiscovery()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cộng Đồng Khám Phá", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF0F2F5))
        ) {
            when (uiState) {
                is DiscoveryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DiscoveryUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mất kết nối máy chủ", color = Color.Gray)
                        Button(onClick = { viewModel.fetchDiscovery() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Thử lại")
                        }
                    }
                }
                is DiscoveryUiState.Success -> {
                    val posts = (uiState as DiscoveryUiState.Success).data
                    if (posts.isEmpty()) {
                        Text("Chưa có bài đăng nào!", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(posts) { post ->
                                DiscoveryPostCard(
                                    post = post,
                                    baseUrl = currentBaseUrl,
                                    onClick = { navController.navigate("discovery_detail/${post.postId}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryPostCard(post: DiscoveryPost, baseUrl: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val journeyTitle = post.payload?.journey?.title ?: "Hành trình không tên"

    val finalAvatarUrl = NetworkConfig.getFullImageUrl(post.authorAvatar, baseUrl)

    // =========================================================================
    // 🚀 CHIÊU THỨC "MÓC ẢNH" MỚI (Trực tiếp bốc ảnh từ Media, bỏ qua model Journey)
    // =========================================================================
    var rawThumbUrl = post.thumbnailUri

    // Nếu Root không có Thumbnail, tìm thẳng vào danh sách điểm dừng để bốc Media đầu tiên
    if (rawThumbUrl.isNullOrBlank()) {
        val firstStopWithMedia = post.payload?.stopPoints?.firstOrNull { !it.media.isNullOrEmpty() }
        rawThumbUrl = firstStopWithMedia?.media?.firstOrNull()?.fileUri
    }

    val finalThumbnailUrl = NetworkConfig.getFullImageUrl(rawThumbUrl, baseUrl)

    SideEffect {
        Log.d("DISCOVERY_DEBUG", """
            --- POST ID: ${post.postId} ---
            📸 Avatar Link: $finalAvatarUrl
            🖼️ Thumbnail Link: $finalThumbnailUrl
        """.trimIndent())
    }

    val formattedTime = remember(post.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(post.createdAt)
            SimpleDateFormat("dd MMMM, HH:mm", Locale("vi", "VN")).format(date!!)
        } catch (e: Exception) { post.createdAt }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- 1. HEADER (AVATAR) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(finalAvatarUrl)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .crossfade(true)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_background)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(45.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.authorName ?: "Ẩn danh", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(formattedTime, color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(journeyTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // --- 2. BODY (ẢNH BÌA BÀI VIẾT) ---
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8F9FA)),
                contentAlignment = Alignment.Center
            ) {
                if (!rawThumbUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(finalThumbnailUrl)
                            .addHeader("ngrok-skip-browser-warning", "true")
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .crossfade(true)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_background)
                            .build(),
                        contentDescription = "Main Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Text("Xem bản đồ chi tiết", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. FOOTER (LIKE & ACTION) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FavoriteBorder, null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${post.likesCount} lượt thích", color = Color.DarkGray, fontSize = 13.sp)

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Text(
                        "Xem hành trình",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}