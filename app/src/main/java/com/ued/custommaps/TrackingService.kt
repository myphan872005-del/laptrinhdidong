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
        // Sửa dòng này:
        repository = MapRepository.getInstance(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
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
            currentMapId?.let { id ->
                val map = repository.getMapById(id)
                if (map != null) repository.saveMap(map.copy(isTracking = false))
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        currentMapId = mapId
        val channelId = "tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(channelId, "GPS", NotificationManager.IMPORTANCE_LOW))
        }
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