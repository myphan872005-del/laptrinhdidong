package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Query("SELECT * FROM journeys ORDER BY startTime DESC")
    fun getAllJourneys(): Flow<List<JourneyEntity>>

    @Update
    suspend fun updateJourney(journey: JourneyEntity)

    @Delete
    suspend fun deleteJourney(journey: JourneyEntity)

    @Insert
    suspend fun insertTrackPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE journeyId = :journeyId ORDER BY timestamp ASC")
    fun getTrackPoints(journeyId: Long): Flow<List<TrackPointEntity>>

    @Insert
    suspend fun insertStopPoint(stopPoint: StopPointEntity): Long

    @Insert
    suspend fun insertMedia(media: StopPointMediaEntity)

    @Query("DELETE FROM stop_points WHERE id IN (:stopIds)")
    suspend fun deleteStopPointsBatch(stopIds: List<Long>)

    @Query("DELETE FROM stop_point_media WHERE parentStopId IN (:stopIds)")
    suspend fun deleteMediaByStopIds(stopIds: List<Long>)

    @Transaction
    @Query("SELECT * FROM stop_points WHERE journeyId = :journeyId ORDER BY timestamp DESC")
    fun getStopPointsWithMedia(journeyId: Long): Flow<List<StopPointWithMedia>>

    @Transaction
    @Query("SELECT * FROM stop_points WHERE id = :stopId")
    fun getStopPointById(stopId: Long): Flow<StopPointWithMedia>

    @Query("UPDATE stop_points SET note = :newNote WHERE id = :stopId")
    suspend fun updateStopPointNote(stopId: Long, newNote: String)

    @Query("UPDATE stop_points SET thumbnailUri = :newThumbnailUri WHERE id = :stopId")
    suspend fun updateStopPointThumbnail(stopId: Long, newThumbnailUri: String?)

    @Delete
    suspend fun deleteSingleMedia(media: StopPointMediaEntity)
}