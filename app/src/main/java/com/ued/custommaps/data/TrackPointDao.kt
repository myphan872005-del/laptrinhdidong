package com.ued.custommaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {

    // 🚀 LƯU Ý: Đã đổi String thành Long để khớp 100% với BIGINT trên MySQL

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE journeyId = :journeyId ORDER BY timestamp ASC")
    fun getPointsForJourney(journeyId: Long): Flow<List<TrackPointEntity>>

    @Query("DELETE FROM track_points WHERE journeyId = :journeyId")
    suspend fun deletePointsForJourney(journeyId: Long)

    // Hàm lấy danh sách thẳng (không qua Flow) để anh công nhân SyncWorker bốc lên Server
    @Query("SELECT * FROM track_points WHERE journeyId = :journeyId ORDER BY timestamp ASC")
    suspend fun getPointsList(journeyId: Long): List<TrackPointEntity>
}