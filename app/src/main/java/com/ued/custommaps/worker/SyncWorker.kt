@file:OptIn(ExperimentalMaterial3Api::class)
package com.ued.custommaps.worker

import android.content.Context
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ued.custommaps.data.JourneyRepository
import com.ued.custommaps.data.SessionManager
import com.ued.custommaps.network.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: JourneyRepository,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SYNC_DEBUG", "👷 [BẮT ĐẦU] Anh công nhân vào ca!")

        try {
            // 🔑 1. Lấy Token để "thông quan" với Server
            val session = sessionManager.userSession.firstOrNull() ?: return Result.failure()
            val token = "Bearer ${session.token}"

            // 🔍 2. Tìm hàng chưa đồng bộ
            val unsyncedJourneys = repository.getUnsyncedJourneys()
            Log.d("SYNC_DEBUG", "🔍 Tìm thấy ${unsyncedJourneys.size} hành trình mới cần đẩy lên mây")

            if (unsyncedJourneys.isEmpty()) return Result.success()

            var isAllSuccess = true
            var isUnauthorized = false // 🛑 Cờ đánh dấu Token đã chết

            for (journey in unsyncedJourneys) {
                try {
                    // ==========================================
                    // 🚀 BẢN VÁ LỖI 1: XỬ LÝ HÀNH TRÌNH BỊ XÓA
                    // ==========================================
                    if (journey.isDeleted) {
                        Log.d("SYNC_DEBUG", "🗑 Hành trình đã xóa, đang gửi tín hiệu Xóa mềm lên Server...")
                        val deleteRequest = SyncJourneyRequest(
                            localId = journey.id,
                            title = journey.title,
                            startTime = journey.startTime,
                            startLat = journey.startLat,
                            startLon = journey.startLon,
                            updatedAt = System.currentTimeMillis(),
                            isDeleted = 1, // Đánh dấu xóa mềm
                            isPublic = if (journey.isPublic) 1 else 0,
                            trackPoints = emptyList(), // Không gửi tọa độ dư thừa
                            stopPoints = emptyList()   // Không gửi ảnh dư thừa
                        )

                        val response = apiService.syncJourney(token, deleteRequest)
                        if (response.isSuccessful) {
                            repository.markJourneyAsSynced(journey.id)
                            Log.d("SYNC_DEBUG", "✨ Đã báo Server xóa thành công: ${journey.title}")
                        } else {
                            Log.e("SYNC_DEBUG", "💥 Lỗi báo Xóa (Code: ${response.code()})")
                            isAllSuccess = false
                            if (response.code() == 401) isUnauthorized = true
                        }
                        continue // Báo xóa xong thì nhảy sang hành trình tiếp theo, bỏ qua khúc dưới!
                    }

                    // ==========================================
                    // KHÚC DƯỚI LÀ XỬ LÝ HÀNH TRÌNH BÌNH THƯỜNG
                    // ==========================================
                    Log.d("SYNC_DEBUG", "📦 Đang đóng gói hành trình: ${journey.title}")

                    val stopPointsList = repository.getStopPointsForSync(journey.id)
                    val syncStopPoints = stopPointsList.map { spWithMedia ->
                        val sp = spWithMedia.stopPoint
                        val mediaToUpload = spWithMedia.mediaList.filter { media ->
                            val uri = media.fileUri ?: ""
                            uri.contains("data/user/0") || uri.startsWith("file://") || uri.startsWith("/")
                        }

                        if (mediaToUpload.isNotEmpty()) {
                            val multipartParts = mediaToUpload.mapNotNull { media ->
                                val uri = media.fileUri ?: return@mapNotNull null
                                val cleanPath = uri.removePrefix("file://")
                                val file = File(cleanPath)

                                if (file.exists()) {
                                    val mimeType = if (media.mediaType == "VIDEO") "video/mp4" else "image/jpeg"
                                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                                    MultipartBody.Part.createFormData("files", file.name, requestFile)
                                } else {
                                    null
                                }
                            }

                            if (multipartParts.isNotEmpty()) {
                                val uploadRes = apiService.uploadMultipleMedia(token, multipartParts)
                                if (uploadRes.isSuccessful) {
                                    val serverResponses = uploadRes.body()?.data ?: emptyList()
                                    mediaToUpload.forEachIndexed { index, oldMedia ->
                                        if (index < serverResponses.size) {
                                            repository.updateMediaUri(oldMedia.id, serverResponses[index].serverPath)
                                        }
                                    }
                                } else if (uploadRes.code() == 401) {
                                    isUnauthorized = true
                                }
                            }
                        }

                        val updatedMediaList = repository.getMediaForStopPointDirect(sp.id)
                        var finalThumbnail = sp.thumbnailUri
                        updatedMediaList.forEach { m ->
                            if (m.fileUri?.startsWith("/uploads") == true) {
                                val fileName = m.fileUri.substringAfterLast("/")
                                if (sp.thumbnailUri?.contains(fileName) == true) {
                                    finalThumbnail = m.fileUri
                                }
                            }
                        }

                        SyncStopPoint(
                            localId = sp.id,
                            latitude = sp.latitude,
                            longitude = sp.longitude,
                            note = sp.note,
                            thumbnailUri = finalThumbnail,
                            timestamp = sp.timestamp,
                            isDeleted = if (sp.isDeleted == 1) 1 else 0,
                            mediaList = updatedMediaList.map { m ->
                                SyncMedia(m.id, m.fileUri ?: "", m.mediaType ?: "IMAGE")
                            }
                        )
                    }

                    val trackPointsList = repository.getTrackPointsList(journey.id)

                    val syncRequest = SyncJourneyRequest(
                        localId = journey.id,
                        title = journey.title,
                        startTime = journey.startTime,
                        startLat = journey.startLat,
                        startLon = journey.startLon,
                        updatedAt = System.currentTimeMillis(),
                        isDeleted = 0,
                        isPublic = if (journey.isPublic) 1 else 0,
                        trackPoints = trackPointsList.map {
                            SyncTrackPoint(it.segmentId, it.latitude, it.longitude, it.timestamp)
                        },
                        stopPoints = syncStopPoints
                    )

                    val response = apiService.syncJourney(token, syncRequest)
                    if (response.isSuccessful) {
                        repository.markJourneyAsSynced(journey.id)
                        Log.d("SYNC_DEBUG", "✨ Đồng bộ trọn gói thành công: ${journey.title}")
                    } else {
                        Log.e("SYNC_DEBUG", "💥 Lỗi Sync API (Code: ${response.code()}): ${response.errorBody()?.string()}")
                        isAllSuccess = false
                        if (response.code() == 401) isUnauthorized = true
                    }

                } catch (e: Exception) {
                    Log.e("SYNC_DEBUG", "🔥 Lỗi tại hành trình ${journey.id}: ${e.message}")
                    isAllSuccess = false
                }
            }

            // ==========================================
            // 🚀 BẢN VÁ LỖI 2: CHỐT CHẶN VÒNG LẶP 401
            // ==========================================
            if (isUnauthorized) {
                Log.e("SYNC_DEBUG", "🛑 Lỗi 401! Hủy luôn Worker để không tốn pin vô ích chờ user login lại.")
                return Result.failure() // Failure: Hủy bỏ hoàn toàn thay vì bắt máy Retry liên tục
            }

            return if (isAllSuccess) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "💥 Lỗi hệ thống: ${e.message}")
            return Result.retry()
        }
    }
}