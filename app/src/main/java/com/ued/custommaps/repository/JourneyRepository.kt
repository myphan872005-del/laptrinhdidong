package com.ued.custommaps.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(
    private val journeyDao: JourneyDao
) {
    // --- JOURNEYS ---
    fun getAllJourneys(userId: Int): Flow<List<JourneyEntity>> = journeyDao.getAllJourneys(userId)
    suspend fun insertJourney(journey: JourneyEntity): Long = journeyDao.insertJourney(journey)
    suspend fun softDeleteJourney(journeyId: Long) = journeyDao.softDeleteJourney(journeyId)
    suspend fun getUnsyncedJourneys(): List<JourneyEntity> = journeyDao.getUnsyncedJourneys()

    // --- TRACK POINTS ---
    suspend fun insertTrackPoint(point: TrackPointEntity) = journeyDao.insertTrackPoint(point)
    fun getTrackPoints(journeyId: Long): Flow<List<TrackPointEntity>> = journeyDao.getTrackPoints(journeyId)

    // --- STOP POINTS & MEDIA ---
    suspend fun insertStopPoint(point: StopPointEntity): Long = journeyDao.insertStopPoint(point)
    suspend fun insertMedia(media: StopPointMediaEntity) = journeyDao.insertMedia(media)
    fun getStopPointsWithMedia(journeyId: Long): Flow<List<StopPointWithMedia>> = journeyDao.getStopPointsWithMedia(journeyId)

    // =========================================================================
    // ĐÂY LÀ 5 HÀM MÌNH BỔ SUNG ĐỂ SỬA 50 LỖI ĐỎ TRONG VIEWMODEL NHÉ:
    // =========================================================================
    fun getStopPointById(stopId: Long): Flow<StopPointWithMedia> = journeyDao.getStopPointById(stopId)

    suspend fun updateStopPointNote(stopId: Long, note: String) = journeyDao.updateStopPointNote(stopId, note)

    suspend fun updateStopPointThumbnail(stopId: Long, uri: String?) = journeyDao.updateStopPointThumbnail(stopId, uri)

    suspend fun softDeleteStopPointsBatch(ids: List<Long>) = journeyDao.softDeleteStopPointsBatch(ids)

    suspend fun deleteSingleMedia(media: StopPointMediaEntity) = journeyDao.deleteSingleMedia(media)
}