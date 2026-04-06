package com.ued.custommaps.network

import com.google.gson.annotations.SerializedName
import com.ued.custommaps.data.JourneyEntity
import com.ued.custommaps.models.DiscoveryPost
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ==========================================
// 1. AUTHENTICATION MODELS
// ==========================================
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
    @SerializedName("avatarUrl") val avatarUrl: String
)

// ==========================================
// 2. SYNC MODELS (ÉP CHUẨN SNAKE_CASE CHO NODE.JS)
// ==========================================
// --- SYNC MODELS ---
data class SyncJourneyRequest(
    @SerializedName("local_id") val localId: Long,
    val title: String,
    @SerializedName("start_time") val startTime: Long,
    @SerializedName("start_lat") val startLat: Double,
    @SerializedName("start_lon") val startLon: Double,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("is_deleted") val isDeleted: Int,
    @SerializedName("isPublic") val isPublic: Int,
    @SerializedName("trackPoints") val trackPoints: List<SyncTrackPoint>,
    @SerializedName("stopPoints") val stopPoints: List<SyncStopPoint>
)

data class SyncTrackPoint(
    @SerializedName("segment_id") val segmentId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class SyncStopPoint(
    @SerializedName("local_id") val localId: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    @SerializedName("thumbnail_uri") val thumbnailUri: String?,
    val timestamp: Long,
    @SerializedName("is_deleted") val isDeleted: Int,
    @SerializedName("media") val mediaList: List<SyncMedia>
)

data class SyncMedia(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("file_uri") val fileUri: String,
    @SerializedName("media_type") val mediaType: String
)

data class SyncResponse(
    val message: String,
    val serverId: Long? = null
)

// ==========================================
// 3. DISCOVERY & PUBLISH MODELS
// =========================================

data class PublishRequest(
    @SerializedName("journeyId") val journeyId: Long,
    val title: String,
    @SerializedName("thumbnailUri") val thumbnailUri: String?,
    val payload: Any // Snapshot JSON cực lớn
)

data class SimpleResponse(val message: String)

data class UploadMediaResponse(
    val message: String,
    val data: List<MediaItemResponse>? = null
)

data class MediaItemResponse(
    val originalName: String,
    val serverPath: String,
    val mediaType: String?
)

data class SingleMediaResponse(
    val message: String,
    val url: String
)

// ==========================================
// 4. RETROFIT INTERFACE
// ==========================================
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @Multipart
    @POST("api/uploads/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part
    ): Response<UploadResponse>

    // 🔄 ĐỒNG BỘ: Đi mua cơm xong bấm nút này nè sếp!
    @POST("api/journeys/sync")
    suspend fun syncJourney(
        @Header("Authorization") token: String,
        @Body request: SyncJourneyRequest
    ): Response<SyncResponse>

    // 🌍 CỘNG ĐỒNG: Đưa hành trình lên tab Khám phá
    @POST("api/journeys/publish")
    suspend fun publishJourney(
        @Header("Authorization") token: String,
        @Body request: PublishRequest
    ): Response<SimpleResponse>

    @Multipart
    @POST("api/uploads/multiple")
    suspend fun uploadMultipleMedia(
        @Header("Authorization") token: String,
        @Part files: List<MultipartBody.Part>
    ): Response<UploadMediaResponse>

    @Headers("ngrok-skip-browser-warning: true")
    @GET("api/discovery")
    suspend fun getDiscoveryFeed(
        @Header("Authorization") token: String
    ): Response<List<DiscoveryPost>>

    @DELETE("api/discovery/{id}")
    suspend fun deleteDiscoveryPost(
        @Header("Authorization") token: String,
        @Path("id") postId: Long
    ): Response<Unit>

    @Multipart
    @POST("api/uploads/single")
    suspend fun uploadSingleMedia(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Response<SingleMediaResponse>
}