package com.ued.custommaps.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.*
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ued.custommaps.data.*
import com.ued.custommaps.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // --- 1. QUẢN LÝ PHIÊN ĐĂNG NHẬP & PHÂN QUYỀN ---
    // ĐÃ FIX: Trả lại đúng kiểu UserSession (không dùng AuthResponse nữa)
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
            // ĐÃ FIX: Bây giờ session.id sẽ không còn báo đỏ nữa!
            repository.getAllJourneys(session.id)
        } else {
            flowOf(emptyList())
        }
    }.asLiveData()

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }

    // --- 2. LOGIC TÌM KIẾM & BẢN ĐỒ ---
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    var isTracking = mutableStateOf(false)
    var currentSegmentId = mutableLongStateOf(System.currentTimeMillis())

    private val zoomedJourneys = mutableStateMapOf<Long, Boolean>()
    fun isZoomed(id: Long) = zoomedJourneys[id] ?: false
    fun markAsZoomed(id: Long) { zoomedJourneys[id] = true }

    fun toggleTracking() {
        isTracking.value = !isTracking.value
        if (isTracking.value) currentSegmentId.longValue = System.currentTimeMillis()
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
                startTime = System.currentTimeMillis()
            )
            repository.insertJourney(newJourney)
        }
    }

    fun deleteMap(journey: JourneyEntity) {
        viewModelScope.launch { repository.softDeleteJourney(journey.id) }
    }

    // --- 4. LOGIC ĐIỂM DỪNG & MEDIA ---
    fun addStopPointWithMedia(context: Context, journeyId: Long, lat: Double, lon: Double, note: String, uris: List<Uri>) {
        viewModelScope.launch {
            val stopPointId = System.currentTimeMillis()
            val newStopPoint = StopPointEntity(
                id = stopPointId,
                journeyId = journeyId,
                latitude = lat,
                longitude = lon,
                note = note
            )
            repository.insertStopPoint(newStopPoint)

            uris.forEach { uri ->
                saveFileToInternal(context, uri)?.let { path ->
                    val type = if (context.contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"
                    repository.insertMedia(
                        StopPointMediaEntity(
                            id = System.currentTimeMillis() + (0..1000).random(),
                            parentStopId = stopPointId,
                            fileUri = path,
                            mediaType = type
                        )
                    )
                }
            }
        }
    }

    // --- UPLOAD AVATAR ---
    fun uploadAvatar(uri: Uri, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = uriToFile(uri, context)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                val token = "Bearer ${_userSession.value?.token}"
                val response = apiService.uploadAvatar(token, body)

                if (response.isSuccessful && response.body() != null) {
                    val newAvatarUrl = response.body()?.avatarUrl

                    val currentSession = _userSession.value
                    if (currentSession != null && newAvatarUrl != null) {
                        // Giả định UserSession của Hoan có thuộc tính avatarUrl
                        val newSession = currentSession.copy(avatarUrl = newAvatarUrl)
                        _userSession.value = newSession
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
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

    fun deleteSelectedStopPoints(ids: List<Long>, allData: List<StopPointWithMedia>) {
        viewModelScope.launch {
            repository.softDeleteStopPointsBatch(ids)
            ids.forEach { stopPointBitmaps.remove(it) }
        }
    }

    fun addMoreMediaToStop(context: Context, stopId: Long, uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                saveFileToInternal(context, uri)?.let { path ->
                    val type = if (context.contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"
                    repository.insertMedia(StopPointMediaEntity(parentStopId = stopId, fileUri = path, mediaType = type))
                }
            }
            stopPointBitmaps.remove(stopId)
        }
    }

    fun deleteMedia(media: StopPointMediaEntity) = viewModelScope.launch {
        try { File(media.fileUri).delete() } catch (e: Exception) {}
        repository.deleteSingleMedia(media)
        stopPointBitmaps.remove(media.parentStopId)

        val stopData = repository.getStopPointById(media.parentStopId).firstOrNull()
        if (stopData?.stopPoint?.thumbnailUri == media.fileUri) {
            repository.updateStopPointThumbnail(media.parentStopId, null)
        }
    }

    private fun saveFileToInternal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    // --- 5. OBSERVERS CHO CHI TIẾT HÀNH TRÌNH ---
    fun getTrackPoints(id: Long) = repository.getTrackPoints(id).asLiveData()
    fun getStopPoints(id: Long) = repository.getStopPointsWithMedia(id).asLiveData()
    fun getStopPointById(id: Long) = repository.getStopPointById(id).asLiveData()

    fun updateStopPointNote(id: Long, note: String) = viewModelScope.launch {
        repository.updateStopPointNote(id, note)
    }

    // --- 6. LOGIC LOAD THUMBNAIL TÙY CHỈNH ---
    val stopPointBitmaps = mutableStateMapOf<Long, Bitmap?>()

    fun updateStopPointThumbnail(stopId: Long, uri: String?) = viewModelScope.launch {
        repository.updateStopPointThumbnail(stopId, uri)
        stopPointBitmaps.remove(stopId)
    }

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
}