package com.ued.custommaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ued.custommaps.repository.MapRepository
import com.ued.custommaps.ui.MainScreen
import com.ued.custommaps.ui.MapDetailScreen
import com.ued.custommaps.ui.theme.UEDCustomMapsTheme
import com.ued.custommaps.viewmodel.MapViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    val context = LocalContext.current
    // Sửa dòng này:
    val repository = remember { MapRepository.getInstance(context) }
    val viewModel = remember { MapViewModel(repository) }
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, viewModel)
        }
        composable(
            route = "map/{mapId}",
            arguments = listOf(navArgument("mapId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getString("mapId")
            if (mapId != null) {
                MapDetailScreen(mapId, navController, viewModel)
            }
        }
    }
}
