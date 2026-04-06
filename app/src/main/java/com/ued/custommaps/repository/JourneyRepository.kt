package com.ued.custommaps.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(
    private val journeyDao: JourneyDao,
    private val trackPointDao: TrackPointDao, // 🚀 BỔ SUNG: Tiêm TrackPointDao vào đây
    private val discoveryDao: DiscoveryDao
) {

    // ==========================================
    // 🗺️ 1. HÀNH TRÌNH (JOURNEYS)
    // ==========================================

    fun getAllJourneys(userId: Int): Flow<List<JourneyEntity>> = journeyDao.getAllJourneys(userId)

    suspend fun insertJourney(journey: JourneyEntity): Long = journeyDao.insertJourney(journey)

    // Đánh dấu xóa (Soft Delete) để chờ SyncWorker đẩy lệnh xóa lên Server
    suspend fun softDeleteJourney(journeyId: Long) = journeyDao.softDeleteJourney(journeyId)

    // 🚀 ĐÃ FIX: Gọi hàm Transaction "Hủy diệt diện rộng" anh em mình viết ở bước trước
    // Để đảm bảo xóa hành trình là bay sạch cả rác tọa độ, ảnh, video
    suspend fun hardDeleteJourneyCompletely(journeyId: Long) {
        journeyDao.hardDeleteJourneyCompletely(journeyId)
    }

    suspend fun getUnsyncedJourneys(): List<JourneyEntity> = journeyDao.getUnsyncedJourneys()

    suspend fun markJourneyAsUnsynced(journeyId: Long) {
        journeyDao.markJourneyAsUnsynced(journeyId)
    }

    // 🚀 BỔ SUNG: Cho SyncWorker dùng sau khi đồng bộ thành công
    suspend fun markJourneyAsSynced(journeyId: Long) {
        journeyDao.markJourneyAsSynced(journeyId)
    }

    // ==========================================
    // 📍 2. TỌA ĐỘ (TRACK POINTS)
    // ==========================================

    // 🚀 ĐÃ FIX: Điều hướng sang dùng TrackPointDao
    suspend fun insertTrackPoint(point: TrackPointEntity) = trackPointDao.insertPoint(point)

    fun getTrackPoints(journeyId: Long): Flow<List<TrackPointEntity>> = trackPointDao.getPointsForJourney(journeyId)

    // 🚀 BỔ SUNG: Lấy list thẳng thớm cho SyncWorker đóng gói JSON
    suspend fun getTrackPointsList(journeyId: Long): List<TrackPointEntity> = trackPointDao.getPointsList(journeyId)

    // ==========================================
    // 📸 3. ĐIỂM DỪNG & MEDIA (STOP POINTS)
    // ==========================================

    suspend fun insertStopPoint(point: StopPointEntity): Long = journeyDao.insertStopPoint(point)

    suspend fun insertMedia(media: StopPointMediaEntity) = journeyDao.insertMedia(media)

    fun getStopPointsWithMedia(journeyId: Long): Flow<List<StopPointWithMedia>> = journeyDao.getStopPointsWithMedia(journeyId)

    fun getStopPointById(stopId: Long): Flow<StopPointWithMedia> = journeyDao.getStopPointById(stopId)

    suspend fun updateStopPointNote(stopId: Long, note: String) = journeyDao.updateStopPointNote(stopId, note)

    suspend fun updateStopPointThumbnail(stopId: Long, uri: String?) = journeyDao.updateStopPointThumbnail(stopId, uri)

    suspend fun softDeleteStopPointsBatch(ids: List<Long>) = journeyDao.softDeleteStopPointsBatch(ids)

    suspend fun deleteSingleMedia(media: StopPointMediaEntity) = journeyDao.deleteSingleMedia(media)

    suspend fun getStopPointsForSync(journeyId: Long) = journeyDao.getStopPointsForSync(journeyId)

    // --- LẤY MEDIA CỦA 1 ĐIỂM DỪNG CỤ THỂ ---

    // Dùng cho UI / ViewModel
    fun getMediaForStopPoint(stopId: Long): kotlinx.coroutines.flow.Flow<List<StopPointMediaEntity>> {
        return journeyDao.getMediaForStopPoint(stopId)
    }

    // Dùng cho SyncWorker / Logic ngầm
    suspend fun getMediaForStopPointDirect(stopId: Long): List<StopPointMediaEntity> {
        return journeyDao.getMediaForStopPointDirect(stopId)
    }

    // Trong JourneyRepository.kt
    suspend fun updateMediaUri(mediaId: Long, newUri: String) {
        journeyDao.updateMediaUri(mediaId, newUri)
    }

    suspend fun getStopPointByIdDirect(stopId: Long): StopPointEntity? {
        return journeyDao.getStopPointByIdDirect(stopId)
    }

    // Thêm vào JourneyRepository.kt
    suspend fun deleteStopPointById(stopId: Long) {
        journeyDao.deleteStopPointById(stopId)
    }

    suspend fun deleteAllStopPointsOfJourney(journeyId: Long) {
        journeyDao.deleteAllStopPointsOfJourney(journeyId)
    }

    // ==========================================
    // 🌍 4. KHÁM PHÁ (LOCAL CACHE)
    // ==========================================
    // Dành cho việc lưu lại các bài đăng Khám phá để xem Offline (Nếu sếp cần nâng cấp sau này)
}