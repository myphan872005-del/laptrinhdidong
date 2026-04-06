package com.ued.custommaps.viewmodel

<<<<<<< HEAD
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ued.custommaps.models.CustomMap
import com.ued.custommaps.models.CustomMarker
import com.ued.custommaps.models.MapStyle
import com.ued.custommaps.repository.MapRepository
import com.ued.custommaps.models.GeoPointData

class MapViewModel(private val repository: MapRepository) : ViewModel() {

    // Quản lý danh sách bản đồ
    private val _maps = mutableStateOf<List<CustomMap>>(emptyList())
    val maps: State<List<CustomMap>> = _maps

    // Quản lý tìm kiếm
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    // Quản lý kiểu bản đồ (Normal, Satellite, Terrain)
=======
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

        loadMaps()
    }

    /**
     * Tải lại toàn bộ danh sách bản đồ từ SharedPreferences
     */
    fun loadMaps() {
        _maps.value = repository.getAllMaps().sortedByDescending { it.createdAt }
    }

    /**
     * Tạo bản đồ mới
     */
    fun createMap(title: String) {
        if (title.isBlank()) return
        val newMap = CustomMap(title = title.trim())
        repository.saveMap(newMap)
        loadMaps()
    }

    /**
     * Xóa bản đồ
     */
    fun deleteMap(mapId: String) {
        repository.deleteMap(mapId)
        loadMaps()
    }

    /**
     * Thêm một điểm đánh dấu (Marker) vào bản đồ
     */
    fun addMarkerToMap(mapId: String, marker: CustomMarker) {
        val currentMap = getMapById(mapId) ?: return
        val updatedMap = currentMap.copy(markers = currentMap.markers + marker)
        repository.saveMap(updatedMap)
        loadMaps()
    }

    /**
     * Lấy thông tin chi tiết của một bản đồ theo ID
     */
    fun getMapById(id: String): CustomMap? {
        return repository.getMapById(id)
    }

    /**
     * Cập nhật từ khóa tìm kiếm
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Trả về danh sách bản đồ sau khi lọc theo từ khóa
     */
    fun getFilteredMaps(): List<CustomMap> {
        return if (_searchQuery.value.isEmpty()) {
            _maps.value
        } else {
            _maps.value.filter { map ->
                map.title.contains(_searchQuery.value, ignoreCase = true)
            }
        }
    }

    /**
     * Thay đổi kiểu hiển thị của bản đồ
     */
    fun changeMapStyle(style: MapStyle) {
        _mapStyle.value = style
    }

    /**
     * Cập nhật trạng thái Đang ghi/Dừng ghi hành trình
     * Quan trọng: Phải đọc dữ liệu mới nhất từ Repo để tránh mất tọa độ polyline
     */
    fun updateTrackingStatus(mapId: String, isTracking: Boolean) {
        val currentMap = repository.getMapById(mapId) ?: return
        val updatedMap = currentMap.copy(isTracking = isTracking)
        repository.saveMap(updatedMap)
        loadMaps()
    }

    /**
     * Cập nhật danh sách tọa độ (Polyline) - Dùng để xóa lộ trình hoặc cập nhật thủ công
     */
    fun updatePolyline(mapId: String, points: List<GeoPointData>) {
        val currentMap = repository.getMapById(mapId) ?: return
        val updatedMap = currentMap.copy(polyline = points)
        repository.saveMap(updatedMap)
        loadMaps()
    }

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