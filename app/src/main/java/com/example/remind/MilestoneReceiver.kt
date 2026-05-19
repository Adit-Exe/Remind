package com.example.remind

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class MilestoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerName = intent.getStringExtra("timer_name") ?: "Timer"
        val status = intent.getStringExtra("status") ?: "Completed"
        val timerId = intent.getStringExtra("timer_id") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alertId = (timerId + status).hashCode()

        val notification = NotificationCompat.Builder(context, "alert_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$timerName $status")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alertId, notification)
    }
}
