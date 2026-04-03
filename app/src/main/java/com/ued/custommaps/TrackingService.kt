package com.ued.custommaps

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ued.custommaps.data.JourneyRepository
import com.ued.custommaps.data.TrackPointEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var repository: JourneyRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentJourneyId: Long = -1L
    private var currentSegmentId: Long = -1L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                serviceScope.launch {
                    // ĐÃ ĐỒNG BỘ: Khớp hoàn toàn với TrackPointEntity của Hoan
                    val point = TrackPointEntity(
                        id = System.currentTimeMillis(), // Khóa chính dùng timestamp
                        journeyId = currentJourneyId,
                        segmentId = currentSegmentId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        isSynced = false
                    )
                    repository.insertTrackPoint(point)
                    Log.d("TRACKING_SERVICE", "Đã lưu tọa độ vào Room: ${location.latitude}")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == "STOP") {
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Lấy thông tin từ Intent (Gửi từ ViewModel)
        currentJourneyId = intent?.getLongExtra("JOURNEY_ID", -1L) ?: -1L
        currentSegmentId = intent?.getLongExtra("SEGMENT_ID", System.currentTimeMillis()) ?: System.currentTimeMillis()

        if (currentJourneyId != -1L) {
            showNotification()
            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, "tracking")
            .setContentTitle("UED Custom Maps")
            .setContentText("Đang ghi lại hành trình của Hoan...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TRACKING_SERVICE", "Lỗi quyền: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}