package com.ued.custommaps

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
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
    private var lastSavedLocation: Location? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return

            // LOGIC LỌC NHIỄU:
            // 1. Chỉ nhận tọa độ có độ chính xác dưới 25m
            if (location.accuracy > 25f) return

            // 2. Chỉ lưu nếu di chuyển ít nhất 3m so với điểm cũ
            val distance = lastSavedLocation?.distanceTo(location) ?: Float.MAX_VALUE
            if (distance < 3f) return

            lastSavedLocation = location

            serviceScope.launch {
                val point = TrackPointEntity(
                    id = System.currentTimeMillis(),
                    journeyId = currentJourneyId,
                    segmentId = currentSegmentId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    isSynced = false
                )
                repository.insertTrackPoint(point)
                Log.d("TRACKING_SERVICE", "Đã lưu tọa độ: ${location.latitude}, ${location.longitude}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        // Xử lý dừng Service
        if (action == "STOP") {
            stopLocationUpdates()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        // Lấy thông tin hành trình từ Intent
        currentJourneyId = intent?.getLongExtra("JOURNEY_ID", -1L) ?: -1L
        currentSegmentId = intent?.getLongExtra("SEGMENT_ID", System.currentTimeMillis()) ?: System.currentTimeMillis()

        if (currentJourneyId != -1L) {
            startForegroundService()
            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "tracking_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ghi lại hành trình",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("UED Custom Maps")
            .setContentText("Đang ghi lại hành trình của bạn...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // Hỗ trợ Android 14+ (Yêu cầu foregroundServiceType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(2f) // Yêu cầu GPS báo cáo mỗi khi di chuyển 2m
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TRACKING_SERVICE", "Lỗi quyền truy cập vị trí: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}