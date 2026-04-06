package com.ued.custommaps.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ued.custommaps.R
import com.ued.custommaps.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: MapViewModel) {
    val userSession by viewModel.userSession.collectAsState()
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            isUploading = true
            // Chỉ định rõ kiểu Boolean cho success
            viewModel.uploadAvatar(selectedUri, context) { success: Boolean ->
                isUploading = false
                if (success) {
                    Toast.makeText(context, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi khi tải ảnh lên!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông tin cá nhân") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Khu vực hiển thị Avatar to rõ nét
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    // Nhớ thêm 1 cái ảnh ic_default_avatar vào thư mục res/drawable nhé
                    model = userSession?.avatarUrl ?: R.drawable.ic_launcher_background,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                // Nút bấm nhỏ hình cái máy ảnh để đổi ảnh
                FloatingActionButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                }

                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(120.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hiển thị thông tin tài khoản
            Text(
                // Nếu UserSession của Hoan không có displayName thì đổi thành username nhé
                text = userSession?.username ?: "N/A",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${userSession?.username ?: "username"}",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Các nút chức năng khác
            OutlinedButton(
                onClick = { /* Làm sau */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text("Đổi mật khẩu")
            }
        }
    }
}