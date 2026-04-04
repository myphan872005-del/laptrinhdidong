package com.ued.custommaps.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long,
    val startLat: Double,
    val startLon: Double,
    var endTime: Long? = null,
    var totalDistance: Double = 0.0
)

@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(entity = JourneyEntity::class, parentColumns = ["id"], childColumns = ["journeyId"], onDelete = ForeignKey.CASCADE)]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val pointId: Long = 0,
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val segmentId: Long // Đổi từ Int sang Long
)

@Entity(
    tableName = "stop_points",
    foreignKeys = [ForeignKey(entity = JourneyEntity::class, parentColumns = ["id"], childColumns = ["journeyId"], onDelete = ForeignKey.CASCADE)]
)
data class StopPointEntity(
    @PrimaryKey(autoGenerate = true) val stopId: Long = 0,
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val note: String,
    val imagePath: String? = null // Đường dẫn ảnh (Uri string)
)