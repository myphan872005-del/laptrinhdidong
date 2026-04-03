package com.ued.custommaps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ued.custommaps.models.DiscoveryPost
import com.ued.custommaps.repository.DiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: DiscoveryRepository
) : ViewModel() {

    // Trạng thái giao diện, mặc định là Loading
    private val _uiState = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Loading)
    val uiState: StateFlow<DiscoveryUiState> = _uiState

    init {
        // Tự động load dữ liệu ngay khi vào màn hình Khám Phá
        fetchDiscoveryData()
    }

    // Hàm gọi API (Có thể gọi lại khi người dùng kéo xuống để refresh)
    fun fetchDiscoveryData() {
        viewModelScope.launch {
            _uiState.value = DiscoveryUiState.Loading
            try {
                // Gọi API lấy danh sách bài đăng từ Repository
                val data = repository.getDiscoveryFeed()
                _uiState.value = DiscoveryUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = DiscoveryUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}

// Định nghĩa các trạng thái của màn hình Khám phá
sealed class DiscoveryUiState {
    object Loading : DiscoveryUiState()

    // Đã đổi 'Any' thành 'List<DiscoveryPost>' chuẩn chỉ
    data class Success(val data: List<DiscoveryPost>) : DiscoveryUiState()

    data class Error(val message: String) : DiscoveryUiState()
}