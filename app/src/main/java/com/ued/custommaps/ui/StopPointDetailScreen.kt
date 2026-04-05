@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.ued.custommaps.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.ued.custommaps.R
import com.ued.custommaps.data.StopPointMediaEntity
import com.ued.custommaps.network.NetworkConfig
import com.ued.custommaps.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StopPointDetailScreen(stopId: Long, navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val baseUrlState by viewModel.currentBaseUrl
    val currentBaseUrl: String = baseUrlState ?: NetworkConfig.BASE_URL

    val data by viewModel.getStopPointById(stopId).observeAsState(initial = null)
    var note by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }
    var videoUriToPlay by remember { mutableStateOf<String?>(null) }

    val addMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            data?.let { item ->
                viewModel.addMoreMediaToStop(context, item.stopPoint.journeyId, stopId, uris)
            }
        }
    }

    LaunchedEffect(data) {
        data?.let { note = it.stopPoint.note }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết điểm dừng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        data?.let {
                            if (it.stopPoint.latitude != 0.0 && it.stopPoint.longitude != 0.0) {
                                viewModel.focusLocation.value = Pair(it.stopPoint.latitude, it.stopPoint.longitude)
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Chưa có tọa độ chuẩn sếp ơi!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.LocationSearching, null, tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        viewModel.updateStopPointNote(stopId, note)
                        scope.launch {
                            showSavedMessage = true
                            delay(2000)
                            showSavedMessage = false
                        }
                    }) {
                        Icon(Icons.Default.Save, null)
                    }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    addMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) { Icon(Icons.Default.AddPhotoAlternate, null) }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            data?.let { item ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Check-in lúc: ${SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(item.stopPoint.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Media (${item.mediaList.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Tính toán chiều cao Grid linh hoạt
                    val gridHeight = if (item.mediaList.isEmpty()) 100.dp else ((item.mediaList.size + 2) / 3 * 130).dp

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(gridHeight)
                    ) {
                        items(item.mediaList) { media ->
                            val normalizedThumb = item.stopPoint.thumbnailUri?.trim()?.removePrefix("/") ?: ""
                            val normalizedMedia = media.fileUri.trim().removePrefix("/")
                            val isThumbnail = normalizedThumb.isNotEmpty() && normalizedThumb == normalizedMedia

                            MediaItemGrid(
                                media = media,
                                isThumbnail = isThumbnail,
                                currentBaseUrl = currentBaseUrl,
                                onPlayVideo = { videoUriToPlay = it },
                                onSetThumbnail = { viewModel.updateStopPointThumbnail(stopId, it) },
                                onDelete = { viewModel.deleteMedia(media) }
                            )
                        }
                    }
                    Spacer(Modifier.height(100.dp))
                }
            }

            AnimatedVisibility(
                visible = showSavedMessage,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
            ) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp
                ) {
                    Text("Đã lưu!", color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                }
            }
        }
    }

    if (videoUriToPlay != null) {
        Dialog(
            onDismissRequest = { videoUriToPlay = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ExoPlayerView(videoUri = videoUriToPlay!!, baseUrl = currentBaseUrl)
                    IconButton(
                        onClick = { videoUriToPlay = null },
                        modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MediaItemGrid(
    media: StopPointMediaEntity,
    isThumbnail: Boolean,
    currentBaseUrl: String,
    onPlayVideo: (String) -> Unit,
    onSetThumbnail: (String?) -> Unit,
    onDelete: (StopPointMediaEntity) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.LightGray)
            .combinedClickable(
                onClick = {
                    if (media.mediaType == "VIDEO") onPlayVideo(media.fileUri)
                    else showFullScreen = true
                },
                onLongClick = { showMenu = true }
            )
    ) {
        val safeUrl = NetworkConfig.getFullImageUrl(media.fileUri, currentBaseUrl)

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(safeUrl)
                .addHeader("ngrok-skip-browser-warning", "true")
                .apply {
                    // 🚀 FIX XỌC XANH: Luôn trích xuất frame nếu là video
                    if (media.mediaType == "VIDEO") {
                        decoderFactory(VideoFrameDecoder.Factory())
                    }
                }
                .crossfade(true)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_background)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (media.mediaType == "VIDEO") {
            Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(0.9f), modifier = Modifier.align(Alignment.Center).size(36.dp))
        }

        // Hiện ngôi sao nếu là ảnh đại diện (cho cả ảnh và video)
        if (isThumbnail) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                shadowElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Thumbnail",
                    modifier = Modifier.padding(4.dp).size(14.dp),
                    tint = Color.White
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            // 🚀 MỞ KHÓA THUMBNAIL CHO CẢ VIDEO VÀ ẢNH
            if (media.mediaType == "IMAGE" || media.mediaType == "VIDEO") {
                DropdownMenuItem(
                    text = { Text(if (isThumbnail) "Hủy đại diện" else "Làm đại diện") },
                    onClick = { onSetThumbnail(if (isThumbnail) null else media.fileUri); showMenu = false },
                    leadingIcon = {
                        Icon(
                            imageVector = if (media.mediaType == "VIDEO") Icons.Default.VideoFile else Icons.Default.PhotoSizeSelectActual,
                            contentDescription = null
                        )
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Xóa", color = Color.Red) },
                onClick = { onDelete(media); showMenu = false },
                leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = Color.Red) }
            )
        }
    }

    if (showFullScreen) {
        FullScreenImageViewer(
            imageUrl = NetworkConfig.getFullImageUrl(media.fileUri, currentBaseUrl),
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
    val animatedOffset by animateOffsetAsState(targetValue = offset, label = "offset")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().onGloballyPositioned { containerSize = it.size },
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .build(),
                    contentDescription = null,
                    onSuccess = { state ->
                        imageSize = IntSize(state.result.drawable.intrinsicWidth, state.result.drawable.intrinsicHeight)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffset.x,
                            translationY = animatedOffset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                val containerSizeFloat = containerSize.toSize()
                                val ratio = minOf(containerSizeFloat.width / imageSize.width, containerSizeFloat.height / imageSize.height)
                                val fittedImageSize = Size(imageSize.width * ratio, imageSize.height * ratio)
                                val zoomedImageSize = Size(fittedImageSize.width * newScale, fittedImageSize.height * newScale)

                                val maxOffsetX = maxOf(0f, (zoomedImageSize.width - containerSizeFloat.width) / 2f)
                                val maxOffsetY = maxOf(0f, (zoomedImageSize.height - containerSizeFloat.height) / 2f)

                                scale = newScale
                                offset = if (newScale > 1f) {
                                    Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else Offset.Zero
                            }
                        },
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ExoPlayerView(videoUri: String, baseUrl: String) {
    val context = LocalContext.current
    val processedUrl = NetworkConfig.getFullImageUrl(videoUri, baseUrl)

    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(processedUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(
        factory = {
            androidx.media3.ui.PlayerView(context).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}