package com.ued.custommaps.viewmodel

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

    // Lấy dữ liệu từ Room dưới dạng LiveData
    val journeys = repository.allJourneys.asLiveData()

    // Biến SearchQuery kiểu State để Compose tự động track
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _isTracking = mutableStateOf(false)
    val isTracking: State<Boolean> = _isTracking

    private val _currentSegmentId = mutableLongStateOf(1L)
    val currentSegmentId: State<Long> = _currentSegmentId

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTracking() {
        _isTracking.value = !_isTracking.value
        if (_isTracking.value) {
            _currentSegmentId.longValue = System.currentTimeMillis()
        }
    }

    fun createMap(title: String, lat: Double, lon: Double) {
        viewModelScope.launch { repository.startNewJourney(title, lat, lon) }
    }

    fun addStopPoint(journeyId: Long, lat: Double, lon: Double, note: String, image: String? = null) {
        viewModelScope.launch { repository.addStopPoint(journeyId, lat, lon, note, image) }
    }

    fun deleteMap(journey: JourneyEntity) = viewModelScope.launch { repository.deleteJourney(journey) }
    fun getTrackPoints(journeyId: Long) = repository.getTrackPoints(journeyId).asLiveData()
    fun getStopPoints(journeyId: Long) = repository.getStopPoints(journeyId).asLiveData()
}