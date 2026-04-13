package com.ued.custommaps

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.util.Log

@HiltAndroidApp
class GeoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Tạo Channel ngay khi App start
        createNotificationChannel()
        Log.d("GEO_APP", "🚀 Ứng dụng đã khởi tạo thành công!")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking",
                "Hành trình",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kênh hiển thị trạng thái ghi hành trình của Hoan"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}