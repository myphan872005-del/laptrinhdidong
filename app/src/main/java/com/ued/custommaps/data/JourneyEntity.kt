package com.ued.custommaps.data

import androidx.room.*

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long,
    val startLat: Double = 0.0,
    val startLon: Double = 0.0
)

@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val segmentId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

@Entity(tableName = "stop_points")
data class StopPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String,
    // Add this for custom thumbnails (Ý 3)
    val thumbnailUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stop_point_media")
data class StopPointMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentStopId: Long,
    val fileUri: String,
    val mediaType: String // "IMAGE" hoặc "VIDEO"
)

data class StopPointWithMedia(
    @Embedded val stopPoint: StopPointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentStopId"
    )
    val mediaList: List<StopPointMediaEntity>
)