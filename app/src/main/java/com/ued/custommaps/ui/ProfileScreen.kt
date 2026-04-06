@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
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
import com.ued.custommaps.viewmodel.MapViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: MapViewModel,
    discoveryViewModel: DiscoveryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userSession by viewModel.userSession.collectAsState()

    val sessionManager = remember { com.ued.custommaps.data.SessionManager(context) }
    val serverUrlFromSession by sessionManager.serverUrlFlow.collectAsState(initial = "")

    val currentBaseUrl = if (serverUrlFromSession.isNullOrBlank()) {
        NetworkConfig.BASE_URL
    } else {
        serverUrlFromSession!!
    }

    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            isUploading = true
            viewModel.uploadAvatar(selectedUri, context) { success ->
                isUploading = false
                if (success) {
                    Toast.makeText(context, "Đổi avatar ngon lành rồi sếp!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi rồi, check server đi sếp!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val uiState by discoveryViewModel.uiState.collectAsState()
    val myPosts = remember(uiState, userSession) {
        if (uiState is DiscoveryUiState.Success && userSession != null) {
            (uiState as DiscoveryUiState.Success).data.filter { it.authorName == userSession?.username }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thông tin cá nhân", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // --- KHU VỰC AVATAR ---
            Box(contentAlignment = Alignment.BottomEnd) {
                val finalImageUrl = NetworkConfig.getFullImageUrl(userSession?.avatarUrl, currentBaseUrl)

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(finalImageUrl)
                        .crossfade(true)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .error(R.drawable.ic_launcher_background)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                    contentScale = ContentScale.Crop
                )

                FloatingActionButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = Color.White)
                }

                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(130.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- THÔNG TIN TEXT ---
            Text(
                text = userSession?.displayName ?: userSession?.username ?: "Thợ săn Map",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "@${userSession?.username ?: "username"}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { /* Triển khai sau */ }, shape = CircleShape) {
                    Text("Đổi mật khẩu", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    viewModel.logout()
                    navController.navigate("login") { popUpTo(0) }
                }) {
                    Text("Đăng xuất", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================
            // 🚀 KHU VỰC QUẢN LÝ BÀI VIẾT
            // =========================================================
            Text(
                text = "Hành trình đã chia sẻ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (myPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Sếp chưa đăng hành trình nào lên cộng đồng!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myPosts) { post ->
                        MyPostManageItem(
                            post = post,
                            baseUrl = currentBaseUrl,
                            onDeleteClick = {
                                // 🚀 ĐÃ NỐI DÂY: Gọi API xóa thật từ ViewModel
                                discoveryViewModel.deletePost(
                                    postId = post.postId.toLong(),
                                    onSuccess = {
                                        Toast.makeText(context, "Đã dọn dẹp sạch sẽ sếp nhé!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyPostManageItem(post: DiscoveryPost, baseUrl: String, onDeleteClick: () -> Unit) {
    var rawThumbUrl = post.thumbnailUri
    if (rawThumbUrl.isNullOrBlank()) {
        val firstStopWithMedia = post.payload?.stopPoints?.firstOrNull { !it.media.isNullOrEmpty() }
        rawThumbUrl = firstStopWithMedia?.media?.firstOrNull()?.fileUri
    }
    val finalThumbnailUrl = NetworkConfig.getFullImageUrl(rawThumbUrl, baseUrl)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(65.dp).clip(RoundedCornerShape(10.dp)).background(Color.LightGray)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(finalThumbnailUrl)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .crossfade(true)
                        .error(R.drawable.ic_launcher_background)
                        .build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = post.payload?.journey?.title ?: "Hành trình không tên",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${post.likesCount} lượt thích",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa bài")
            }
        }
    }
}