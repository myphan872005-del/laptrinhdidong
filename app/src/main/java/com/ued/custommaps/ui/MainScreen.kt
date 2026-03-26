package com.ued.custommaps.ui

import android.Manifest // Quan trọng
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ued.custommaps.data.JourneyEntity
import com.ued.custommaps.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    val journeys by viewModel.journeys.observeAsState(initial = emptyList())

    // --- BỘ CODE HỎI QUYỀN TRUY CẬP GPS ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Xử lý sau khi người dùng bấm cho phép hoặc từ chối (tùy chọn)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    // --------------------------------------

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hành trình của tôi") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tạo mới")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Tìm kiếm hành trình...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            val filtered = viewModel.getFilteredMaps()
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Chưa có hành trình nào") }
            } else {
                LazyColumn {
                    items(filtered) { item ->
                        MapItem(journey = item,
                            onClick = { navController.navigate("map/${item.id}") },
                            onDelete = { viewModel.deleteMap(item) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Bắt đầu hành trình mới") },
            text = {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Tên hành trình") })
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitle.isNotBlank()) {
                        try {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { location ->
                                    val lat = location?.latitude ?: 16.068
                                    val lon = location?.longitude ?: 108.210
                                    viewModel.createMap(newTitle, lat, lon)
                                    showDialog = false
                                    newTitle = ""
                                }
                        } catch (e: SecurityException) {
                            viewModel.createMap(newTitle, 16.068, 108.210)
                            showDialog = false
                        }
                    }
                }) { Text("Tạo") }
            }
        )
    }
}

@Composable
fun MapItem(journey: JourneyEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(journey.title, style = MaterialTheme.typography.titleMedium)
                Text("Bắt đầu: ${journey.startTime}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}