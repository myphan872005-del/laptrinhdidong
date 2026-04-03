package com.ued.custommaps.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

// ==========================================
// 🗺️ MODELS CŨ (DÀNH CHO BẢN ĐỒ & TRACKING)
// ==========================================
data class GeoPointData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomMarker(
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String = ""
)

data class CustomMap(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val markers: List<CustomMarker> = emptyList(),
    val polyline: List<GeoPointData> = emptyList(),
    val isTracking: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MapStyle {
    NORMAL, SATELLITE, TERRAIN
}

// ==========================================
// 🌍 MODELS MỚI (DÀNH CHO TÍNH NĂNG KHÁM PHÁ - API)
// ==========================================

// 1. Lớp ngoài cùng hứng trực tiếp từ API (/api/discovery)
data class DiscoveryPost(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("original_journey_id") val originalJourneyId: String,
    @SerializedName("display_name") val authorName: String?,
    @SerializedName("avatar_url") val authorAvatar: String?,
    @SerializedName("likes_count") val likesCount: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("payload") val payload: DiscoveryPayload?
)

// 2. Các Model phụ trợ để parse object "payload"
data class DiscoveryPayload(
    @SerializedName("journey") val journey: DiscoveryJourneyInfo,
    @SerializedName("track_points") val trackPoints: List<DiscoveryTrackPoint>? = emptyList(),
    @SerializedName("stop_points") val stopPoints: List<DiscoveryStopPoint>? = emptyList()
)

data class DiscoveryJourneyInfo(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("start_lat") val startLat: Double,
    @SerializedName("start_lon") val startLon: Double,
    @SerializedName("start_time") val startTime: Long
)

// 3. Các Model phục vụ vẽ bản đồ Read-Only
data class DiscoveryTrackPoint(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class DiscoveryStopPoint(
    @SerializedName("local_id") val id: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("note") val note: String?,
    @SerializedName("media") val media: List<DiscoveryMedia>? = emptyList()
)

data class DiscoveryMedia(
    @SerializedName("file_uri") val fileUri: String,
    @SerializedName("media_type") val mediaType: String // "IMAGE" hoặc "VIDEO"
)