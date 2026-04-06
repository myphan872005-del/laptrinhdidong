package com.ued.custommaps

import android.app.*
import android.content.Intent

import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ued.custommaps.models.GeoPointData
import com.ued.custommaps.repository.MapRepository

class TrackingService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: MapRepository
    private var currentMapId: String? = null

    override fun onCreate() {
        super.onCreate()

        repository = MapRepository(this)

        // Sửa dòng này:
        repository = MapRepository.getInstance(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->

                    // LỚP LỌC 1: Độ chính xác (Accuracy)
                    // Chỉ lấy nếu sai số dưới 20 mét. Nếu nhảy ra trường sai số thường là > 100m.
                    if (location.accuracy > 25f) return@let

                    // LỚP LỌC 2: Vận tốc (Speed)
                    // Nếu bạn đang đi bộ, vận tốc khoảng 0.5 - 1.5 m/s.
                    // Nếu nhảy vọt ra trường trong 1 giây, vận tốc sẽ là hàng trăm m/s -> Loại bỏ.
                    if (location.speed < 0.2f && location.accuracy > 15f) return@let

                    currentMapId?.let { id ->
                        val map = repository.getMapById(id) ?: return@let

                        // LỚP LỌC 3: Khoảng cách tối thiểu (Distance)
                        // Nếu điểm mới quá gần điểm cũ (dưới 3m), không lưu để tránh "mạng nhện" khi đứng yên.
                        if (map.polyline.isNotEmpty()) {
                            val lastPoint = map.polyline.last()
                            val distanceResults = FloatArray(1)
                            android.location.Location.distanceBetween(
                                lastPoint.latitude, lastPoint.longitude,
                                location.latitude, location.longitude,
                                distanceResults
                            )
                            if (distanceResults[0] < 3f) return@let
                        }

                        // Nếu vượt qua hết các lớp lọc thì mới lưu
                        val updatedMap = map.copy(polyline = map.polyline + GeoPointData(location.latitude, location.longitude))
                        repository.saveMap(updatedMap)
                        sendBroadcast(Intent("TRACKING_UPDATE"))

                    if (location.accuracy > 25f) return@let
                    currentMapId?.let { id ->
                        val map = repository.getMapById(id) ?: return@let
                        if (map.polyline.isNotEmpty()) {
                            val last = map.polyline.last()
                            val dist = FloatArray(1)
                            android.location.Location.distanceBetween(last.latitude, last.longitude, location.latitude, location.longitude, dist)
                            if (dist[0] < 3f) return@let
                        }
                        repository.saveMap(map.copy(polyline = map.polyline + GeoPointData(location.latitude, location.longitude)))

                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mapId = intent?.getStringExtra("MAP_ID")

        if (intent?.action == "STOP_TRACKING") {
            fusedLocationClient.removeLocationUpdates(locationCallback)


        if (intent?.action == "STOP_TRACKING") {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            currentMapId?.let { id ->
                val map = repository.getMapById(id)
                if (map != null) repository.saveMap(map.copy(isTracking = false))
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        currentMapId = mapId
        startForeground(1, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(2f)
            .setWaitForAccurateLocation(true) // ÉP BUỘC chờ có tọa độ chính xác mới trả về
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        } catch (e: SecurityException) { }
    }

    private fun createNotification(): Notification {


        currentMapId = mapId

        val channelId = "tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(channelId, "GPS", NotificationManager.IMPORTANCE_LOW))
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ghi hành trình...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(1, createNotification(channelId))
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1).let { it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL }
        val interval = if (isCharging) 3000L else 10000L

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateDistanceMeters(2f).setWaitForAccurateLocation(true).build()
        try { fusedLocationClient.requestLocationUpdates(request, locationCallback, null) } catch (e: SecurityException) {}
    }

    private fun createNotification(channelId: String): Notification {
        val stopIntent = Intent(this, TrackingService::class.java).apply { action = "STOP_TRACKING" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("UED Custom Maps").setContentText("Đang ghi hành trình...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dừng ghi", stopPendingIntent).build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}