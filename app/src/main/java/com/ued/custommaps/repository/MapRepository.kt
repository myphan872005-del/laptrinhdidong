package com.ued.custommaps.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ued.custommaps.models.CustomMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapRepository private constructor(context: Context) { // Chuyển thành private constructor
    private val prefs: SharedPreferences = context.getSharedPreferences("maps_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _mapsFlow = MutableStateFlow<List<CustomMap>>(emptyList())
    val mapsFlow: StateFlow<List<CustomMap>> = _mapsFlow.asStateFlow()

    init {
        refresh()
    }

    // Cơ chế Singleton để dùng chung một Instance duy nhất toàn app
    companion object {
        @Volatile
        private var INSTANCE: MapRepository? = null

        fun getInstance(context: Context): MapRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MapRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun refresh() {
        val json = prefs.getString("maps", "[]") ?: "[]"
        val type = object : TypeToken<List<CustomMap>>() {}.type
        val list = try {
            gson.fromJson<List<CustomMap>>(json, type)
        } catch (e: Exception) {
            emptyList()
        }
        _mapsFlow.value = list.sortedByDescending { it.createdAt }.toList()
        Log.d("DEBUG_APP", "Repository (${this.hashCode()}): Refresh xong")
    }

    fun getAllMaps(): List<CustomMap> = _mapsFlow.value

    fun saveMap(map: CustomMap) {
        val maps = getAllMaps().toMutableList()
        val index = maps.indexOfFirst { it.id == map.id }
        if (index >= 0) maps[index] = map else maps.add(0, map)

        prefs.edit().putString("maps", gson.toJson(maps)).apply()
        Log.d("DEBUG_APP", "Repository (${this.hashCode()}): Đã lưu map isTracking=${map.isTracking}")
        refresh()
    }

    fun getMapById(id: String): CustomMap? = getAllMaps().find { it.id == id }

    fun deleteMap(mapId: String) {
        val maps = getAllMaps().filter { it.id != mapId }
        prefs.edit().putString("maps", gson.toJson(maps)).apply()
        refresh()
    }
}