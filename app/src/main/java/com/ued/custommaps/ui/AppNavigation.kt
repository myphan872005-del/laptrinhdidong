package com.ued.custommaps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ued.custommaps.viewmodel.MapViewModel
import com.ued.custommaps.viewmodel.DiscoveryViewModel

@Composable
fun AppNavigation(
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userSession by mapViewModel.userSession.collectAsState(initial = null)
    val startDestination = if (userSession != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // ==========================================
        // 1. LUỒNG XÁC THỰC (AUTH)
        // ==========================================
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

        // ==========================================
        // 2. LUỒNG CHÍNH (MAIN & PROFILE)
        // ==========================================
        composable("main") {
            MainScreen(navController = navController, viewModel = mapViewModel)
        }

        composable("profile") {
            ProfileScreen(navController = navController, viewModel = mapViewModel)
        }

        // ==========================================
        // 3. LUỒNG KHÁM PHÁ (DISCOVERY)
        // ==========================================
        composable("discovery_screen") {
            DiscoveryScreen(navController = navController)
        }

        composable(
            route = "discovery_detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.LongType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getLong("postId") ?: 0L
            val discoveryViewModel: DiscoveryViewModel = hiltViewModel()

            DiscoveryDetailScreen(postId, navController, discoveryViewModel)
        }

        // 🚀 ĐÃ FIX GỌN GÀNG: Gọi đúng 2 tham số đời mới
        composable("discovery_stop_detail") { backStackEntry ->
            // Lấy lại cái ViewModel đang giữ dữ liệu ở màn discovery_detail
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("discovery_detail/{postId}")
            }
            val discoveryViewModel: DiscoveryViewModel = hiltViewModel(parentEntry)

            // 🚀 Truyền thẳng NavController và ViewModel vào, để màn hình tự lôi dữ liệu ra dùng
            DiscoveryStopPointDetail(
                navController = navController,
                viewModel = discoveryViewModel
            )
        }

        // ==========================================
        // 4. LUỒNG BẢN ĐỒ CÁ NHÂN (MAPS)
        // ==========================================
        composable(
            route = "map_detail/{mapId}",
            arguments = listOf(navArgument("mapId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getLong("mapId") ?: -1L
            MapDetailScreen(mapId, navController, mapViewModel)
        }

        composable(
            route = "stop_detail/{stopId}",
            arguments = listOf(navArgument("stopId") { type = NavType.LongType })
        ) { backStackEntry ->
            val stopId = backStackEntry.arguments?.getLong("stopId") ?: -1L
            StopPointDetailScreen(stopId, navController, mapViewModel)
        }
    }
}