package com.ued.custommaps.repository

import com.ued.custommaps.data.DiscoveryDao
import com.ued.custommaps.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryRepository @Inject constructor(
    private val apiService: ApiService,
    private val discoveryDao: DiscoveryDao
) {

    // Thêm các hàm xử lý DAO nếu cần
    suspend fun getDiscoveryFeed(token: String) = apiService.getDiscoveryFeed(token)
}