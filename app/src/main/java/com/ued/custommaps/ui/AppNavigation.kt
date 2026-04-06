package com.ued.custommaps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// Import ViewModels
import com.ued.custommaps.viewmodel.MapViewModel
import com.ued.custommaps.viewmodel.DiscoveryViewModel

// LƯU Ý: Xóa các dòng "import MainScreen" thủ công cũ đi.
// Nếu các file Screen nằm cùng package com.ued.custommaps.ui thì không cần import.
// Nếu nằm khác package, hãy nhấn Alt + Enter vào tên từng Screen để tự động import.

@Composable
fun AppNavigation(
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userSession by mapViewModel.userSession.collectAsState()

    // Luồng điều hướng: Nếu có session thì vào main, ngược lại bắt đăng nhập
    val startDestination = if (userSession != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // 1. MÀN HÌNH ĐĂNG NHẬP
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // 2. MÀN HÌNH ĐĂNG KÝ
        composable("register") {
            RegisterScreen(
                navController = navController,
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // 3. MÀN HÌNH CHÍNH (DANH SÁCH HÀNH TRÌNH)
        composable("main") {
            MainScreen(navController = navController, viewModel = mapViewModel)
        }

        // 4. MÀN HÌNH KHÁM PHÁ (CỘNG ĐỒNG)
        composable("discovery_screen") {
            DiscoveryScreen(navController = navController)
        }

        // 5. CHI TIẾT KHÁM PHÁ (POST TỪ SERVER)
        composable(
            route = "discovery_detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.IntType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getInt("postId") ?: 0
            val discoveryViewModel: DiscoveryViewModel = hiltViewModel()
            DiscoveryDetailScreen(postId, navController, discoveryViewModel)
        }

        // 6. CHI TIẾT BẢN ĐỒ (CỦA CÁ NHÂN)
        composable(
            route = "map_detail/{mapId}",
            arguments = listOf(navArgument("mapId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getLong("mapId") ?: -1L
            MapDetailScreen(mapId, navController, mapViewModel)
        }

        // 7. CHI TIẾT ĐIỂM DỪNG (CHECK-IN)
        composable(
            route = "stop_detail/{stopId}",
            arguments = listOf(navArgument("stopId") { type = NavType.LongType })
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments?.getLong("stopId") ?: -1L
            StopPointDetailScreen(stopId, navController, mapViewModel)
        }

        // 8. MÀN HÌNH PROFILE
        composable("profile") {
            ProfileScreen(navController = navController, viewModel = mapViewModel)
        }
    }
}