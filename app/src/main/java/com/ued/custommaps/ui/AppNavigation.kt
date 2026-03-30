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
import com.ued.custommaps.viewmodel.MapViewModel

@Composable
fun AppNavigation(
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userSession by mapViewModel.userSession.collectAsState()

    // Kiểm tra đã đăng nhập chưa để chọn màn hình bắt đầu
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

        // 3. MÀN HÌNH CHÍNH
        composable("main") {
            MainScreen(navController = navController, viewModel = mapViewModel)
        }

        // 4. CHI TIẾT MAP
        composable(
            route = "map_detail/{mapId}",
            arguments = listOf(navArgument("mapId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getLong("mapId") ?: -1L
            MapDetailScreen(mapId, navController, mapViewModel)
        }

        // 5. CHI TIẾT ĐIỂM DỪNG
        composable(
            route = "stop_detail/{stopId}",
            arguments = listOf(navArgument("stopId") { type = NavType.LongType })
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments?.getLong("stopId") ?: -1L
            StopPointDetailScreen(stopId, navController, mapViewModel)
        }

        // 6. MÀN HÌNH THÔNG TIN CÁ NHÂN (PROFILE) - MỚI THÊM
        composable("profile") {
            ProfileScreen(navController = navController, viewModel = mapViewModel)
        }
    }
}