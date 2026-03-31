package com.ued.custommaps.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ued.custommaps.data.JourneyRepository
import com.ued.custommaps.data.SessionManager
import com.ued.custommaps.network.ApiService
import com.ued.custommaps.network.SyncJourneyRequest
import com.ued.custommaps.network.SyncMedia
import com.ued.custommaps.network.SyncStopPoint
import com.ued.custommaps.network.SyncTrackPoint
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: JourneyRepository,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "👷 [BẮT ĐẦU] Công nhân đã được gọi dậy!")

        try {
            val session = sessionManager.userSession.firstOrNull()
            if (session == null) {
                Log.e("SyncWorker", "❌ [LỖI] Chưa đăng nhập!")
                return Result.failure()
            }

            val token = "Bearer ${session.token}"
            val unsyncedJourneys = repository.getUnsyncedJourneys()
            Log.d("SyncWorker", "📦 [TÌM KIẾM] Tìm thấy ${unsyncedJourneys.size} mục cần xử lý.")

            if (unsyncedJourneys.isEmpty()) return Result.success()

            var isAllSuccess = true

            for (journey in unsyncedJourneys) {
                // 1. Kiểm tra xem đây là hành trình mới hay hành trình vừa bị xóa
                val deleteFlag = if (journey.isDeleted) 1 else 0
                val statusText = if (journey.isDeleted) "XÓA" else "MỚI/CẬP NHẬT"

                Log.d("SyncWorker", "🚀 [XỬ LÝ] Hành trình: ${journey.title} | Trạng thái: $statusText")

                val trackPointsList = repository.getTrackPoints(journey.id).firstOrNull() ?: emptyList()

                // 🔥 ĐÃ SỬA Ở ĐÂY: Dùng hàm mới lấy TẤT CẢ (kể cả đã xóa) để đồng bộ, và bỏ firstOrNull() vì hàm mới trả về List thẳng
                val stopPointsList = repository.getStopPointsForSync(journey.id)

                val syncTrackPoints = trackPointsList.map {
                    SyncTrackPoint(it.segmentId, it.latitude, it.longitude, it.timestamp)
                }

                val syncStopPoints = stopPointsList.map { sp ->
                    SyncStopPoint(
                        local_id = sp.stopPoint.id, latitude = sp.stopPoint.latitude,
                        longitude = sp.stopPoint.longitude, note = sp.stopPoint.note,
                        thumbnail_uri = sp.stopPoint.thumbnailUri, timestamp = sp.stopPoint.id,
                        is_deleted = if (sp.stopPoint.isDeleted) 1 else 0,
                        mediaList = sp.mediaList.map { m -> SyncMedia(m.id, m.fileUri, m.mediaType) }
                    )
                }

                val request = SyncJourneyRequest(
                    id = journey.id,
                    title = journey.title,
                    startTime = journey.startTime,
                    startLat = journey.startLat,
                    startLon = journey.startLon,
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = deleteFlag, // GỬI TRẠNG THÁI THỰC TẾ LÊN SERVER
                    isPublic = 0,
                    trackPoints = syncTrackPoints,
                    stopPoints = syncStopPoints
                )

                val response = apiService.syncJourney(token, request)

                if (response.isSuccessful) {
                    if (journey.isDeleted) {
                        // NẾU LÀ LỆNH XÓA THÀNH CÔNG -> XÓA HẲN KHỎI MÁY (HARD DELETE)
                        Log.d("SyncWorker", "🗑️ [DB] Đã xóa vĩnh viễn ${journey.title} khỏi máy.")
                        // Lưu ý: Cần viết thêm hàm deleteJourney thẳng trong Repository/Dao nếu chưa có
                        // Nếu chưa có, tạm thời cứ đánh dấu isSynced = 1 để nó biến mất khỏi hàng đợi
                        repository.insertJourney(journey.copy(isSynced = true))
                    } else {
                        Log.d("SyncWorker", "🎉 [DB] Đã đồng bộ thành công ${journey.title}")
                        repository.insertJourney(journey.copy(isSynced = true))
                    }
                } else {
                    Log.e("SyncWorker", "💥 [SERVER ERROR] ${response.code()}: ${response.errorBody()?.string()}")
                    isAllSuccess = false
                }
            }

            return if (isAllSuccess) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e("SyncWorker", "🔥 [ERROR] ${e.message}")
            return Result.retry()
        }
    }
}