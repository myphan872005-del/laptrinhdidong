@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

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

    var showDialog by remember { mutableStateOf(false) }
    var newMapTitle by remember { mutableStateOf("") }
    val filteredJourneys = journeys.filter { it.title.contains(searchQuery, ignoreCase = true) }

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

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Thông tin cá nhân") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("profile")
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Đăng xuất", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error)
                                    },
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

                // Thanh tìm kiếm và Nút Test Đồng Bộ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Tìm kiếm...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // NÚT TEST ĐỒNG BỘ
                    FilledIconButton(
                        onClick = {
                            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                            WorkManager.getInstance(context).enqueue(syncRequest)
                            Toast.makeText(context, "Đã ra lệnh Đồng bộ ngầm!", Toast.LENGTH_SHORT).show()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Now")
                    }
                }

            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) { Icon(Icons.Default.Add, contentDescription = "Thêm mới") }
        }
    ) { padding ->

        // DÙNG BOX LÀM VỎ BỌC CHO TOÀN BỘ PHẦN THÂN ĐỂ CHO PHÉP CHỒNG LÊN NHAU
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 1. Phần hiển thị List Journey
            if (filteredJourneys.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có hành trình nào. Hãy tạo mới!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 80.dp // Thêm bottom padding để list không bị che bởi nút Khám phá
                    )
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

            // 2. NÚT KHÁM PHÁ NẰM ĐỘC LẬP Ở GÓC DƯỚI TRÁI
            Button(
                onClick = { navController.navigate("discovery_screen") },
                shape = RectangleShape, // Ép nút thành hình vuông
                modifier = Modifier
                    .align(Alignment.BottomStart) // Bây giờ thì Alignment này hoàn toàn hợp lệ
                    .padding(16.dp)
                    .size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Discovery",
                    modifier = Modifier.size(32.dp) // Cho Icon to lên một chút cho đẹp
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo hành trình mới", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newMapTitle,
                    onValueChange = { newMapTitle = it },
                    label = { Text("Tên hành trình") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newMapTitle.isNotBlank()) {
                        viewModel.createMap(newMapTitle)
                        showDialog = false
                        newMapTitle = ""
                    }
                }) { Text("Tạo ngay") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun JourneyCard(journey: com.ued.custommaps.data.JourneyEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(journey.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (journey.isSynced) {
                        Icon(Icons.Default.Done, contentDescription = "Synced", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = "Unsynced", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(journey.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFE57373))
            }
        }
    }
}