package com.ued.custommaps.data

import androidx.room.*

@Entity(tableName = "journeys")
data class JourneyEntity(
    // Bỏ autoGenerate, dùng thời gian thực làm ID để đồng bộ Server không bị trùng
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "userId") val userId: Int, // BẮT BUỘC CÓ để phân biệt bài ai nấy xem
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "startTime") val startTime: Long,
    @ColumnInfo(name = "startLat") val startLat: Double = 0.0,
    @ColumnInfo(name = "startLon") val startLon: Double = 0.0,
    @ColumnInfo(name = "isPublic") val isPublic: Int = 0,
    @ColumnInfo(name = "isSynced") val isSynced: Boolean = false, // BẮT BUỘC CÓ để biết đã đẩy lên server chưa
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false // BẮT BUỘC CÓ để xóa mềm
)

@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "journeyId") val journeyId: Long,
    @ColumnInfo(name = "segmentId") val segmentId: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "isSynced") val isSynced: Boolean = false
)

@Entity(tableName = "stop_points")
data class StopPointEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "journeyId") val journeyId: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "thumbnailUri") val thumbnailUri: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isSynced") val isSynced: Boolean = false,
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false
)

@Entity(tableName = "stop_point_media")
data class StopPointMediaEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "parentStopId") val parentStopId: Long,
    @ColumnInfo(name = "fileUri") val fileUri: String,
    @ColumnInfo(name = "mediaType") val mediaType: String,
    @ColumnInfo(name = "isSynced") val isSynced: Boolean = false
)

// Gộp luôn class này vào đây cho gọn
data class StopPointWithMedia(
    @Embedded val stopPoint: StopPointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentStopId"
    )
    val mediaList: List<StopPointMediaEntity>
)