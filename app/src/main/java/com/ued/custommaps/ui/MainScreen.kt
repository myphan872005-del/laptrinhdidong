package com.ued.custommaps.ui

import android.Manifest
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    // Quan sát dữ liệu
    val journeys by viewModel.journeys.observeAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery

    // Logic lọc danh sách (Tự động chạy khi journeys hoặc searchQuery đổi)
    val filteredJourneys = remember(searchQuery, journeys) {
        if (searchQuery.isBlank()) journeys
        else journeys.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    // Xin quyền (Giữ nguyên)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hành trình của tôi") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Tìm kiếm hành trình...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            if (filteredJourneys.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "Chưa có hành trình nào" else "Không tìm thấy kết quả")
                }
            } else {
                LazyColumn {
                    items(filteredJourneys) { item ->
                        MapItem(
                            journey = item,
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
            title = { Text("Tạo hành trình mới") },
            text = { OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Tên chuyến đi") }) },
            confirmButton = {
                Button(onClick = {
                    if (newTitle.isNotBlank()) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc ->
                                viewModel.createMap(newTitle, loc?.latitude ?: 16.0, loc?.longitude ?: 108.0)
                                showDialog = false; newTitle = ""
                            }
                    }
                }) { Text("Bắt đầu") }
            }
        )
    }
}

// HÀM BỊ THIẾU LÚC NÃY ĐÂY HOAN ƠI!
@Composable
fun MapItem(journey: JourneyEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(journey.title, style = MaterialTheme.typography.titleMedium)
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(journey.startTime))
                Text("Bắt đầu: $date", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
            }
        }
    }
}