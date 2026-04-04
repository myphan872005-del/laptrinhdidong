package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(trackPoint: TrackPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoint(stopPoint: StopPointEntity)

    @Update
    suspend fun updateJourney(journey: JourneyEntity)

    @Delete
    suspend fun deleteJourney(journey: JourneyEntity)

    @Query("SELECT * FROM journeys ORDER BY startTime DESC")
    fun getAllJourneys(): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM track_points WHERE journeyId = :journeyId ORDER BY timestamp ASC")
    fun getTrackPoints(journeyId: Long): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM stop_points WHERE journeyId = :journeyId ORDER BY timestamp DESC")
    fun getStopPoints(journeyId: Long): Flow<List<StopPointEntity>>
}