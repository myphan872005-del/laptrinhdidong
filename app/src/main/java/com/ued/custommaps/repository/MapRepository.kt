package com.ued.custommaps.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ued.custommaps.models.CustomMap
import com.ued.custommaps.models.CustomMarker

class MapRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maps_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllMaps(): List<CustomMap> {
        val json = prefs.getString("maps", "[]") ?: "[]"
        val type = object : TypeToken<List<CustomMap>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveMap(map: CustomMap) {
        val maps = getAllMaps().toMutableList()
        val index = maps.indexOfFirst { it.id == map.id }
        if (index >= 0) maps[index] = map else maps.add(map)

        // Dùng commit() để ghi dữ liệu ngay, tránh lag file XML
        prefs.edit().putString("maps", gson.toJson(maps)).commit()
    }

    fun getMapById(id: String): CustomMap? = getAllMaps().find { it.id == id }

    fun deleteMap(mapId: String) {
        val maps = getAllMaps().filter { it.id != mapId }
        prefs.edit().putString("maps", gson.toJson(maps)).commit()
    }
}