package com.ued.custommaps

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ued.custommaps.ui.*
import com.ued.custommaps.ui.theme.UEDCustomMapsTheme
import com.ued.custommaps.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. Khai báo trình xin quyền vị trí
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Kích hoạt Popup xin quyền ngay khi vào App
        requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )

        // 3. Cấu hình OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContent {
            UEDCustomMapsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MapViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController, viewModel) }

        composable(
            route = "map_detail/{mapId}",
            arguments = listOf(navArgument("mapId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getString("mapId") ?: ""
            MapDetailScreen(mapId, navController, viewModel)
        }

        composable(
            route = "stop_detail/{stopId}",
            arguments = listOf(navArgument("stopId") { type = NavType.LongType })
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments?.getLong("stopId") ?: -1L
            StopPointDetailScreen(stopId, navController, viewModel)
        }
    }
}