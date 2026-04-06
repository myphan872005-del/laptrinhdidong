package com.ued.custommaps

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ued.custommaps.ui.AppNavigation
import com.ued.custommaps.ui.theme.UEDCustomMapsTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration

@AndroidEntryPoint //  Để Hilt khởi tạo và bơm dữ liệu cho toàn bộ App
class MainActivity : ComponentActivity() {

    // Trình xin quyền vị trí ngay khi mở App (Quan trọng cho OSMDroid)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Xin quyền GPS
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        // 2. Cấu hình OSMDroid để load bản đồ mượt mà
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        // Thiết lập User Agent cho OSMDroid (Tránh bị chặn server bản đồ)
        Configuration.getInstance().userAgentValue = packageName

        // 3. Giao diện Compose
        setContent {
            UEDCustomMapsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    AppNavigation()
                }
            }
        }
    }
}