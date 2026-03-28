@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ued.custommaps.viewmodel.MapViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(navController: NavController, viewModel: MapViewModel) {
    val journeys by viewModel.journeys.observeAsState(initial = emptyList())
    // KHẮC PHỤC LỖI: Lấy giá trị searchQuery đúng cách
    val searchQuery by viewModel.searchQuery

    var showDialog by remember { mutableStateOf(false) }
    var newMapTitle by remember { mutableStateOf("") }
    val filteredJourneys = journeys.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Hành trình của tôi", fontWeight = FontWeight.Bold) })
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) }, // Fix lỗi Unresolved updateSearchQuery
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Tìm kiếm...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            items(filteredJourneys) { journey ->
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                    navController.navigate("map_detail/${journey.id}")
                }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(journey.title, fontWeight = FontWeight.Bold)
                            Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(journey.startTime)), color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.deleteMap(journey) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo mới") },
            text = { OutlinedTextField(value = newMapTitle, onValueChange = { newMapTitle = it }, label = { Text("Tên") }) },
            confirmButton = {
                Button(onClick = {
                    // ĐÃ FIX: Chỉ truyền 1 tham số title vào đây thôi
                    viewModel.createMap(newMapTitle)
                    showDialog = false
                    newMapTitle = ""
                }) {
                    Text("Tạo")
                }
            }
        )
    }
}