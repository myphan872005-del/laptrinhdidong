package com.ued.custommaps.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. Lưu thông tin hành trình từ Server
@Entity(tableName = "discovery_journeys")
data class DiscoveryJourneyEntity(
    @PrimaryKey val journeyId: Long,
    val title: String,
    val startTime: Long,
    val startLat: Double,
    val startLon: Double,
    val authorName: String?,
    val createdAt: String
)

// 2. Lưu các điểm chạy (Track Points) của hành trình đó
@Entity(tableName = "discovery_track_points")
data class DiscoveryTrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double
)

// 3. Lưu các điểm dừng (Stop Points)
@Entity(tableName = "discovery_stop_points")
data class DiscoveryStopPointEntity(
    @PrimaryKey val stopId: Long, // ID từ server trả về
    val journeyId: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String,
    val timestamp: Long
)

// 4. Lưu Media của từng điểm dừng
@Entity(tableName = "discovery_media")
data class DiscoveryMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stopId: Long,
    val fileUri: String,
    val mediaType: String
)