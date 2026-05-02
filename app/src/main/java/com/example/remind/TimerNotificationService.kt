package com.example.remind

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class TimerNotificationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: TimerRepository
    private val notificationIdMap = mutableMapOf<String, Int>()
    private val SERVICE_NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        repository = TimerRepository(applicationContext)
        
        serviceScope.launch {
            while (isActive) {
                val timers = repository.timersFlow.first()
                val pinnedTimers = timers.filter { it.isPinned }
                val hasPendingMilestones = timers.any { !it.notifiedAt100 && it.endDateTime > System.currentTimeMillis() }
                
                if (pinnedTimers.isEmpty() && !hasPendingMilestones) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                
                checkMilestones(timers)
                updatePinnedNotifications(pinnedTimers)
                
                // Always update widget in the loop
                try {
                    TimerWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    // Ignore widget update errors if no instances exist
                }
                
                delay(1000)
            }
        }
    }

    private suspend fun checkMilestones(timers: List<Timer>) {
        val now = System.currentTimeMillis()
        val updatedTimers = timers.toMutableList()
        var changed = false

        timers.forEachIndexed { index, timer ->
            if (timer.endDateTime <= timer.startDateTime) return@forEachIndexed
            
            val progress = Utils.calculateProgress(timer.startDateTime, timer.endDateTime, now)
            val currentTimer = updatedTimers[index]
            
            if (progress >= 1.0f && !currentTimer.notifiedAt100) {
                sendAlert(currentTimer, "Completed")
                updatedTimers[index] = currentTimer.copy(notifiedAt100 = true, notifiedAt90 = true, notifiedAt50 = true)
                changed = true
            } else if (progress >= 0.9f && !currentTimer.notifiedAt90) {
                sendAlert(currentTimer, "90% Completed")
                updatedTimers[index] = currentTimer.copy(notifiedAt90 = true, notifiedAt50 = true)
                changed = true
            } else if (progress >= 0.5f && !currentTimer.notifiedAt50) {
                sendAlert(currentTimer, "50% Completed")
                updatedTimers[index] = currentTimer.copy(notifiedAt50 = true)
                changed = true
            }
        }

        if (changed) {
            repository.saveTimers(updatedTimers)
        }
    }

    private fun sendAlert(timer: Timer, status: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val alertId = (timer.id + "_alert").hashCode()
        
        val notification = NotificationCompat.Builder(this, "alert_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${timer.name} $status")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(alertId, notification)
    }

    private fun updatePinnedNotifications(timers: List<Timer>) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val now = System.currentTimeMillis()

        if (timers.isEmpty()) {
            // Show a silent placeholder notification to keep the foreground service alive
            val placeholder = NotificationCompat.Builder(this, "timer_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Remind")
                .setContentText("Monitoring active timers...")
                .setOngoing(true)
                .setSilent(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
            
            startForeground(SERVICE_NOTIFICATION_ID, placeholder)
        } else {
            // Cancel the placeholder if it was showing
            notificationManager.cancel(SERVICE_NOTIFICATION_ID)
            
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
                    startForeground(notificationId, notification)
                } else {
                    notificationManager.notify(notificationId, notification)
                }
                notificationIdMap[timer.id] = notificationId
            }
        }

        // Cleanup stale notifications
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
