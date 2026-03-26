package com.ued.custommaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ued.custommaps.ui.MainScreen
import com.ued.custommaps.ui.MapDetailScreen
import com.ued.custommaps.ui.theme.UEDCustomMapsTheme
import com.ued.custommaps.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration

@AndroidEntryPoint // BẮT BUỘC: Để Hilt "bơm" ViewModel vào các màn hình bên dưới
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cấu hình OSMDroid để load bản đồ mượt mà
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Sử dụng hiltViewModel() để tự động lấy MapViewModel đã được "bơm" Repository
    // Bạn không cần dòng MapRepository.getInstance(context) ở đây nữa!
    val viewModel: MapViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "main") {
        // Màn hình Danh sách Hành trình
        composable("main") {
            MainScreen(navController, viewModel)
        }

        // Màn hình Chi tiết Bản đồ
        composable(
            route = "map/{mapId}",
            arguments = listOf(navArgument("mapId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getString("mapId")
            if (mapId != null) {
                // MapDetailScreen nhận mapId để lấy đúng dữ liệu từ Room
                MapDetailScreen(mapId, navController, viewModel)
            }
        }
    }
}