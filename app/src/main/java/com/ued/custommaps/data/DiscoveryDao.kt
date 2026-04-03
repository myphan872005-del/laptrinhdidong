package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {
    // Chèn dữ liệu mới (Nếu trùng ID thì ghi đè - chuẩn Cache)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: DiscoveryJourneyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<DiscoveryTrackPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoints(stops: List<DiscoveryStopPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<DiscoveryMediaEntity>)

    // Truy vấn dữ liệu để hiển thị lên Map
    @Query("SELECT * FROM discovery_journeys WHERE journeyId = :id")
    fun getJourneyById(id: Long): Flow<DiscoveryJourneyEntity?>

    @Query("SELECT * FROM discovery_track_points WHERE journeyId = :id")
    fun getTrackPoints(id: Long): Flow<List<DiscoveryTrackPointEntity>>

    @Query("SELECT * FROM discovery_stop_points WHERE journeyId = :id")
    fun getStopPoints(id: Long): Flow<List<DiscoveryStopPointEntity>>

    @Query("SELECT * FROM discovery_media WHERE stopId = :stopId")
    fun getMediaForStop(stopId: Long): Flow<List<DiscoveryMediaEntity>>

    // Xóa cache cũ nếu cần
    @Query("DELETE FROM discovery_journeys")
    suspend fun clearCache()
}