package com.example.remind

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimerViewModel(private val repository: TimerRepository) : ViewModel() {

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
        }
    }

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            val currentList = timers.value.toMutableList()
            currentList.removeAll { it.id == timer.id }
            repository.saveTimers(currentList)
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
