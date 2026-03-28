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
import com.ued.custommaps.repository.JourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(private val repository: JourneyRepository) : ViewModel() {

    val journeys = repository.allJourneys.asLiveData()
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

    fun updateJourneyStartLocation(journey: JourneyEntity, lat: Double, lon: Double) {
        viewModelScope.launch { repository.updateJourney(journey.copy(startLat = lat, startLon = lon)) }
    }

    fun createMap(title: String) {
        viewModelScope.launch { repository.startNewJourney(title, 0.0, 0.0) }
    }

    fun deleteMap(journey: JourneyEntity) = viewModelScope.launch { repository.deleteJourney(journey) }

    fun addStopPointWithMedia(context: Context, journeyId: Long, lat: Double, lon: Double, note: String, uris: List<Uri>) {
        viewModelScope.launch {
            val paths = uris.mapNotNull { saveFileToInternal(context, it) }
            repository.addStopPointWithMedia(journeyId, lat, lon, note, paths)
        }
    }

    fun deleteSelectedStopPoints(ids: List<Long>, allData: List<StopPointWithMedia>) {
        viewModelScope.launch {
            repository.deleteStopPointsBatch(ids)
            ids.forEach { stopPointBitmaps.remove(it) } // Clear cache
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
            stopPointBitmaps.remove(stopId) // Clear cache ép load lại Priority
        }
    }

    fun deleteMedia(media: StopPointMediaEntity) = viewModelScope.launch {
        try { File(media.fileUri).delete() } catch (e: Exception) {}
        repository.deleteSingleMedia(media)

        // Clear cache để tính toán lại Thumbnail
        stopPointBitmaps.remove(media.parentStopId)

        // Nếu cái ảnh bị xóa đang là thumbnail tùy chỉnh, reset nó về null
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

    fun getTrackPoints(id: Long) = repository.getTrackPoints(id).asLiveData()
    fun getStopPoints(id: Long) = repository.getStopPoints(id).asLiveData()
    fun getStopPointById(id: Long) = repository.getStopPointById(id).asLiveData()
    fun updateStopPointNote(id: Long, note: String) = viewModelScope.launch { repository.updateStopPointNote(id, note) }

    // --- THUMBNAIL LOGIC ---
    val stopPointBitmaps = mutableStateMapOf<Long, Bitmap?>()

    fun updateStopPointThumbnail(stopId: Long, uri: String?) = viewModelScope.launch {
        repository.updateStopPointThumbnail(stopId, uri)
        stopPointBitmaps.remove(stopId) // Ép load lại ảnh mới trên Map
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

    val focusLocation = mutableStateOf<Pair<Double, Double>?>(null)
}