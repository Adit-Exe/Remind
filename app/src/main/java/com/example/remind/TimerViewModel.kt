package com.example.remind

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class TimerViewModel(private val repository: TimerRepository, private val application: Application) : AndroidViewModel(application) {

    val timers = repository.timersFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val notificationSettings = repository.notificationSettingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen = _isSettingsOpen.asStateFlow()

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime = _currentTime.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadSizeInfo = MutableStateFlow("")
    val downloadSizeInfo = _downloadSizeInfo.asStateFlow()

    private val _isUpdateDone = MutableStateFlow(false)
    val isUpdateDone = _isUpdateDone.asStateFlow()

    private var downloadedApkFile: File? = null

    init {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://adit-exe.github.io/Portfolio/remind/remind.json")
                val connection = url.openConnection() as HttpURLConnection
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val info = Json.decodeFromString<UpdateInfo>(jsonText)

                val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }

                if (info.versionCode > currentVersionCode) {
                    _updateInfo.value = info
                    _showUpdateDialog.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun startUpdate() {
        _showUpdateDialog.value = false
        _isUpdating.value = true
        downloadUpdate()
    }

    private fun downloadUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(info.url)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val apkFile = File(application.cacheDir, "update.apk")
                downloadedApkFile = apkFile
                val outputStream = FileOutputStream(apkFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        _downloadProgress.value = total.toFloat() / fileLength
                        val totalMB = fileLength / (1024 * 1024f)
                        val currentMB = total / (1024 * 1024f)
                        _downloadSizeInfo.value = String.format(Locale.US, "%.1f MB / %.1f MB", currentMB, totalMB)
                    }
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                _isUpdateDone.value = true
                _downloadProgress.value = 1.0f
            } catch (e: Exception) {
                e.printStackTrace()
                _isUpdating.value = false
            }
        }
    }

    fun installUpdate() {
        val apkFile = downloadedApkFile ?: return
        val contentUri: Uri = FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        application.startActivity(intent)
    }

    fun addTimer(name: String, endDateTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val newTimer = Timer(name = name, endDateTime = endDateTime, startDateTime = now)
            val currentList = timers.value.toMutableList()
            currentList.add(newTimer)
            repository.saveTimers(currentList)
            withContext(Dispatchers.Default) {
                scheduleMilestoneAlarms(newTimer)
            }
        }
    }

    private suspend fun scheduleMilestoneAlarms(timer: Timer, settings: List<NotificationSetting>? = null) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val duration = timer.endDateTime - timer.startDateTime
        val currentSettings = settings ?: repository.notificationSettingsFlow.first()

        currentSettings.forEach { setting ->
            try {
                val triggerTime = when (setting) {
                    is NotificationSetting.PercentageSetting -> {
                        timer.startDateTime + (duration * (setting.percentage / 100f)).toLong()
                    }
                    is NotificationSetting.TimeSetting -> {
                        val offset = when (setting.unit) {
                            TimeUnitType.MINUTES -> setting.value * 60 * 1000L
                            TimeUnitType.DAYS -> setting.value * 24 * 60 * 60 * 1000L
                            TimeUnitType.MONTHS -> setting.value * 30 * 24 * 60 * 60 * 1000L // Approximation
                        }
                        // Trigger time is calculated from timer start date, not end date
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
                    val intent = Intent(application, MilestoneReceiver::class.java).apply {
                        putExtra("timer_name", timer.name)
                        putExtra("status", status)
                        putExtra("timer_id", timer.id)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        application,
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

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelMilestoneAlarms(timer)
            val currentList = timers.value.toMutableList()
            currentList.removeAll { it.id == timer.id }
            repository.saveTimers(currentList)
        }
    }

    private suspend fun cancelMilestoneAlarms(timer: Timer, settings: List<NotificationSetting>? = null) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentSettings = settings ?: repository.notificationSettingsFlow.first()

        currentSettings.forEach { setting ->
            val intent = Intent(application, MilestoneReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                application,
                (timer.id + setting.id).hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    fun togglePin(timer: Timer) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = timers.value.map {
                if (it.id == timer.id) it.copy(isPinned = !it.isPinned) else it
            }
            repository.saveTimers(currentList)
        }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun addNotificationSetting(setting: NotificationSetting) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = repository.notificationSettingsFlow.first().toMutableList()
                current.add(setting)
                repository.saveNotificationSettings(current)
                rescheduleAllTimers(current)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateNotificationSetting(setting: NotificationSetting) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = repository.notificationSettingsFlow.first().map {
                    if (it.id == setting.id) setting else it
                }
                repository.saveNotificationSettings(current)
                rescheduleAllTimers(current)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteNotificationSetting(settingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = repository.notificationSettingsFlow.first().filter { it.id != settingId }
                repository.saveNotificationSettings(current)
                rescheduleAllTimers(current)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun rescheduleAllTimers(settings: List<NotificationSetting>? = null) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentSettings = settings ?: repository.notificationSettingsFlow.first()
            timers.value.forEach { timer ->
                cancelMilestoneAlarms(timer, currentSettings)
                scheduleMilestoneAlarms(timer, currentSettings)
            }
        }
    }
}
