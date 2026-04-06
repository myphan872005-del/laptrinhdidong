@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ued.custommaps.R
import com.ued.custommaps.data.JourneyEntity
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

    // Logic lấy vị trí
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showDialog by remember { mutableStateOf(false) }
    var newMapTitle by remember { mutableStateOf("") }

    // Logic lọc danh sách
    val filteredJourneys = remember(searchQuery, journeys) {
        if (searchQuery.isBlank()) journeys
        else journeys.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    // Xin quyền vị trí khi vào App
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Hành trình của tôi", fontWeight = FontWeight.Bold) },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userSession?.avatarUrl ?: R.drawable.ic_launcher_background)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { expanded = true },
                                contentScale = ContentScale.Crop
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Thông tin cá nhân") },
                                    leadingIcon = { Icon(Icons.Default.Person, null) },
                                    onClick = { expanded = false; navController.navigate("profile") }
                                )
                                HorizontalDivider()
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
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Tìm kiếm...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                            WorkManager.getInstance(context).enqueue(syncRequest)
                            Toast.makeText(context, "Đã ra lệnh Đồng bộ!", Toast.LENGTH_SHORT).show()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Icon(Icons.Default.Refresh, null) }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Add, contentDescription = "Thêm mới")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filteredJourneys.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "Chưa có hành trình nào" else "Không tìm thấy kết quả", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
                ) {
                    items(filteredJourneys) { journey ->
                        JourneyCard(
                            journey = journey,
                            onClick = { navController.navigate("map_detail/${journey.id}") },
                            onDelete = { viewModel.deleteMap(journey) }
                        )
                    }
                }
            }

            // NÚT KHÁM PHÁ (Bản đồ chung)
            Button(
                onClick = { navController.navigate("discovery_screen") },
                shape = RectangleShape,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Icon(Icons.Default.Explore, contentDescription = "Discovery", modifier = Modifier.size(32.dp)) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo hành trình mới", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = newMapTitle, onValueChange = { newMapTitle = it }, label = { Text("Tên hành trình") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (newMapTitle.isNotBlank()) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc ->
                                viewModel.createMap(newMapTitle, loc?.latitude ?: 16.0, loc?.longitude ?: 108.0)
                                showDialog = false; newMapTitle = ""
                            }
                    }
                }) { Text("Bắt đầu") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Hủy") } }
        )
    }
}

@Composable
fun JourneyCard(journey: JourneyEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp).padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(journey.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (journey.isSynced) {
                        Icon(Icons.Default.Done, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    }
                }
                val date = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(journey.startTime))
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373)) }
        }
    }
}