package com.ued.custommaps.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Khởi tạo DataStore
private val Context.dataStore by preferencesDataStore(name = "user_session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = intPreferencesKey("user_id") // ID từ MySQL là INT
        val USERNAME = stringPreferencesKey("username")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
    }

    // Luồng dữ liệu UserSession để UI (Compose) tự động cập nhật
    val userSession: Flow<UserSession?> = context.dataStore.data.map { preferences ->
        val token = preferences[TOKEN]
        val userId = preferences[USER_ID]

        if (token.isNullOrEmpty() || userId == null) {
            null // Chưa đăng nhập
        } else {
            UserSession(
                id = userId,
                token = token,
                username = preferences[USERNAME] ?: "",
                displayName = preferences[DISPLAY_NAME] ?: "",
                avatarUrl = preferences[AVATAR_URL] ?: ""
            )
        }
    }

    suspend fun saveSession(id: Int, token: String, username: String, displayName: String, avatarUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[TOKEN] = token
            prefs[USERNAME] = username
            prefs[DISPLAY_NAME] = displayName
            prefs[AVATAR_URL] = avatarUrl
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}

// Data class chứa thông tin phiên
data class UserSession(
    val id: Int,
    val token: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String
)