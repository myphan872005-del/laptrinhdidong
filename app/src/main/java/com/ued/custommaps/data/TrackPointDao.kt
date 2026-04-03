package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Thêm cái này để nếu trùng ID thì nó ghi đè, tránh crash
    suspend fun insertPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE journeyId = :jId ORDER BY timestamp ASC")
    fun getPointsForJourney(jId: String): Flow<List<TrackPointEntity>>

    @Query("DELETE FROM track_points WHERE journeyId = :jId")
    suspend fun deletePointsForJourney(jId: String)

    // Thêm hàm này để lấy dữ liệu nhanh khi cần Sync lên Server
    @Query("SELECT * FROM track_points WHERE journeyId = :jId ORDER BY timestamp ASC")
    suspend fun getPointsList(jId: String): List<TrackPointEntity>
}