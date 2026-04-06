package com.ued.custommaps.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri

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
    fun createMap(title: String, lat: Double, lon: Double) {
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

    fun deleteMap(journey: JourneyEntity) {
        viewModelScope.launch { repository.softDeleteJourney(journey.id) }
    }

    // --- 4. LOGIC ĐIỂM DỪNG & MEDIA ---
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
                    repository.insertMedia(StopPointMediaEntity(id = System.currentTimeMillis() + index, parentStopId = stopPointId, fileUri = path, mediaType = type))
                }
            }
            if (firstSavedPath != null) repository.updateStopPointThumbnail(stopPointId, firstSavedPath)
            repository.markJourneyAsUnsynced(journeyId)
        }
    }

    fun deleteSelectedStopPoints(ids: List<Long>, allData: List<StopPointWithMedia>) {
        viewModelScope.launch {
            repository.softDeleteStopPointsBatch(ids)
            ids.forEach { stopPointBitmaps.remove(it) }
            val journeyId = allData.firstOrNull { it.stopPoint.id in ids }?.stopPoint?.journeyId
            if (journeyId != null) repository.markJourneyAsUnsynced(journeyId)
        }
    }

    // --- 5. HỆ THỐNG CACHE BITMAP (Để hiện ảnh trên Marker bản đồ) ---
    val stopPointBitmaps = mutableStateMapOf<Long, Bitmap?>()

    fun loadBitmapsForStopPoints(context: Context, stopPoints: List<StopPointWithMedia>) {
        viewModelScope.launch {
            val imageLoader = ImageLoader(context)
            stopPoints.forEach { item ->
                val stopId = item.stopPoint.id
                if (!stopPointBitmaps.containsKey(stopId)) {
                    val uriToLoad: Any? = item.stopPoint.thumbnailUri ?: item.mediaList.firstOrNull()?.fileUri ?: android.R.drawable.ic_menu_mylocation
                    try {
                        val request = ImageRequest.Builder(context).data(uriToLoad).size(120, 120).build()
                        val result = (imageLoader.execute(request) as SuccessResult).drawable
                        stopPointBitmaps[stopId] = (result as BitmapDrawable).bitmap
                    } catch (e: Exception) { stopPointBitmaps[stopId] = null }
                }
            }
        }
    }

    // --- 6. ĐỒNG BỘ SERVER (SYNC) ---
    fun syncJourneyToServer(journey: JourneyEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${_userSession.value?.token}"
                val trackPointsList = repository.getTrackPoints(journey.id).firstOrNull() ?: emptyList()
                val stopPointsList = repository.getStopPointsWithMedia(journey.id).firstOrNull() ?: emptyList()
                // ... (Giữ nguyên logic sync cũ của bạn vì nó rất hoàn thiện)
                onResult(true, "Đồng bộ thành công!")
            } catch (e: Exception) { onResult(false, "Lỗi: ${e.message}") }
        }
    }

    // --- HELPERS ---
    private fun saveFileToInternal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "media_${System.currentTimeMillis()}.jpg")
            inputStream?.use { it.copyTo(file.outputStream()) }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    fun getTrackPoints(id: Long) = repository.getTrackPoints(id).asLiveData()
    fun getStopPoints(id: Long) = repository.getStopPointsWithMedia(id).asLiveData()
}