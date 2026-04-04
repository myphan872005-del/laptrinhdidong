package com.ued.custommaps

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ued.custommaps.repository.JourneyRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {
    @Inject lateinit var repository: JourneyRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJourneyId = -1L
    private var currentSegmentId = 1L // Đổi thành Long
    private var lastSavedLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            if (location.accuracy > 25f) return
            val distance = lastSavedLocation?.distanceTo(location) ?: Float.MAX_VALUE
            if (distance < 3f) return

            lastSavedLocation = location
            serviceScope.launch {
                repository.addTrackPoint(currentJourneyId, location.latitude, location.longitude, currentSegmentId)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val journeyId = intent?.getLongExtra("JOURNEY_ID", -1L) ?: -1L
        // FIX TẠI ĐÂY: Dùng getLongExtra thay vì getIntExtra
        currentSegmentId = intent?.getLongExtra("SEGMENT_ID", 1L) ?: 1L

        if (intent?.action == "STOP") {
            stopLocationUpdates()
            stopSelf()
            return START_NOT_STICKY
        }

        if (journeyId != -1L) {
            currentJourneyId = journeyId
            startForegroundService()
            startLocationUpdates()
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateDistanceMeters(2f)
            .build()
        try { fusedLocationClient.requestLocationUpdates(request, locationCallback, null) } catch (e: SecurityException) {}
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun startForegroundService() {
        val channelId = "tracking_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ghi hành trình", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("UED Custom Maps")
            .setContentText("Đang ghi lại hành trình của bạn...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }
}