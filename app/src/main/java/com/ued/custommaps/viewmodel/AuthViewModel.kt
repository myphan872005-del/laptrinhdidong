package com.ued.custommaps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ued.custommaps.data.SessionManager
import com.ued.custommaps.network.ApiService
import com.ued.custommaps.network.AuthRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Gọi API Login lên Node.js
                val response = apiService.login(AuthRequest(username, password))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val user = body.user
                    val token = body.token

                    if (user != null && token != null) {
                        // Lưu ngay vào DataStore
                        sessionManager.saveSession(
                            id = user.id,
                            token = token,
                            username = user.username,
                            displayName = user.displayName,
                            avatarUrl = user.avatarUrl ?: ""
                        )
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error("Dữ liệu từ Server không hợp lệ!")
                    }
                } else {
                    _authState.value = AuthState.Error("Sai tài khoản hoặc mật khẩu!")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Không thể kết nối đến Server: ${e.message}")
            }
        }
    }

    // Thêm hàm này vào bên trong class AuthViewModel
    fun register(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.register(AuthRequest(username, password))
                if (response.isSuccessful) {
                    // Đăng ký thành công, chúng ta có thể chuyển sang Success
                    // hoặc tự động đăng nhập cho user luôn. Ở đây ta báo Success để user quay lại Login.
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Tên tài khoản đã tồn tại hoặc không hợp lệ!")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi kết nối: ${e.message}")
            }
        }
    }

    // Reset state khi cần
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

// Lớp niêm phong (Sealed class) quản lý trạng thái UI
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}