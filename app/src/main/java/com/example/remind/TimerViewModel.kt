package com.example.remind

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimerViewModel(private val repository: TimerRepository, private val application: Application) : AndroidViewModel(application) {

    val timers = repository.timersFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime = _currentTime.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    fun addTimer(name: String, endDateTime: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newTimer = Timer(name = name, endDateTime = endDateTime, startDateTime = now)
            val currentList = timers.value.toMutableList()
            currentList.add(newTimer)
            repository.saveTimers(currentList)
            scheduleMilestoneAlarms(newTimer)
        }
    }

    private fun scheduleMilestoneAlarms(timer: Timer) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val duration = timer.endDateTime - timer.startDateTime
        
        val milestones = listOf(
            0.5f to "50% Completed",
            0.9f to "90% Completed",
            1.0f to "Completed"
        )

        milestones.forEach { (fraction, status) ->
            val triggerTime = timer.startDateTime + (duration * fraction).toLong()
            if (triggerTime > System.currentTimeMillis()) {
                val intent = Intent(application, MilestoneReceiver::class.java).apply {
                    putExtra("timer_name", timer.name)
                    putExtra("status", status)
                    putExtra("timer_id", timer.id)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    application,
                    (timer.id + status).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Fallback to inexact if permission not granted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            cancelMilestoneAlarms(timer)
            val currentList = timers.value.toMutableList()
            currentList.removeAll { it.id == timer.id }
            repository.saveTimers(currentList)
        }
    }

    private fun cancelMilestoneAlarms(timer: Timer) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val statuses = listOf("50% Completed", "90% Completed", "Completed")
        
        statuses.forEach { status ->
            val intent = Intent(application, MilestoneReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                application,
                (timer.id + status).hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    fun togglePin(timer: Timer) {
        viewModelScope.launch {
            val currentList = timers.value.map {
                if (it.id == timer.id) it.copy(isPinned = !it.isPinned) else it
            }
            repository.saveTimers(currentList)
        }
    }
}
