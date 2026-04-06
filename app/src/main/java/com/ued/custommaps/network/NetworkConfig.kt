package com.ued.custommaps.network

object NetworkConfig {
    const val BASE_URL = "https://cytogenetic-amuck-adelaide.ngrok-free.dev/"

    fun getFullImageUrl(url: String?, baseUrl: String): String {
        if (url.isNullOrBlank()) return ""

        val trimmedUrl = url.trim()

        // 1. Nếu là link mạng xịn hoặc đã có file:// thì giữ nguyên
        if (trimmedUrl.startsWith("http") || trimmedUrl.startsWith("file://") || trimmedUrl.startsWith("content://")) {
            return trimmedUrl
        }

        // 2. 🚀 ĐẶC TRỊ LỖI CỦA SẾP: Nếu chứa /data/user/0/ hoặc tên gói app
        if (trimmedUrl.contains("data/user/0/") || trimmedUrl.contains("com.ued.custommaps")) {
            // Chuẩn hóa: /data/... -> file:///data/... (Coil cần 3 dấu gạch chéo cho file)
            val absolutePath = if (trimmedUrl.startsWith("/")) trimmedUrl else "/$trimmedUrl"
            return "file://$absolutePath"
        }

        // 3. Nếu là link Server (uploads/media/...) thì mới ghép Ngrok
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val cleanUrl = if (trimmedUrl.startsWith("/")) trimmedUrl.drop(1) else trimmedUrl
        return "$cleanBaseUrl/$cleanUrl"
    }
}