package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 🚜 DATA ACCESS OBJECT CHO KHÁM PHÁ
// ==========================================

@Dao
interface DiscoveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: DiscoveryJourneyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<DiscoveryTrackPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoints(stops: List<DiscoveryStopPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<DiscoveryMediaEntity>)

    @Query("SELECT * FROM discovery_journeys WHERE journeyId = :id")
    fun getJourneyById(id: Long): Flow<DiscoveryJourneyEntity?>

    @Query("SELECT * FROM discovery_track_points WHERE journeyId = :id ORDER BY id ASC")
    fun getTrackPoints(id: Long): Flow<List<DiscoveryTrackPointEntity>>

    // Lấy nguyên cụm StopPoint + Media trả thẳng ra UI
    @Transaction
    @Query("SELECT * FROM discovery_stop_points WHERE journeyId = :id")
    fun getStopPointsWithMedia(id: Long): Flow<List<DiscoveryStopPointWithMedia>>

    // Gọi 1 lệnh này là dọn sạch sẽ toàn bộ Cache nhờ tính năng CASCADE
    @Query("DELETE FROM discovery_journeys")
    suspend fun clearCache()
}