package com.ued.custommaps.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.*
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
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
import java.io.FileOutputStream
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

    init {
        viewModelScope.launch {
            sessionManager.userSession.collect { session ->
                _userSession.value = session
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val journeys: LiveData<List<JourneyEntity>> = userSession.flatMapLatest { session ->
        if (session != null) {
            repository.getAllJourneys(session.id)
        } else {
            flowOf(emptyList())
        }
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

    private val zoomedJourneys = mutableStateMapOf<Long, Boolean>()
    fun isZoomed(id: Long) = zoomedJourneys[id] ?: false
    fun markAsZoomed(id: Long) { zoomedJourneys[id] = true }

    // SỬA LỖI & ĐỒNG BỘ SERVICE: Chạy ngầm chuẩn chuyên nghiệp
    fun toggleTracking(context: Context, journeyId: Long) {
        val intent = Intent(context, TrackingService::class.java)
        if (!isTracking.value) {
            currentSegmentId.longValue = System.currentTimeMillis()
            intent.action = "START"
            intent.putExtra("JOURNEY_ID", journeyId)
            intent.putExtra("SEGMENT_ID", currentSegmentId.longValue)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
            isTracking.value = true
        } else {
            intent.action = "STOP"
            context.stopService(intent)
            isTracking.value = false
        }
    }

    val focusLocation = mutableStateOf<Pair<Double, Double>?>(null)

    // --- 3. LOGIC HÀNH TRÌNH ---
    fun updateJourneyStartLocation(journey: JourneyEntity, lat: Double, lon: Double) {
        viewModelScope.launch {
            repository.insertJourney(journey.copy(startLat = lat, startLon = lon, isSynced = false))
        }
    }

    fun createMap(title: String) {
        val currentUserId = userSession.value?.id ?: return
        viewModelScope.launch {
            val newJourney = JourneyEntity(
                id = System.currentTimeMillis(),
                userId = currentUserId,
                title = title,
                startTime = System.currentTimeMillis(),
                isSynced = false
            )
            repository.insertJourney(newJourney)
        }
    }

    fun deleteMap(journey: JourneyEntity) {
        viewModelScope.launch { repository.softDeleteJourney(journey.id) }
    }

    // --- 4. LOGIC ĐIỂM DỪNG & MEDIA ---
    fun deleteSelectedStopPoints(ids: List<Long>, allData: List<StopPointWithMedia>) {
        viewModelScope.launch {
            // 1. Thực hiện xóa trong Repository (Room)
            repository.softDeleteStopPointsBatch(ids)

            // 2. Xóa cache bitmap để giải phóng RAM
            ids.forEach { stopPointBitmaps.remove(it) }

            // 3. Đánh dấu Map này cần đồng bộ lại với Server (isSynced = false)
            val journeyId = allData.firstOrNull { it.stopPoint.id in ids }?.stopPoint?.journeyId
            if (journeyId != null) {
                repository.markJourneyAsUnsynced(journeyId)
            }
            Log.d("MAP_VM", "Đã xóa ${ids.size} điểm dừng cục bộ.")
        }
    }
    fun addStopPointWithMedia(context: Context, journeyId: Long, lat: Double, lon: Double, note: String, uris: List<Uri>) {
        viewModelScope.launch {
            val stopPointId = System.currentTimeMillis()
            var firstSavedPath: String? = null

            val newStopPoint = StopPointEntity(
                id = stopPointId,
                journeyId = journeyId,
                latitude = lat,
                longitude = lon,
                note = note
            )
            repository.insertStopPoint(newStopPoint)

            uris.forEachIndexed { index, uri ->
                saveFileToInternal(context, uri)?.let { path ->
                    if (index == 0) firstSavedPath = path
                    val type = if (context.contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"

                    repository.insertMedia(
                        StopPointMediaEntity(
                            id = System.currentTimeMillis() + index,
                            parentStopId = stopPointId,
                            fileUri = path,
                            mediaType = type
                        )
                    )
                }
            }

            if (firstSavedPath != null) {
                repository.updateStopPointThumbnail(stopPointId, firstSavedPath)
            }
            repository.markJourneyAsUnsynced(journeyId)
        }
    }

    // --- THÊM VÀO MapViewModel.kt ---

    fun addMoreMediaToStop(context: Context, stopId: Long, uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                saveFileToInternal(context, uri)?.let { path ->
                    val type = if (context.contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"
                    repository.insertMedia(
                        StopPointMediaEntity(
                            parentStopId = stopId,
                            fileUri = path,
                            mediaType = type
                        )
                    )
                }
            }
            // Xóa cache bitmap để UI load lại ảnh mới
            stopPointBitmaps.remove(stopId)

            // Đánh dấu cần đồng bộ lại
            repository.getStopPointById(stopId).firstOrNull()?.let {
                repository.markJourneyAsUnsynced(it.stopPoint.journeyId)
            }
        }
    }

    fun updateStopPointNote(id: Long, note: String) = viewModelScope.launch {
        repository.updateStopPointNote(id, note)
        repository.getStopPointById(id).firstOrNull()?.let {
            repository.markJourneyAsUnsynced(it.stopPoint.journeyId)
        }
    }

    fun updateStopPointThumbnail(stopId: Long, uri: String?) = viewModelScope.launch {
        repository.updateStopPointThumbnail(stopId, uri)
        stopPointBitmaps.remove(stopId) // Ép UI load lại icon marker
    }

    fun deleteMedia(media: StopPointMediaEntity) = viewModelScope.launch {
        try { File(media.fileUri).delete() } catch (e: Exception) {}
        repository.deleteSingleMedia(media)
        stopPointBitmaps.remove(media.parentStopId)
    }

    // --- 5. LOGIC LOAD THUMBNAIL TÙY CHỈNH ---
    val stopPointBitmaps = mutableStateMapOf<Long, Bitmap?>()

    fun loadBitmapsForStopPoints(context: Context, stopPoints: List<StopPointWithMedia>) {
        viewModelScope.launch {
            val imageLoader = ImageLoader(context)
            stopPoints.forEach { item ->
                val stopId = item.stopPoint.id
                if (!stopPointBitmaps.containsKey(stopId)) {
                    val uriToLoad: Any? = when {
                        item.stopPoint.thumbnailUri != null -> item.stopPoint.thumbnailUri
                        item.mediaList.any { it.mediaType == "IMAGE" } -> item.mediaList.find { it.mediaType == "IMAGE" }?.fileUri
                        item.mediaList.any { it.mediaType == "VIDEO" } -> android.R.drawable.ic_menu_slideshow
                        else -> android.R.drawable.ic_menu_mylocation
                    }

                    if (uriToLoad != null) {
                        try {
                            val request = ImageRequest.Builder(context).data(uriToLoad).size(150, 150).build()
                            val result = (imageLoader.execute(request) as SuccessResult).drawable
                            stopPointBitmaps[stopId] = (result as BitmapDrawable).bitmap
                        } catch (e: Exception) { stopPointBitmaps[stopId] = null }
                    } else { stopPointBitmaps[stopId] = null }
                }
            }
        }
    }

    // --- 6. TÍNH NĂNG ĐỒNG BỘ (SYNC) ---
    fun syncJourneyToServer(journey: JourneyEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${_userSession.value?.token}"
                val trackPointsList = repository.getTrackPoints(journey.id).firstOrNull() ?: emptyList()
                val stopPointsList = repository.getStopPointsWithMedia(journey.id).firstOrNull() ?: emptyList()

                val baseUrl = "http://192.168.1.25:3000" // Đảm bảo IP này đúng

                val syncStopPoints = stopPointsList.map { spWithMedia ->
                    val sp = spWithMedia.stopPoint

                    // Upload Media trước
                    val mediaParts = spWithMedia.mediaList.map { media ->
                        val file = File(media.fileUri)
                        val requestFile = file.asRequestBody(
                            if (media.mediaType == "VIDEO") "video/*".toMediaTypeOrNull()
                            else "image/*".toMediaTypeOrNull()
                        )
                        MultipartBody.Part.createFormData("files", file.name, requestFile)
                    }

                    val serverUrls = mutableListOf<String>()
                    if (mediaParts.isNotEmpty()) {
                        try {
                            val uploadRes = apiService.uploadMultipleMedia(token, mediaParts)
                            if (uploadRes.isSuccessful) {
                                serverUrls.addAll(uploadRes.body()?.urls ?: emptyList())
                            }
                        } catch (e: Exception) { Log.e("SYNC", "Upload media error: ${e.message}") }
                    }

                    SyncStopPoint(
                        local_id = sp.id,
                        latitude = sp.latitude,
                        longitude = sp.longitude,
                        note = sp.note,
                        thumbnail_uri = if (serverUrls.isNotEmpty()) "$baseUrl${serverUrls[0]}" else null,
                        timestamp = sp.id,
                        is_deleted = if (sp.isDeleted) 1 else 0,
                        mediaList = serverUrls.mapIndexed { index, url ->
                            SyncMedia(
                                local_id = System.currentTimeMillis() + index,
                                file_uri = "$baseUrl$url",
                                media_type = if (url.lowercase().contains(".mp4")) "VIDEO" else "IMAGE"
                            )
                        }
                    )
                }

                val request = SyncJourneyRequest(
                    id = journey.id,
                    title = journey.title,
                    startTime = journey.startTime,
                    startLat = if (journey.startLat != 0.0) journey.startLat else (trackPointsList.firstOrNull()?.latitude ?: 16.0),
                    startLon = if (journey.startLon != 0.0) journey.startLon else (trackPointsList.firstOrNull()?.longitude ?: 108.0),
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = if (journey.isDeleted) 1 else 0,
                    isPublic = if (journey.isPublic == 1) 1 else 1, // Mặc định 1 để hiện lên Discovery
                    trackPoints = trackPointsList.map { SyncTrackPoint(it.segmentId, it.latitude, it.longitude, it.timestamp) },
                    stopPoints = syncStopPoints
                )

                val response = apiService.syncJourney(token, request)
                if (response.isSuccessful) {
                    repository.insertJourney(journey.copy(isSynced = true))
                    onResult(true, "Chia sẻ thành công!")
                } else {
                    onResult(false, "Lỗi Server: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                onResult(false, "Lỗi hệ thống: ${e.message}")
            }
        }
    }

    // --- HELPERS ---
    private fun saveFileToInternal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    fun uploadAvatar(uri: Uri, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Chuyển Uri thành File để upload
                val file = uriToFile(uri, context)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                val token = "Bearer ${_userSession.value?.token}"
                val response = apiService.uploadAvatar(token, body)

                if (response.isSuccessful && response.body() != null) {
                    val newAvatarUrl = response.body()?.avatarUrl
                    val currentSession = _userSession.value

                    if (currentSession != null && newAvatarUrl != null) {
                        // Cập nhật StateFlow để UI đổi ảnh ngay lập tức
                        val newSession = currentSession.copy(avatarUrl = newAvatarUrl)
                        _userSession.value = newSession

                        // Lưu vào Datastore/SharedPrefs để lần sau mở app vẫn còn
                        sessionManager.saveSession(
                            id = currentSession.id,
                            token = currentSession.token,
                            username = currentSession.username,
                            displayName = currentSession.displayName,
                            avatarUrl = newAvatarUrl
                        )
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                android.util.Log.e("AVATAR_UPLOAD", "Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // Trong MapViewModel.kt

    fun publishJourneyToDiscovery(
        journey: JourneyEntity,
        newTitle: String,
        selectedThumb: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${_userSession.value?.token}"

                // 1. Lấy dữ liệu tươi nhất từ Room
                val trackPoints = repository.getTrackPoints(journey.id).firstOrNull() ?: emptyList()
                val stopPoints = repository.getStopPointsWithMedia(journey.id).firstOrNull() ?: emptyList()

                // 2. Đóng gói Payload (Snapshot)
                val payload = mapOf(
                    "journey" to mapOf(
                        "id" to journey.id,
                        "title" to newTitle,
                        "start_lat" to journey.startLat,
                        "start_lon" to journey.startLon,
                        "start_time" to journey.startTime
                    ),
                    "track_points" to trackPoints.map {
                        mapOf("latitude" to it.latitude, "longitude" to it.longitude, "timestamp" to it.timestamp)
                    },
                    "stop_points" to stopPoints.map { sp ->
                        mapOf(
                            "local_id" to sp.stopPoint.id,
                            "note" to sp.stopPoint.note,
                            "latitude" to sp.stopPoint.latitude,
                            "longitude" to sp.stopPoint.longitude,
                            "media" to sp.mediaList.map { m ->
                                mapOf("file_uri" to m.fileUri, "media_type" to m.mediaType)
                            }
                        )
                    }
                )

                // 3. Gửi yêu cầu Publish
                val request = PublishRequest(
                    journeyId = journey.id,
                    title = newTitle,
                    thumbnailUri = selectedThumb,
                    payload = payload
                )

                val response = apiService.publishJourney(token, request)
                if (response.isSuccessful) {
                    onResult(true, response.body()?.message ?: "Thành công")
                } else {
                    onResult(false, "Lỗi: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult(false, "Lỗi hệ thống: ${e.message}")
            }
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val file = File(context.cacheDir, "temp_avatar.jpg")
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    fun getTrackPoints(id: Long) = repository.getTrackPoints(id).asLiveData()
    fun getStopPoints(id: Long) = repository.getStopPointsWithMedia(id).asLiveData()
    // Thêm vào MapViewModel.kt
    fun getStopPointById(id: Long): LiveData<StopPointWithMedia?> {
        return repository.getStopPointById(id).asLiveData()
    }
}