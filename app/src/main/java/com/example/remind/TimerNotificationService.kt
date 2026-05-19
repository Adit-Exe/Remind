package com.example.remind

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class TimerNotificationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: TimerRepository
    private val notificationIdMap = mutableMapOf<String, Int>()

    override fun onCreate() {
        super.onCreate()
        repository = TimerRepository(applicationContext)
        
        // Android 14+ requires calling startForeground immediately
        val initialNotification = NotificationCompat.Builder(this, "timer_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Remind")
            .setContentText("Checking timers...")
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, initialNotification)
        }
        
        serviceScope.launch {
            while (isActive) {
                val timers = repository.timersFlow.first()
                val pinnedTimers = timers.filter { it.isPinned }
                
                if (pinnedTimers.isEmpty()) {
                    // No pinned timers? Stop the foreground service and the persistent notification.
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                
                updatePinnedNotifications(pinnedTimers)
                
                try {
                    TimerWidget().updateAll(applicationContext)
                } catch (e: Exception) {}
                
                delay(1000)
            }
        }
    }

    private fun updatePinnedNotifications(timers: List<Timer>) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val now = System.currentTimeMillis()

        timers.forEachIndexed { index, timer ->
            val progress = Utils.calculateProgress(timer.startDateTime, timer.endDateTime, now)
            val remaining = Utils.getRemainingTimeText(timer.endDateTime, now)
            val isCompleted = progress >= 1.0f

            val notification = NotificationCompat.Builder(this, "timer_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(timer.name)
                .setContentText(if (isCompleted) "Completed" else remaining)
                .setProgress(100, (progress * 100).toInt(), false)
                .setOngoing(!isCompleted)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()

                val notificationId = timer.id.hashCode()
                if (index == 0) {
                    // The first pinned timer keeps the service alive
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(notificationId, notification)
                    }
                } else {
                    notificationManager.notify(notificationId, notification)
                }
            notificationIdMap[timer.id] = notificationId
        }

        // Cleanup notifications for timers that were just unpinned
        val currentPinnedIds = timers.map { it.id }.toSet()
        val idsToRemove = notificationIdMap.keys.filter { it !in currentPinnedIds }
        idsToRemove.forEach { id ->
            notificationManager.cancel(notificationIdMap[id]!!)
            notificationIdMap.remove(id)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
