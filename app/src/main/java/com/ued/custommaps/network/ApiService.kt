package com.ued.custommaps.network

import com.google.gson.annotations.SerializedName
import com.ued.custommaps.data.JourneyEntity
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// --- CÁC DATA CLASS REQUEST & RESPONSE ---

data class AuthRequest(val username: String, val password: String)

data class AuthResponse(
    val message: String,
    val token: String?,
    val user: UserDto?
)

data class UserDto(
    val id: Int,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)

// Data class hứng kết quả trả về sau khi upload ảnh thành công
data class UploadResponse(
    val message: String,
    val avatarUrl: String
)

data class SyncJourneyRequest(
    val id: String, // local_id (UUID)
    val title: String,
    val startTime: Long,
    val startLat: Double,
    val startLon: Double,
    val updatedAt: Long,
    val isDeleted: Int
)

data class SyncResponse(val message: String, val serverId: String?)

// --- INTERFACE RETROFIT ---

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    // API Đồng bộ 1 hành trình
    @POST("api/journeys/sync")
    suspend fun syncJourney(@Body journey: SyncJourneyRequest): Response<SyncResponse>

    // Khám phá mạng xã hội
    @GET("api/journeys/public")
    suspend fun getPublicJourneys(): Response<List<JourneyEntity>>

    // --- TÍNH NĂNG AVATAR ---
    @Multipart
    @POST("api/auth/upload-avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part
    ): Response<UploadResponse>
}