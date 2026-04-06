package com.ued.custommaps

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
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
    private var pointsCount = 0

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "tracking_channel"

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                if (location.accuracy > 25f) return@forEach

                serviceScope.launch {
                    val point = TrackPointEntity(
                        id = System.currentTimeMillis() * 1000 + (System.nanoTime() % 1000),
                        journeyId = currentJourneyId,
                        segmentId = currentSegmentId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertTrackPoint(point)
                    pointsCount++
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 🚀 KHỞI TẠO BỘ NHỚ TẠM (BÙA HỒI SINH)
        val prefs = getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)

        // 🛑 XỬ LÝ LỆNH DỪNG
        if (intent?.action == "STOP") {
            prefs.edit().clear().apply() // Xóa bùa khi người dùng chủ động dừng
            stopLocationUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // 🚀 XỬ LÝ ID KHI CHẠY / HOẶC BỊ HỆ THỐNG GỌI DẬY (intent == null)
        val passedJourneyId = intent?.getLongExtra("JOURNEY_ID", -1L) ?: -1L
        val passedSegmentId = intent?.getLongExtra("SEGMENT_ID", -1L) ?: -1L

        if (passedJourneyId != -1L) {
            // NẾU NGƯỜI DÙNG BẤM "CHẠY" TỪ APP
            currentJourneyId = passedJourneyId
            currentSegmentId = passedSegmentId
            // Lưu vào bùa hồi sinh
            prefs.edit()
                .putLong("SAVED_JOURNEY_ID", currentJourneyId)
                .putLong("SAVED_SEGMENT_ID", currentSegmentId)
                .apply()
        } else {
            // NẾU HỆ THỐNG TỰ GỌI DẬY (SAU KHI VUỐT APP)
            currentJourneyId = prefs.getLong("SAVED_JOURNEY_ID", -1L)
            currentSegmentId = prefs.getLong("SAVED_SEGMENT_ID", -1L)
            Log.d("TrackingService", "🧟 Service hồi sinh! Tiếp tục Journey ID: $currentJourneyId")
        }

        // 🚀 BẬT THÔNG BÁO VÀ GPS
        if (currentJourneyId != -1L) {
            startForegroundServiceWithNotification()
            startLocationUpdates()
        } else {
            // Không có ID nào cả thì cho chết luôn
            stopSelf()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ghi lại hành trình",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo khi ứng dụng đang ghi lại tọa độ GPS"
                enableLights(true)
                lightColor = Color.BLUE
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification("Đang ghi GPS (Bất tử mode)...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notification = buildNotification("Đã lưu $pointsCount điểm tọa độ.")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Custom Maps - Đang ghi hình")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .apply {
                setMinUpdateDistanceMeters(2f)
                setMinUpdateIntervalMillis(2000)
                setWaitForAccurateLocation(true)
            }.build()

        try {
            val looper = Looper.myLooper() ?: Looper.getMainLooper()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, looper)
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Lỗi GPS: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TrackingService", "Bị vuốt app! Đang chờ hệ thống gọi sống dậy...")
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}