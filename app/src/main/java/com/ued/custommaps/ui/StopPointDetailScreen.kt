@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ued.custommaps.data.StopPointMediaEntity
import com.ued.custommaps.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Dialog


@Composable
fun StopPointDetailScreen(stopId: Long, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Quan sát dữ liệu từ Room (Kiểu StopPointWithMedia)
    val data by viewModel.getStopPointById(stopId).observeAsState(initial = null)

    var note by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }
    var videoUriToPlay by remember { mutableStateOf<String?>(null) }

    // Launcher thêm media mới
    val addMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addMoreMediaToStop(context, stopId, uris)
        }
    }

    // Cập nhật note khi data load xong
    LaunchedEffect(data) {
        data?.let { note = it.stopPoint.note }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết điểm dừng") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // Nút Focus trên bản đồ
                    IconButton(onClick = {
                        data?.let {
                            viewModel.focusLocation.value = Pair(it.stopPoint.latitude, it.stopPoint.longitude)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, "Xem trên bản đồ")
                    }

                    // Nút Lưu ghi chú
                    IconButton(onClick = {
                        viewModel.updateStopPointNote(stopId, note)
                        scope.launch {
                            showSavedMessage = true
                            delay(1500)
                            showSavedMessage = false
                        }
                    }) {
                        Icon(Icons.Default.Save, "Lưu ghi chú")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
            ) { Icon(Icons.Default.AddPhotoAlternate, null) }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            data?.let { item ->
                Column(Modifier.padding(16.dp)) {
                    // Thông tin thời gian
                    Text(
                        text = "Ghi lại lúc: ${SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(item.stopPoint.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú chuyến đi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Hình ảnh & Video", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(item.mediaList) { media ->
                            MediaItemGrid(
                                media = media,
                                isThumbnail = item.stopPoint.thumbnailUri == media.fileUri,
                                onPlayVideo = { videoUriToPlay = it },
                                onSetThumbnail = { viewModel.updateStopPointThumbnail(stopId, it) },
                                onDelete = { viewModel.deleteMedia(it) }
                            )
                        }
                    }
                }
            }

            // Thông báo "Đã lưu" dạng Toast-like
            AnimatedVisibility(
                visible = showSavedMessage,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp
                ) {
                    Text("Đã lưu ghi chú thành công!", modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                }
            }
        }
    }

    // Dialog Xem Video
    if (videoUriToPlay != null) {
        Dialog(onDismissRequest = { videoUriToPlay = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(9f/16f),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box {
                    ExoPlayerView(videoUri = videoUriToPlay!!)
                    IconButton(
                        onClick = { videoUriToPlay = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemGrid(
    media: StopPointMediaEntity,
    isThumbnail: Boolean,
    onPlayVideo: (String) -> Unit,
    onSetThumbnail: (String?) -> Unit,
    onDelete: (StopPointMediaEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    if (media.mediaType == "VIDEO") onPlayVideo(media.fileUri)
                    else expanded = true
                },
                onLongClick = { expanded = true }
            )
    ) {
        AsyncImage(
            model = media.fileUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (media.mediaType == "VIDEO") {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(32.dp)
            )
        }

        if (isThumbnail) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Yellow, CircleShape)
                    .padding(2.dp)
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color.Black)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (media.mediaType == "IMAGE") {
                DropdownMenuItem(
                    text = { Text(if (isThumbnail) "Bỏ làm đại diện" else "Đặt làm đại diện") },
                    onClick = {
                        onSetThumbnail(if (isThumbnail) null else media.fileUri)
                        expanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Image, null) }
                )
            }
            DropdownMenuItem(
                text = { Text("Xóa vĩnh viễn", color = Color.Red) },
                onClick = {
                    onDelete(media)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ExoPlayerView(videoUri: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(Uri.parse(videoUri))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            androidx.media3.ui.PlayerView(context).apply {
                player = exoPlayer
                useController = true
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}