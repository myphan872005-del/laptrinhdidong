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

            for (journey in unsyncedJourneys) {
                try {
                    if (journey.isDeleted) continue // Hành trình nào xóa rồi thì thôi

                    Log.d("SYNC_DEBUG", "📦 Đang đóng gói hành trình: ${journey.title}")

                    // --- XỬ LÝ ĐIỂM DỪNG (STOP POINTS) ---
                    val stopPointsList = repository.getStopPointsForSync(journey.id)

                    val syncStopPoints = stopPointsList.map { spWithMedia ->
                        val sp = spWithMedia.stopPoint

                        // 📤 3. Upload Media (Ảnh/Video) của điểm dừng này
                        val mediaToUpload = spWithMedia.mediaList.filter { media ->
                            val uri = media.fileUri ?: ""
                            // Chỉ upload những file còn nằm ở bộ nhớ máy (local)
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
                                    Log.e("SYNC_DEBUG", "    ❌ File không tồn tại: $cleanPath")
                                    null
                                }
                            }

                            if (multipartParts.isNotEmpty()) {
                                Log.d("SYNC_DEBUG", "    📤 Đang gửi ${multipartParts.size} file lên Server...")
                                val uploadRes = apiService.uploadMultipleMedia(token, multipartParts)

                                if (uploadRes.isSuccessful) {
                                    val serverResponses = uploadRes.body()?.data ?: emptyList()
                                    // Cập nhật lại đường dẫn Server vào Database ở máy
                                    mediaToUpload.forEachIndexed { index, oldMedia ->
                                        if (index < serverResponses.size) {
                                            val serverPath = serverResponses[index].serverPath
                                            repository.updateMediaUri(oldMedia.id, serverPath)
                                            Log.d("SYNC_DEBUG", "    📝 Đã đổi link máy thành link Server: $serverPath")
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Lấy lại list media MỚI (đã có link server) để gửi kèm JSON Sync
                        val updatedMediaList = repository.getMediaForStopPointDirect(sp.id)

                        // 🚀 5. HOÁN ĐỔI THUMBNAIL: Đảm bảo thumbnail gửi lên là link Server
                        var finalThumbnail = sp.thumbnailUri
                        updatedMediaList.forEach { m ->
                            if (m.fileUri?.startsWith("/uploads") == true) {
                                val fileName = m.fileUri.substringAfterLast("/")
                                // Nếu thumbnail cũ của sếp có chứa tên file vừa upload -> lấy link server luôn
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

                    // --- XỬ LÝ TỌA ĐỘ (TRACK POINTS) ---
                    val trackPointsList = repository.getTrackPointsList(journey.id)

                    // 📊 Log này cực quan trọng để sếp check xem có bao nhiêu điểm đã được ghi
                    Log.d("SYNC_DEBUG", "📊 [KIỂM TRA] Hành trình '${journey.title}' có ${trackPointsList.size} tọa độ.")

                    // 6. Gửi "Gói hàng tổng thể" lên API Sync
                    val syncRequest = SyncJourneyRequest(
                        localId = journey.id,
                        title = journey.title,
                        startTime = journey.startTime,
                        startLat = journey.startLat,
                        startLon = journey.startLon,
                        updatedAt = System.currentTimeMillis(),
                        isDeleted = if (journey.isDeleted) 1 else 0,
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
                    }

                } catch (e: Exception) {
                    Log.e("SYNC_DEBUG", "🔥 Lỗi tại hành trình ${journey.id}: ${e.message}")
                    isAllSuccess = false
                }
            }

            return if (isAllSuccess) Result.success() else Result.retry()

        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "💥 Lỗi hệ thống: ${e.message}")
            return Result.retry()
        }
    }
}