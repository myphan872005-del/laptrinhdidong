package com.ued.custommaps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ued.custommaps.models.CustomMap
import com.ued.custommaps.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MapViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UED Custom Maps") },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Tìm kiếm...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            val maps = viewModel.getFilteredMaps()

            if (maps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có bản đồ")
                }
            } else {
                LazyColumn {
                    items(maps) { map ->
                        MapItem(map,
                            onClick = { navController.navigate("map/${map.id}") },
                            onDelete = { viewModel.deleteMap(map.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo bản đồ mới") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Tên bản đồ") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitle.isNotBlank()) {
                        viewModel.createMap(newTitle)
                        newTitle = ""
                        showDialog = false
                    }
                }) { Text("Tạo") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun MapItem(map: CustomMap, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(map.title, fontWeight = FontWeight.Bold)
                Text("${map.markers.size} địa điểm", color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}
