package com.ued.custommaps.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ued.custommaps.models.CustomMap
import com.ued.custommaps.models.CustomMarker
import com.ued.custommaps.models.GeoPointData
import com.ued.custommaps.models.MapStyle
import com.ued.custommaps.repository.MapRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(private val repository: MapRepository) : ViewModel() {

    // Lấy Flow trực tiếp từ Repository
    val maps: StateFlow<List<CustomMap>> = repository.mapsFlow

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _mapStyle = mutableStateOf(MapStyle.NORMAL)
    val mapStyle: State<MapStyle> = _mapStyle

    init {
        Log.d("DEBUG_APP", "ViewModel: Khởi tạo thành công")
    }

    fun loadMaps() {
        Log.d("DEBUG_APP", "ViewModel: Yêu cầu Repository làm mới dữ liệu")
        repository.refresh()
    }

    fun createMap(title: String) {
        repository.saveMap(CustomMap(title = title.trim()))
    }

    fun deleteMap(mapId: String) {
        repository.deleteMap(mapId)
    }

    fun updateTrackingStatus(mapId: String, isTracking: Boolean) {
        Log.d("DEBUG_APP", "ViewModel: Cập nhật tracking status: $isTracking")
        val currentMap = repository.getMapById(mapId) ?: return
        repository.saveMap(currentMap.copy(isTracking = isTracking))
    }

    fun addMarkerToMap(mapId: String, marker: CustomMarker) {
        val currentMap = repository.getMapById(mapId) ?: return
        repository.saveMap(currentMap.copy(markers = currentMap.markers + marker))
    }

    fun updatePolyline(mapId: String, points: List<GeoPointData>) {
        val currentMap = repository.getMapById(mapId) ?: return
        repository.saveMap(currentMap.copy(polyline = points))
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun getFilteredMaps(): List<CustomMap> {
        val currentMaps = maps.value
        return if (_searchQuery.value.isEmpty()) currentMaps
        else currentMaps.filter { it.title.contains(_searchQuery.value, true) }
    }

    fun changeMapStyle(style: MapStyle) { _mapStyle.value = style }
}