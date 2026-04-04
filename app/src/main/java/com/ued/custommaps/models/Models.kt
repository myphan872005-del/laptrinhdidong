package com.ued.custommaps.models

import java.util.UUID

data class GeoPointData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomMarker(
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String = ""
)

data class CustomMap(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val markers: List<CustomMarker> = emptyList(),
    val polyline: List<GeoPointData> = emptyList(),
    val isTracking: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MapStyle {
    NORMAL, SATELLITE, TERRAIN
}