package com.example.remind

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class RemindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        
        // Channel for live updates (Silent)
        val timerChannel = NotificationChannel(
            "timer_channel",
            "Active Timers",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
        }
        
        // Channel for milestone alerts (Sound/Vibration)
        val alertChannel = NotificationChannel(
            "alert_channel",
            "Milestone Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableLights(true)
            enableVibration(true)
        }
        
        manager.createNotificationChannel(timerChannel)
        manager.createNotificationChannel(alertChannel)
    }
}
