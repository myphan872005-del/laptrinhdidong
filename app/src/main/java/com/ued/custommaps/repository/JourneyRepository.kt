package com.ued.custommaps.repository

import com.ued.custommaps.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(private val dao: JourneyDao) {
    val allJourneys: Flow<List<JourneyEntity>> = dao.getAllJourneys()

    suspend fun startNewJourney(title: String, lat: Double, lon: Double): Long {
        return dao.insertJourney(
            JourneyEntity(
                title = title,
                startTime = System.currentTimeMillis(),
                startLat = lat,
                startLon = lon
            )
        )
    }

    // FIX TẠI ĐÂY: Đổi Int thành Long cho segmentId
    suspend fun addTrackPoint(journeyId: Long, lat: Double, lon: Double, segmentId: Long) {
        dao.insertTrackPoint(
            TrackPointEntity(
                journeyId = journeyId,
                latitude = lat,
                longitude = lon,
                timestamp = System.currentTimeMillis(),
                segmentId = segmentId
            )
        )
    }

    suspend fun addStopPoint(journeyId: Long, lat: Double, lon: Double, note: String, image: String? = null) {
        dao.insertStopPoint(
            StopPointEntity(
                journeyId = journeyId,
                latitude = lat,
                longitude = lon,
                timestamp = System.currentTimeMillis(),
                note = note,
                imagePath = image
            )
        )
    }

    fun getTrackPoints(journeyId: Long) = dao.getTrackPoints(journeyId)
    fun getStopPoints(journeyId: Long) = dao.getStopPoints(journeyId)
    suspend fun deleteJourney(journey: JourneyEntity) = dao.deleteJourney(journey)
}