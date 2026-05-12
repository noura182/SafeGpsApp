package com.safegps

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.safegps.service.LocationForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeGpsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LocationForegroundService.CHANNEL_ID,
                "GPS Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for continuous location tracking"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
