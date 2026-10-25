package com.example.remind

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun scheduleMilestoneAlarms(context: Context, timer: Timer, settings: List<NotificationSetting>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val duration = timer.endDateTime - timer.startDateTime

        settings.forEach { setting ->
            try {
                val triggerTime = when (setting) {
                    is NotificationSetting.PercentageSetting -> {
                        // Bug 4 fix: use Double instead of Float to avoid precision loss on large durations
                        timer.startDateTime + (duration.toDouble() * (setting.percentage / 100.0)).toLong()
                    }
                    is NotificationSetting.TimeSetting -> {
                        // Bug 1 fix: use .toLong() on setting.value first to ensure all arithmetic is in Long,
                        // preventing integer overflow for large durations (e.g. months)
                        val offset = when (setting.unit) {
                            TimeUnitType.MINUTES -> setting.value.toLong() * 60L * 1000L
                            TimeUnitType.DAYS -> setting.value.toLong() * 24L * 60L * 60L * 1000L
                            TimeUnitType.MONTHS -> setting.value.toLong() * 30L * 24L * 60L * 60L * 1000L
                        }
                        if (setting.type == TimeType.BEFORE) {
                            timer.endDateTime - offset
                        } else {
                            timer.startDateTime + offset
                        }
                    }
                }

                val status = when (setting) {
                    is NotificationSetting.PercentageSetting -> "${setting.percentage}% Completed"
                    is NotificationSetting.TimeSetting -> {
                        val unitText = setting.unit.name.lowercase().replaceFirstChar { it.uppercase() }
                        if (setting.type == TimeType.BEFORE) {
                            "${setting.value} ${unitText} left"
                        } else {
                            "${setting.value} ${unitText} completed"
                        }
                    }
                }

                if (triggerTime > System.currentTimeMillis()) {
                    val intent = Intent(context, MilestoneReceiver::class.java).apply {
                        putExtra("timer_name", timer.name)
                        putExtra("status", status)
                        putExtra("timer_id", timer.id)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        (timer.id + setting.id).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelMilestoneAlarms(context: Context, timer: Timer, settings: List<NotificationSetting>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        settings.forEach { setting ->
            val intent = Intent(context, MilestoneReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (timer.id + setting.id).hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
