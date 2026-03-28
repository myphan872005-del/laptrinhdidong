@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ued.custommaps.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StopPointDetailScreen(stopId: Long, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data by viewModel.getStopPointById(stopId).observeAsState()
    var note by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }

    // State lưu uri video đang cần Play (null thì ẩn Dialog)
    var videoUriToPlay by remember { mutableStateOf<String?>(null) }

    val addMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) viewModel.addMoreMediaToStop(context, stopId, uris)
    }

    LaunchedEffect(data) { data?.let { note = it.stopPoint.note } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết điểm dừng") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    // --- NÚT ĐI TỚI ĐIỂM TRÊN MAP ---
                    IconButton(onClick = {
                        data?.let {
                            viewModel.focusLocation.value = Pair(it.stopPoint.latitude, it.stopPoint.longitude)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Đi đến điểm này")
                    }

                    // --- NÚT LƯU GHI CHÚ ---
                    IconButton(onClick = {
                        viewModel.updateStopPointNote(stopId, note)
                        scope.launch { showSavedMessage = true; delay(1000); showSavedMessage = false }
                    }) {
                        Icon(Icons.Default.Save, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Icon(Icons.Default.AddPhotoAlternate, null) }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            data?.let { item ->
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(16.dp))

                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(item.mediaList) { media ->
                            var expanded by remember { mutableStateOf(false) }
                            val isThumbnail = item.stopPoint.thumbnailUri == media.fileUri

                            // --- BẤM VÀO THÌ MỞ MENU (ẢNH) HOẶC PHÁT VIDEO ---
                            Box(Modifier.aspectRatio(1f).clickable {
                                if (media.mediaType == "VIDEO") {
                                    videoUriToPlay = media.fileUri // Ra lệnh phát video
                                } else {
                                    expanded = true // Hiện menu chọn Thumbnail cho ảnh
                                }
                            }) {
                                AsyncImage(model = media.fileUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)

                                if (media.mediaType == "VIDEO") {
                                    Icon(Icons.Default.PlayCircleOutline, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(32.dp))
                                }

                                if (isThumbnail) {
                                    Icon(Icons.Default.Star, contentDescription = "Thumbnail", tint = Color.Yellow, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
                                }

                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    if (media.mediaType == "IMAGE") {
                                        DropdownMenuItem(
                                            text = { Text(if (isThumbnail) "Hủy Thumbnail" else "Đặt làm Thumbnail") },
                                            onClick = {
                                                val newThumbnail = if (isThumbnail) null else media.fileUri
                                                viewModel.updateStopPointThumbnail(stopId, newThumbnail)
                                                expanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Image, null) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Xóa file này", color = Color.Red) },
                                        onClick = { viewModel.deleteMedia(media); expanded = false },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = showSavedMessage, enter = fadeIn() + slideInVertically(initialOffsetY = { it }), exit = fadeOut() + slideOutVertically(targetOffsetY = { it }), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) { Text("Đã lưu thành công!", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
            }
        }
    }

    // --- DIALOG XEM VIDEO BẰNG EXOPLAYER ---
    if (videoUriToPlay != null) {
        AlertDialog(
            onDismissRequest = { videoUriToPlay = null },
            modifier = Modifier.fillMaxWidth().aspectRatio(9f/16f), // Size dọc giống Tiktok/Shorts
            title = null,
            text = {
                ExoPlayerView(videoUri = videoUriToPlay!!)
            },
            confirmButton = {
                TextButton(onClick = { videoUriToPlay = null }) { Text("Đóng") }
            }
        )
    }
}

// --- COMPOSABLE RENDER EXOPLAYER ---
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ExoPlayerView(videoUri: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(Uri.parse(videoUri))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true // Tự động phát khi load xong
        }
    }

    // Nhớ giải phóng bộ nhớ khi tắt Video đi
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            androidx.media3.ui.PlayerView(context).apply {
                player = exoPlayer
                useController = true // Hiện thanh tua, play/pause
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
    )
}