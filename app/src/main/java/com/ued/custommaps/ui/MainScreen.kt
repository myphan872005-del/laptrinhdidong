@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.ued.custommaps.R
import com.ued.custommaps.viewmodel.MapViewModel
import com.ued.custommaps.worker.SyncWorker
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(navController: NavController, viewModel: MapViewModel) {
    val journeys by viewModel.journeys.observeAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery
    val userSession by viewModel.userSession.collectAsState()
    val context = LocalContext.current

    // 🚀 Lấy link Ngrok động
    val currentBaseUrl by viewModel.currentBaseUrl

    var showDialog by remember { mutableStateOf(false) }
    var newMapTitle by remember { mutableStateOf("") }
    val filteredJourneys = journeys.filter { it.title.contains(searchQuery, ignoreCase = true) }

    // Client lấy vị trí
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = {
                            Column {
                                Text("UED Custom Maps", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Text("Hành trình của ${userSession?.username ?: "bạn"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        },
                        actions = {
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.padding(end = 16.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(com.ued.custommaps.network.NetworkConfig.getFullImageUrl(userSession?.avatarUrl, currentBaseUrl))                                        .addHeader("ngrok-skip-browser-warning", "true")
                                        .crossfade(true)
                                        .error(R.drawable.ic_launcher_background)
                                        .build(),
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .clickable { expanded = true },
                                    contentScale = ContentScale.Crop
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Thông tin cá nhân") },
                                        leadingIcon = { Icon(Icons.Default.Person, null) },
                                        onClick = { expanded = false; navController.navigate("profile") }
                                    )
                                    Divider()
                                    DropdownMenuItem(
                                        text = { Text("Đăng xuất", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            expanded = false
                                            viewModel.logout()
                                            navController.navigate("login") { popUpTo(0) }
                                        }
                                    )
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tìm tên chuyến đi...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledIconButton(
                            onClick = {
                                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                                WorkManager.getInstance(context).enqueue(syncRequest)
                                Toast.makeText(context, "🔄 Đang đồng bộ...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(50.dp)
                        ) { Icon(Icons.Default.Sync, null) }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filteredJourneys.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("Chưa có dữ liệu nào", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredJourneys) { journey ->
                        JourneyCard(
                            journey = journey,
                            onClick = {
                                // 🚀 FIX 1: Tự động định vị camera về vị trí bắt đầu khi click vào Card
                                viewModel.focusLocation.value = Pair(journey.startLat, journey.startLon)
                                navController.navigate("map_detail/${journey.id}")
                            },
                            onDelete = { viewModel.deleteMap(journey) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            LargeFloatingActionButton(
                onClick = { navController.navigate("discovery_screen") },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(65.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Explore, null, modifier = Modifier.size(28.dp))
                    Text("Cộng đồng", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Bắt đầu chuyến đi mới", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newMapTitle,
                    onValueChange = { newMapTitle = it },
                    label = { Text("Tên hành trình (Vd: Phượt Đà Lạt)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMapTitle.isNotBlank()) {
                            // 🚀 FIX 2: Capture cái title lại ngay lập tức để không bị rỗng khi lấy tọa độ xong
                            val finalTitle = newMapTitle

                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    val lat = location?.latitude ?: 0.0
                                    val lon = location?.longitude ?: 0.0
                                    viewModel.createMap(finalTitle, lat, lon) // Dùng finalTitle
                                    showDialog = false
                                    newMapTitle = ""
                                }
                            } else {
                                viewModel.createMap(finalTitle, 0.0, 0.0)
                                showDialog = false
                                newMapTitle = ""
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Tạo Map") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Hủy") } }
        )
    }
}

@Composable
fun JourneyCard(journey: com.ued.custommaps.data.JourneyEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = journey.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (journey.isSynced) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (journey.isSynced) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault()).format(Date(journey.startTime)),
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFEF5350))
            }
        }
    }
}