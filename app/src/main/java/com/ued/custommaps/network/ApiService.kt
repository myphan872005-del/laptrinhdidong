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

data class UploadResponse(
    val message: String,
    val avatarUrl: String
)

// --- CÁC DATA CLASS DÀNH CHO ĐỒNG BỘ (SYNC) ---

// 1. Gói hàng tổng chứa tất cả
data class SyncJourneyRequest(
    val id: Long,           // Đổi thành Long cho khớp local_id (BIGINT)
    val title: String,
    val startTime: Long,
    val startLat: Double,
    val startLon: Double,
    val updatedAt: Long,
    val isDeleted: Int,
    val isPublic: Int,      // Thêm trường isPublic
    val trackPoints: List<SyncTrackPoint>, // Danh sách tọa độ
    val stopPoints: List<SyncStopPoint>    // Danh sách điểm dừng
)

// 2. Gói hàng con: Tọa độ
data class SyncTrackPoint(
    val segment_id: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

// 3. Gói hàng con: Điểm dừng
data class SyncStopPoint(
    val local_id: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    val thumbnail_uri: String?,
    val timestamp: Long,
    val is_deleted: Int,
    @SerializedName("media") val mediaList: List<SyncMedia> // Danh sách ảnh/video
)

// 4. Gói hàng cháu: Ảnh/Video của điểm dừng
data class SyncMedia(
    val local_id: Long,
    val file_uri: String,
    val media_type: String
)

data class SyncResponse(
    val message: String,
    val serverId: Long? // Sửa thành Long vì MySQL ID là số nguyên
)

// --- INTERFACE RETROFIT ---

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    // API Đồng bộ 1 hành trình từ A-Z
    // (Lưu ý: Mình thêm Header token vào đây để khớp với verifyToken bên Node.js nhé.
    // Nếu Hoan đang dùng Interceptor để tự nhét token thì có thể xóa dòng @Header đi)
    @POST("api/journeys/sync")
    suspend fun syncJourney(
        @Header("Authorization") token: String,
        @Body request: SyncJourneyRequest
    ): Response<SyncResponse>

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