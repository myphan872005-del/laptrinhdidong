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
    // Logic lấy dữ liệu từ API và lưu vào local DB (Cache) sẽ viết ở đây
    suspend fun getDiscoveryFeed() = apiService.getDiscoveryFeed()

    // Thêm các hàm xử lý DAO nếu cần
}