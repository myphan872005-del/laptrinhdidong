package com.ued.custommaps.repository

import com.ued.custommaps.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(private val dao: JourneyDao) {
    val allJourneys: Flow<List<JourneyEntity>> = dao.getAllJourneys()

    suspend fun startNewJourney(title: String, lat: Double, lon: Double) {
        dao.insertJourney(JourneyEntity(title = title, startLat = lat, startLon = lon, startTime = System.currentTimeMillis()))
    }

    suspend fun deleteJourney(journey: JourneyEntity) = dao.deleteJourney(journey)
    fun getTrackPoints(journeyId: Long) = dao.getTrackPoints(journeyId)
    fun getStopPoints(journeyId: Long) = dao.getStopPointsWithMedia(journeyId)
    fun getStopPointById(stopId: Long) = dao.getStopPointById(stopId)

    // Thêm các hàm này để ViewModel gọi, thay vì gọi trực tiếp dao
    suspend fun updateJourney(journey: JourneyEntity) = dao.updateJourney(journey)
    suspend fun updateStopPointNote(stopId: Long, note: String) = dao.updateStopPointNote(stopId, note)
    suspend fun updateStopPointThumbnail(stopId: Long, uri: String?) = dao.updateStopPointThumbnail(stopId, uri)
    suspend fun deleteStopPointsBatch(ids: List<Long>) = dao.deleteStopPointsBatch(ids)
    suspend fun insertMedia(media: StopPointMediaEntity) = dao.insertMedia(media)
    suspend fun deleteSingleMedia(media: StopPointMediaEntity) = dao.deleteSingleMedia(media)

    suspend fun addStopPointWithMedia(journeyId: Long, lat: Double, lon: Double, note: String, mediaPaths: List<String>) {
        val stopId = dao.insertStopPoint(StopPointEntity(journeyId = journeyId, latitude = lat, longitude = lon, note = note))
        mediaPaths.forEach { path ->
            val type = if (path.endsWith(".mp4") || path.endsWith(".mkv")) "VIDEO" else "IMAGE"
            dao.insertMedia(StopPointMediaEntity(parentStopId = stopId, fileUri = path, mediaType = type))
        }
    }
}