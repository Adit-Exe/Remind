package com.example.remind

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder

class MilestoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerName = intent.getStringExtra("timer_name") ?: "Timer"
        val status = intent.getStringExtra("status") ?: "Completed"
        val timerId = intent.getStringExtra("timer_id") ?: ""

        val alertId = (timerId + status).hashCode()

        val notification = NotificationCompat.Builder(context, "alert_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(timerName)
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alertId, notification)
        } catch (e: SecurityException) {
            // Handle cases where notification permission might have been revoked
        }
    }
}
