package com.ued.custommaps.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import coil.imageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.ued.custommaps.TrackingService
import com.ued.custommaps.data.*
import com.ued.custommaps.network.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: JourneyRepository,
    private val sessionManager: SessionManager,
    private val apiService: ApiService
) : ViewModel() {

    // --- 1. QUẢN LÝ PHIÊN ĐĂNG NHẬP ---
    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession: StateFlow<UserSession?> = _userSession
    val focusLocation = mutableStateOf<Pair<Double, Double>?>(null)

    val currentBaseUrl = mutableStateOf(NetworkConfig.BASE_URL)

    init {
        viewModelScope.launch {
            sessionManager.userSession.collect { session ->
                _userSession.value = session
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val journeys: LiveData<List<JourneyEntity>> = userSession.flatMapLatest { session ->
        if (session != null) repository.getAllJourneys(session.id)
        else flowOf(emptyList())
    }.asLiveData()

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }

    // --- 2. LOGIC BẢN ĐỒ & TRACKING ---
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    var isTracking = mutableStateOf(false)
    var currentSegmentId = mutableLongStateOf(System.currentTimeMillis())

    // 🚀 Đã tối ưu Action để khớp với Service và không gây lỗi
    fun toggleTracking(context: Context, journeyId: Long) {
        val intent = Intent(context, TrackingService::class.java)

        if (!isTracking.value) {
            currentSegmentId.longValue = System.currentTimeMillis()
            intent.action = "START_TRACKING" // Khớp với lệnh bên TrackingService
            intent.putExtra("JOURNEY_ID", journeyId)
            intent.putExtra("SEGMENT_ID", currentSegmentId.longValue)

            ContextCompat.startForegroundService(context, intent)
            isTracking.value = true
        } else {
            intent.action = "STOP"
            context.startService(intent)
            isTracking.value = false
        }
    }

    // 🚀 Hàm hỗ trợ đồng bộ trạng thái (Gọi từ UI nếu cần)
    fun syncTrackingState(context: Context) {
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val runningServices = manager.getRunningServices(Int.MAX_VALUE)
            if (runningServices != null) {
                for (service in runningServices) {
                    if (TrackingService::class.java.name == service.service.className) {
                        isTracking.value = true
                        return
                    }
                }
            }
            isTracking.value = false
        } catch (e: Exception) {
            isTracking.value = false
        }
    }

    // --- 3. LOGIC HÀNH TRÌNH ---
    fun updateStopPointNote(stopId: Long, note: String) {
        viewModelScope.launch { repository.updateStopPointNote(stopId, note) }
    }

    fun updateStopPointThumbnail(stopId: Long, uri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStopPointThumbnail(stopId, uri)
            _stopPointBitmaps.remove(stopId)
            val sp = repository.getStopPointByIdDirect(stopId)
            sp?.journeyId?.let { repository.markJourneyAsUnsynced(it) }
            Log.d("MapViewModel", "🔄 Đã đổi thumbnail cho điểm $stopId, xóa cache marker.")
        }
    }

    fun deleteMedia(media: StopPointMediaEntity) {
        viewModelScope.launch { repository.deleteSingleMedia(media) }
    }

    fun addMoreMediaToStop(context: Context, journeyId: Long, stopId: Long, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                uris.forEach { uri ->
                    saveFileToInternal(context, uri)?.let { path ->
                        val type = if (context.contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"
                        repository.insertMedia(StopPointMediaEntity(
                            parentStopId = stopId,
                            fileUri = path,
                            mediaType = type
                        ))
                    }
                }
                repository.markJourneyAsUnsynced(journeyId)
                Log.d("MapViewModel", "✅ Đã chuyển hành trình $journeyId về trạng thái CHƯA ĐỒNG BỘ")
            } catch (e: Exception) {
                Log.e("MapViewModel", "💥 Lỗi thêm media: ${e.message}")
            }
        }
    }

    fun deleteMap(journey: JourneyEntity) {
        viewModelScope.launch {
            repository.softDeleteJourney(journey.id)
            _stopPointBitmaps.clear()
        }
    }

    fun createMap(title: String, lat: Double = 0.0, lon: Double = 0.0) {
        val currentUserId = userSession.value?.id ?: return
        viewModelScope.launch {
            val newJourney = JourneyEntity(
                id = System.currentTimeMillis(),
                userId = currentUserId,
                title = title,
                startTime = System.currentTimeMillis(),
                startLat = lat,
                startLon = lon,
                isSynced = false
            )
            repository.insertJourney(newJourney)
        }
    }

    // --- 4. LOGIC ĐIỂM DỪNG & MEDIA ---
    fun getMediaForPublish(journeyId: Long): LiveData<List<StopPointMediaEntity>> {
        return repository.getStopPointsWithMedia(journeyId).map { list ->
            list.flatMap { it.mediaList }.filter { it.mediaType == "IMAGE" }
        }.asLiveData()
    }

    fun addStopPointWithMedia(context: Context, journeyId: Long, lat: Double, lon: Double, note: String, uris: List<Uri>) {
        viewModelScope.launch {
            val stopPointId = System.currentTimeMillis()
            var firstSavedPath: String? = null

            val newStopPoint = StopPointEntity(id = stopPointId, journeyId = journeyId, latitude = lat, longitude = lon, note = note)
            repository.insertStopPoint(newStopPoint)

            uris.forEachIndexed { index, uri ->
                saveFileToInternal(context, uri)?.let { path ->
                    if (index == 0) firstSavedPath = path
                    val type = if (context.contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"
                    repository.insertMedia(StopPointMediaEntity(id = System.currentTimeMillis() + index, parentStopId = stopPointId, fileUri = path, mediaType = type))
                }
            }
            if (firstSavedPath != null) repository.updateStopPointThumbnail(stopPointId, firstSavedPath)
            repository.markJourneyAsUnsynced(journeyId)
        }
    }

    // --- 5. LOGIC LOAD THUMBNAIL CHO MARKER ---
    private val _stopPointBitmaps = mutableStateMapOf<Long, Bitmap?>()
    val stopPointBitmaps: Map<Long, Bitmap?> = _stopPointBitmaps

    fun loadBitmapsForStopPoints(context: Context, stopPoints: List<StopPointWithMedia>) {
        val imageLoader = context.imageLoader
        val baseUrl = currentBaseUrl.value

        stopPoints.forEach { item ->
            val sp = item.stopPoint
            val url = NetworkConfig.getFullImageUrl(sp.thumbnailUri, baseUrl)

            if (url.isNotBlank() && !_stopPointBitmaps.containsKey(sp.id)) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .size(250, 250)
                    .transformations(CircleCropTransformation())
                    .target { drawable ->
                        val bitmap = (drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            _stopPointBitmaps[sp.id] = bitmap
                        }
                    }
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    // --- 6. TÍNH NĂNG ĐỒNG BỘ (SYNC) & ĐĂNG BÀI (PUBLISH) ---
    // 🚀 1. HÀM PHỤ: Ném 1 file lên Server và lấy link online về
    @Suppress("DEPRECATION")
    private suspend fun uploadSingleFile(filePath: String, token: String): String {
        return try {
            // Đã là link web (http) rồi thì bỏ qua
            if (filePath.startsWith("http")) return filePath

            val file = java.io.File(filePath)
            if (!file.exists()) return filePath

            val mimeType = if (filePath.endsWith(".mp4", ignoreCase = true)) "video/*" else "image/*"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.uploadSingleMedia(token, body)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.url
            } else {
                filePath
            }
        } catch (e: Exception) {
            android.util.Log.e("UPLOAD_MEDIA", "Lỗi up file: ${e.message}")
            filePath
        }
    }

    // 🚀 2. HÀM CHÍNH ĐÃ NÂNG CẤP: Xóa hàm cũ đi và dán cục này vào
    fun publishJourneyToDiscovery(journey: JourneyEntity, newTitle: String, selectedThumb: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = "Bearer ${_userSession.value?.token}"

                // Lấy data từ Room DB
                val trackPoints = repository.getTrackPoints(journey.id).firstOrNull() ?: emptyList()
                val stopPoints = repository.getStopPointsWithMedia(journey.id).firstOrNull() ?: emptyList()

                // BƯỚC 1: Xử lý Avatar/Thumbnail bài đăng
                val finalThumbUrl = if (!selectedThumb.isNullOrBlank()) {
                    uploadSingleFile(selectedThumb, token)
                } else {
                    selectedThumb
                }

                // BƯỚC 2: Quét qua tất cả điểm dừng, bốc từng ảnh lên Server lấy link online
                val processedStopPoints = stopPoints.map { sp ->
                    val processedMedia = sp.mediaList.map { m ->
                        val onlineUrl = uploadSingleFile(m.fileUri, token)
                        mapOf("file_uri" to onlineUrl, "media_type" to m.mediaType)
                    }

                    mapOf(
                        "local_id" to sp.stopPoint.id,
                        "note" to sp.stopPoint.note,
                        "latitude" to sp.stopPoint.latitude,
                        "longitude" to sp.stopPoint.longitude,
                        "media" to processedMedia
                    )
                }

                // BƯỚC 3: Đóng gói JSON (Ảnh lúc này 100% là link Online)
                val payload = mapOf(
                    "journey" to mapOf(
                        "id" to journey.id, "title" to newTitle,
                        "start_lat" to journey.startLat, "start_lon" to journey.startLon,
                        "start_time" to journey.startTime
                    ),
                    "track_points" to trackPoints.map {
                        mapOf("latitude" to it.latitude, "longitude" to it.longitude, "timestamp" to it.timestamp)
                    },
                    "stop_points" to processedStopPoints
                )

                // BƯỚC 4: Gửi bài lên API publish
                val request = PublishRequest(journeyId = journey.id, title = newTitle, thumbnailUri = finalThumbUrl, payload = payload)
                val response = apiService.publishJourney(token, request)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) onResult(true, "Đã đăng bài kèm ảnh lên Cộng đồng!")
                    else onResult(false, "Lỗi đăng bài!")
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Lỗi hệ thống: ${e.message}")
                }
            }
        }
    }

    fun deleteStopPoint(stopId: Long, journeyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteStopPointById(stopId)
                repository.markJourneyAsUnsynced(journeyId)
                Log.d("MapViewModel", "✅ Đã xóa Check-in $stopId và báo Unsync cho Hành trình $journeyId")
            } catch (e: Exception) {
                Log.e("MapViewModel", "💥 Lỗi xóa Check-in: ${e.message}")
            }
        }
    }

    fun deleteAllStopPoints(journeyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAllStopPointsOfJourney(journeyId)
                repository.markJourneyAsUnsynced(journeyId)
                Log.d("MapViewModel", "✅ Đã dọn sạch Check-in của Hành trình $journeyId")
            } catch (e: Exception) {
                Log.e("MapViewModel", "💥 Lỗi xóa toàn bộ Check-in: ${e.message}")
            }
        }
    }

    // --- HELPERS ---
    private fun saveFileToInternal(context: Context, uri: Uri): String? {
        return try {
            val isVideo = context.contentResolver.getType(uri)?.contains("video") == true
            val extension = if (isVideo) ".mp4" else ".jpg"
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}$extension")

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun getTrackPoints(id: Long) = repository.getTrackPoints(id).asLiveData()
    fun getStopPoints(id: Long) = repository.getStopPointsWithMedia(id).asLiveData()
    fun getStopPointById(id: Long) = repository.getStopPointById(id).asLiveData()

    // --- 7. TÍNH NĂNG UPLOAD AVATAR ---
    @Suppress("DEPRECATION")
    fun uploadAvatar(uri: Uri, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = uriToFile(uri, context)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                val token = "Bearer ${_userSession.value?.token}"
                val response = apiService.uploadAvatar(token, body)

                if (response.isSuccessful && response.body() != null) {
                    val newUrl = response.body()?.avatarUrl
                    _userSession.value?.let { session ->
                        val updated = session.copy(avatarUrl = newUrl ?: "")
                        _userSession.value = updated
                        sessionManager.saveSession(
                            updated.id, updated.token, updated.username,
                            updated.displayName, updated.avatarUrl
                        )
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("UPLOAD_AVATAR", "Lỗi upload: ${e.message}")
                onResult(false)
            }
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val file = File(context.cacheDir, "temp_avatar.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}