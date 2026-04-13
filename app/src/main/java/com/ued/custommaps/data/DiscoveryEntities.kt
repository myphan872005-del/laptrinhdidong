package com.ued.custommaps.data

import androidx.room.*

// ==========================================
// 🏗️ BỘ BẢNG CACHE CHO MỤC KHÁM PHÁ
// ==========================================

@Entity(tableName = "discovery_journeys")
data class DiscoveryJourneyEntity(
    @PrimaryKey val journeyId: Long, // 🚀 Dùng ID từ Server (Original ID)
    val title: String,
    val startTime: Long,
    val startLat: Double,
    val startLon: Double,
    val authorName: String?,
    val createdAt: String
)

@Entity(
    tableName = "discovery_track_points",
    foreignKeys = [ForeignKey(
        entity = DiscoveryJourneyEntity::class,
        parentColumns = ["journeyId"],
        childColumns = ["journeyId"],
        onDelete = ForeignKey.CASCADE // Xóa hành trình -> Tự động xóa tọa độ
    )],
    indices = [Index("journeyId")]
)
data class DiscoveryTrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Cái này OK nếu sếp xóa sạch bảng rồi insert lại mỗi khi xem bài mới
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double
)
@Entity(
    tableName = "discovery_stop_points",
    foreignKeys = [ForeignKey(
        entity = DiscoveryJourneyEntity::class,
        parentColumns = ["journeyId"],
        childColumns = ["journeyId"],
        onDelete = ForeignKey.CASCADE // Xóa hành trình -> Tự động xóa điểm dừng
    )],
    indices = [Index("journeyId")]
)
data class DiscoveryStopPointEntity(
    @PrimaryKey val stopId: Long,
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    val thumbnailUri: String?, // Bổ sung ảnh đại diện
    val timestamp: Long,

)

@Entity(
    tableName = "discovery_media",
    foreignKeys = [ForeignKey(
        entity = DiscoveryStopPointEntity::class,
        parentColumns = ["stopId"],
        childColumns = ["stopId"],
        onDelete = ForeignKey.CASCADE // Xóa điểm dừng -> Tự động xóa Media
    )],
    indices = [Index("stopId")]
)
data class DiscoveryMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stopId: Long,
    val fileUri: String,
    val mediaType: String
)

// 🚀 DATA CLASS GOM NHÓM ĐỂ TRUY VẤN UI
data class DiscoveryStopPointWithMedia(
    @Embedded val stopPoint: DiscoveryStopPointEntity,
    @Relation(
        parentColumn = "stopId",
        entityColumn = "stopId"
    )
    val mediaList: List<DiscoveryMediaEntity>
)