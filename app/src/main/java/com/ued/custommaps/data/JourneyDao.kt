package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {

    // ==========================================
    // 🗺️ 1. HÀNH TRÌNH (JOURNEYS)
    // ==========================================

    @Query("SELECT * FROM journeys WHERE userId = :currentUserId AND isDeleted = 0 ORDER BY startTime DESC")
    fun getAllJourneys(currentUserId: Int): Flow<List<JourneyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Update
    suspend fun updateJourney(journey: JourneyEntity)

    @Query("SELECT * FROM journeys WHERE isSynced = 0")
    suspend fun getUnsyncedJourneys(): List<JourneyEntity>

    // 🚀 SOFT DELETE: Đánh dấu xóa mềm
    @Query("UPDATE journeys SET isDeleted = 1, isSynced = 0 WHERE id = :journeyId")
    suspend fun softDeleteJourney(journeyId: Long)

    @Query("UPDATE journeys SET isSynced = 1 WHERE id = :journeyId")
    suspend fun markJourneyAsSynced(journeyId: Long)

    @Query("UPDATE journeys SET isSynced = 0 WHERE id = :journeyId")
    suspend fun markJourneyAsUnsynced(journeyId: Long)

    @Query("UPDATE journeys SET startLat = :lat, startLon = :lon, isSynced = 0 WHERE id = :id")
    suspend fun updateJourneyStartLocation(id: Long, lat: Double, lon: Double)

    // ==========================================
    // 📍 2. TỌA ĐỘ (TRACK POINTS)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE journeyId = :journeyId ORDER BY timestamp ASC")
    fun getTrackPoints(journeyId: Long): Flow<List<TrackPointEntity>>

    // ==========================================
    // 📸 3. ĐIỂM DỪNG & MEDIA (STOP POINTS)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoint(point: StopPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: StopPointMediaEntity)

    @Transaction
    @Query("SELECT * FROM stop_points WHERE id = :stopId")
    fun getStopPointById(stopId: Long): Flow<StopPointWithMedia>

    @Query("UPDATE stop_points SET note = :note, is_synced = 0 WHERE id = :stopId")
    suspend fun updateStopPointNote(stopId: Long, note: String)

    @Query("UPDATE stop_points SET thumbnail_uri = :uri, is_synced = 0 WHERE id = :stopId")
    suspend fun updateStopPointThumbnail(stopId: Long, uri: String?)

    @Delete
    suspend fun deleteSingleMedia(media: StopPointMediaEntity)

    @Query("UPDATE stop_points SET is_deleted = 1, is_synced = 0 WHERE id IN (:ids)")
    suspend fun softDeleteStopPointsBatch(ids: List<Long>)

    @Transaction
    @Query("SELECT * FROM stop_points WHERE journeyId = :journeyId")
    suspend fun getStopPointsForSync(journeyId: Long): List<StopPointWithMedia>

    // ==========================================
    // 🗑️ 4. CHUỖI LỆNH DỌN DẸP SẠCH (HARD DELETE)
    // ==========================================

    @Query("DELETE FROM stop_point_media WHERE parentStopId IN (SELECT id FROM stop_points WHERE journeyId = :journeyId)")
    suspend fun deleteMediaByJourney(journeyId: Long)

    @Query("DELETE FROM stop_points WHERE journeyId = :journeyId")
    suspend fun deleteStopPointsByJourney(journeyId: Long)

    @Query("DELETE FROM track_points WHERE journeyId = :journeyId")
    suspend fun deleteTrackPointsByJourney(journeyId: Long)

    @Query("DELETE FROM journeys WHERE id = :journeyId")
    suspend fun deleteJourneyById(journeyId: Long)

    // 🚀 GOM CHUNG LẠI THÀNH 1 LỆNH DUY NHẤT CHO REPOSITORY GỌI
    @Transaction
    suspend fun hardDeleteJourneyCompletely(journeyId: Long) {
        deleteMediaByJourney(journeyId)
        deleteStopPointsByJourney(journeyId)
        deleteTrackPointsByJourney(journeyId)
        deleteJourneyById(journeyId)
    }

    @Query("SELECT * FROM stop_point_media WHERE parentStopId = :stopId")
    fun getMediaForStopPoint(stopId: Long): kotlinx.coroutines.flow.Flow<List<StopPointMediaEntity>>

    @Query("UPDATE stop_point_media SET fileUri = :newUri WHERE id = :mediaId")
    suspend fun updateMediaUri(mediaId: Long, newUri: String): Int // Trả về số dòng đã sửa

    // 🚀 2. Lấy StopPoint trực tiếp (Vá lỗi getStopPointByIdDirect)
    @Query("SELECT * FROM stop_points WHERE id = :id")
    suspend fun getStopPointByIdDirect(id: Long): StopPointEntity?

    // 🚀 3. Lấy Media trực tiếp (Dùng cho đóng gói JSON)
    @Query("SELECT * FROM stop_point_media WHERE parentStopId = :stopId")
    suspend fun getMediaForStopPointDirect(stopId: Long): List<StopPointMediaEntity>

    // Thêm vào JourneyDao.kt
    // 🗑️ Xóa cứng 1 điểm
    // 🗑️ Chuyển trạng thái thành Đã Xóa
    @Query("UPDATE stop_points SET is_deleted = 1 WHERE id = :stopId")
    suspend fun deleteStopPointById(stopId: Long)

    @Query("UPDATE stop_points SET is_deleted = 1 WHERE journeyId = :journeyId")
    suspend fun deleteAllStopPointsOfJourney(journeyId: Long)

    // 🚀 CỰC KỲ QUAN TRỌNG: Ẩn các điểm đã xóa khỏi bản đồ
    @Transaction
    @Query("SELECT * FROM stop_points WHERE journeyId = :journeyId AND is_deleted = 0")
    fun getStopPointsWithMedia(journeyId: Long): Flow<List<StopPointWithMedia>>
}