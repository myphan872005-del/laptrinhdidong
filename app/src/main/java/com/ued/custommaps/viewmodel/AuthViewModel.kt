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

    // 🚀 BỔ SUNG: Giúp UI LoginScreen gọi cập nhật URL động dễ dàng
    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            sessionManager.updateServerUrl(url)
        }
    }

    fun login(username: String, password: String) {
        // Chặn trường hợp sếp quên nhập liệu
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ tài khoản và mật khẩu!")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(AuthRequest(username, password))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val user = body.user
                    val token = body.token

                    if (user != null && token != null) {
                        // Lưu phiên đăng nhập chuẩn xác
                        sessionManager.saveSession(
                            id = user.id,
                            token = token,
                            username = user.username,
                            displayName = user.displayName,
                            avatarUrl = user.avatarUrl ?: ""
                        )
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error("Dữ liệu Server trả về bị thiếu!")
                    }
                } else {
                    _authState.value = AuthState.Error("Sai tài khoản hoặc mật khẩu!")
                }
            } catch (e: Exception) {
                // 🚀 GỢI Ý: Nếu lỗi kết nối, có thể do link Ngrok sai hoặc Server chưa bật
                _authState.value = AuthState.Error("Kết nối thất bại! Hãy kiểm tra lại link Server/Ngrok.")
            }
        }
    }

    fun register(username: String, password: String) {
        if (username.length < 4 || password.length < 4) {
            _authState.value = AuthState.Error("Tài khoản và mật khẩu phải có ít nhất 4 ký tự!")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.register(AuthRequest(username, password))
                if (response.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Tên tài khoản đã tồn tại!")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi kết nối Server!")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}