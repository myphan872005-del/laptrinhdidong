package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {

    // --- JOURNEYS ---

    // Lấy hành trình của User đang đăng nhập và chưa bị xóa
    @Query("SELECT * FROM journeys WHERE userId = :currentUserId AND isDeleted = 0 ORDER BY startTime DESC")
    fun getAllJourneys(currentUserId: Int): Flow<List<JourneyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Update
    suspend fun updateJourney(journey: JourneyEntity)

    // Lấy các hành trình cần đồng bộ lên server (Dùng cho WorkManager)
    @Query("SELECT * FROM journeys WHERE isSynced = 0")
    suspend fun getUnsyncedJourneys(): List<JourneyEntity>

    // Xóa mềm: Không xóa hẳn mà chỉ đổi cờ isDeleted = 1 để còn đồng bộ lên Server
    @Query("UPDATE journeys SET isDeleted = 1, isSynced = 0 WHERE id = :journeyId")
    suspend fun softDeleteJourney(journeyId: Long)

    // Đánh dấu đã đồng bộ xong
    @Query("UPDATE journeys SET isSynced = 1 WHERE id = :journeyId")
    suspend fun markJourneyAsSynced(journeyId: Long)

    // --- TRACK POINTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE journeyId = :journeyId ORDER BY timestamp ASC")
    fun getTrackPoints(journeyId: Long): Flow<List<TrackPointEntity>>

    // --- STOP POINTS & MEDIA ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoint(point: StopPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: StopPointMediaEntity)

    @Transaction
    @Query("SELECT * FROM stop_points WHERE journeyId = :journeyId AND isDeleted = 0")
    fun getStopPointsWithMedia(journeyId: Long): Flow<List<StopPointWithMedia>>

    @Transaction
    @Query("SELECT * FROM stop_points WHERE id = :stopId")
    fun getStopPointById(stopId: Long): Flow<StopPointWithMedia>

    @Query("UPDATE stop_points SET note = :note, isSynced = 0 WHERE id = :stopId")
    suspend fun updateStopPointNote(stopId: Long, note: String)

    @Query("UPDATE stop_points SET thumbnailUri = :uri, isSynced = 0 WHERE id = :stopId")
    suspend fun updateStopPointThumbnail(stopId: Long, uri: String?)

    @Delete
    suspend fun deleteSingleMedia(media: StopPointMediaEntity)

    // Xóa mềm Điểm dừng
    @Query("UPDATE stop_points SET isDeleted = 1, isSynced = 0 WHERE id IN (:ids)")
    suspend fun softDeleteStopPointsBatch(ids: List<Long>)

    // Trong JourneyDao.kt
    @Query("UPDATE journeys SET isSynced = 0 WHERE id = :journeyId")
    suspend fun markJourneyAsUnsynced(journeyId: Long)

    // Hàm LẤY TẤT CẢ (kể cả đã xóa) để đồng bộ lên Server
    @Transaction
    @Query("SELECT * FROM stop_points WHERE journeyId = :journeyId")
    suspend fun getStopPointsForSync(journeyId: Long): List<StopPointWithMedia>
}