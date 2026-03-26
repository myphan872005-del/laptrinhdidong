package com.ued.custommaps.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.ued.custommaps.data.JourneyEntity
import com.ued.custommaps.repository.JourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(private val repository: JourneyRepository) : ViewModel() {

    val journeys = repository.allJourneys.asLiveData()
    val searchQuery = mutableStateOf("")

    private val _isTracking = mutableStateOf(false)
    val isTracking: State<Boolean> = _isTracking

    private val _currentSegmentId = mutableLongStateOf(1L)
    val currentSegmentId: State<Long> = _currentSegmentId

    fun toggleTracking() {
        _isTracking.value = !_isTracking.value
        if (_isTracking.value) {
            _currentSegmentId.longValue = System.currentTimeMillis() // Dùng timestamp làm segmentId duy nhất
        }
    }

    fun createMap(title: String, lat: Double, lon: Double) {
        viewModelScope.launch { repository.startNewJourney(title, lat, lon) }
    }

    // Cập nhật hàm thêm điểm dừng để nhận Uri ảnh
    fun addStopPoint(journeyId: Long, lat: Double, lon: Double, note: String, imageUri: Uri?) {
        viewModelScope.launch {
            repository.addStopPoint(journeyId, lat, lon, note, imageUri?.toString())
        }
    }

    fun deleteMap(journey: JourneyEntity) = viewModelScope.launch { repository.deleteJourney(journey) }
    fun getTrackPoints(journeyId: Long) = repository.getTrackPoints(journeyId).asLiveData()
    fun getStopPoints(journeyId: Long) = repository.getStopPoints(journeyId).asLiveData()
    fun updateSearchQuery(q: String) { searchQuery.value = q }
    fun getFilteredMaps() = journeys.value?.filter { it.title.contains(searchQuery.value, true) } ?: emptyList()
}