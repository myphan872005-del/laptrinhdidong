package com.ued.custommaps.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ued.custommaps.data.SessionManager
import com.ued.custommaps.models.DiscoveryPost
import com.ued.custommaps.models.DiscoveryStopPoint
import com.ued.custommaps.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

// 🚀 TRẠNG THÁI UI (Dùng để điều khiển vòng xoay Loading/Error)
sealed interface DiscoveryUiState {
    object Loading : DiscoveryUiState
    data class Success(val data: List<DiscoveryPost>) : DiscoveryUiState
    data class Error(val message: String) : DiscoveryUiState
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- 1. QUẢN LÝ DỮ LIỆU ĐIỂM DỪNG ĐƯỢC CHỌN ---
    // 🚀 Đã khai báo để fix lỗi Unresolved reference 'setSelectedStopPoint'
    private val _selectedStopPoint = mutableStateOf<DiscoveryStopPoint?>(null)
    val selectedStopPoint: State<DiscoveryStopPoint?> = _selectedStopPoint

    /**
     * Hàm dùng để cất dữ liệu điểm dừng vào "kho"
     * trước khi chuyển màn hình từ Detail sang StopDetail
     */
    fun setSelectedStopPoint(stopPoint: DiscoveryStopPoint) {
        _selectedStopPoint.value = stopPoint
    }

    private val _focusLocation = mutableStateOf<Pair<Double, Double>?>(null)
    val focusLocation: State<Pair<Double, Double>?> = _focusLocation

    // Thêm 2 hàm này vào để gọi khi cần bay đến tọa độ
    fun focusMapOn(lat: Double, lon: Double) {
        _focusLocation.value = Pair(lat, lon)
    }

    fun clearFocusLocation() {
        _focusLocation.value = null
    }


    // --- 2. QUẢN LÝ DANH SÁCH BÀI ĐĂNG (FEED) ---
    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Loading)
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        fetchDiscovery() // Tự động gọi API khi khởi tạo ViewModel
    }

    /**
     * Hàm lấy dữ liệu từ Server (Discovery Feed)
     */
    fun fetchDiscovery() {
        viewModelScope.launch {
            _uiState.value = DiscoveryUiState.Loading
            try {
                // 1. Lấy token từ SessionManager (Cần dùng firstOrNull vì là Flow)
                val session = sessionManager.userSession.firstOrNull()
                val token = session?.token

                if (!token.isNullOrBlank()) {
                    // 2. Gọi API với Token Bearer
                    val response = apiService.getDiscoveryFeed("Bearer $token")

                    if (response.isSuccessful) {
                        // 3. Cập nhật dữ liệu bài đăng vào Success State
                        val posts = response.body() ?: emptyList()
                        _uiState.value = DiscoveryUiState.Success(posts)
                    } else {
                        _uiState.value = DiscoveryUiState.Error("Lỗi Server: ${response.code()}")
                    }
                } else {
                    _uiState.value = DiscoveryUiState.Error("Vui lòng đăng nhập lại để xem cộng đồng!")
                }

            } catch (e: Exception) {
                // Bắt các lỗi văng app do mất mạng, sập server...
                _uiState.value = DiscoveryUiState.Error("Lỗi kết nối: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // 🚀 HÀM XÓA BÀI VIẾT
    fun deletePost(postId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val session = sessionManager.userSession.firstOrNull()
                val token = session?.token

                if (!token.isNullOrBlank()) {
                    val response = apiService.deleteDiscoveryPost("Bearer $token", postId)
                    if (response.isSuccessful) {
                        // Gọi thành công -> Báo ra UI -> Load lại Feed
                        onSuccess()
                        fetchDiscovery()
                    } else {
                        onError("Xóa thất bại: Lỗi ${response.code()}")
                    }
                } else {
                    onError("Lỗi xác thực, vui lòng đăng nhập lại!")
                }
            } catch (e: Exception) {
                onError("Mất kết nối máy chủ!")
            }
        }
    }
}