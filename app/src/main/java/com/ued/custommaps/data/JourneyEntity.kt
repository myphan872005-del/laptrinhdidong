package com.ued.custommaps.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.ColumnInfo

// ==========================================
// 1. HÀNH TRÌNH (Bảng Cha)
// ==========================================
@Entity(tableName = "journeys")
data class JourneyEntity(
    // ID trong máy tự sinh bằng Timestamp (Long), đẩy lên Server sẽ chui vào cột 'local_id' (BIGINT)
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val userId: Int = 0,
    val title: String,
    val startTime: Long,
    val startLat: Double = 0.0,
    val startLon: Double = 0.0,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false,
    val isPublic: Boolean = false
)

// ==========================================
// 2. TỌA ĐỘ ĐƯỜNG ĐI (Bảng Con của Journey)
// ==========================================
@Entity(tableName = "track_points")
data class TrackPointEntity(
    // 🚀 ĐÃ FIX: Đổi Int thành Long, và bỏ luôn autoGenerate vì Service đã tự sinh ID quá chuẩn rồi
    @PrimaryKey val id: Long,
    val journeyId: Long, // Khóa ngoại nối với JourneyEntity.id (BIGINT)
    val segmentId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

// ==========================================
// 3. ĐIỂM DỪNG CHÂN (Bảng Con của Journey)
// ==========================================
@Entity(tableName = "stop_points")
data class StopPointEntity(
    @PrimaryKey
    val id: Long, // ID gốc từ máy (Timestamp), đẩy lên Server vào cột 'id' (BIGINT)

    @ColumnInfo(name = "journeyId") // Khóa ngoại nối với JourneyEntity.id
    val journeyId: Long,

    val latitude: Double,
    val longitude: Double,
    val note: String = "",

    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String? = null,

    val timestamp: Long = System.currentTimeMillis(),

    // 🚀 ĐÂY LÀ "TỬ HUYỆT" - Phải có @ColumnInfo và kiểu Int
    @ColumnInfo(name = "is_deleted")
    var isDeleted: Int = 0,

    @ColumnInfo(name = "is_synced")
    var isSynced: Boolean = false
)


// ==========================================
// 4. MEDIA CỦA ĐIỂM DỪNG (Bảng Con của StopPoint)
// ==========================================
@Entity(tableName = "stop_point_media")
data class StopPointMediaEntity(
    @PrimaryKey val id: Long = System.currentTimeMillis(), // Đẩy lên Server thành local_id (BIGINT)
    val parentStopId: Long, // Khóa ngoại nối với StopPointEntity.id
    val fileUri: String,
    val mediaType: String // "IMAGE" hoặc "VIDEO"
)

// ==========================================
// 5. DATA CLASS KẾT HỢP (Dùng để Query 1 chạm)
// ==========================================
data class StopPointWithMedia(
    @Embedded val stopPoint: StopPointEntity,
    @Relation(
        parentColumn = "id",         // Trỏ vào id của StopPointEntity
        entityColumn = "parentStopId" // Trỏ vào khóa ngoại của StopPointMediaEntity
    )
    val mediaList: List<StopPointMediaEntity>
)