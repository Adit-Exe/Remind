package com.example.remind

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "timers")

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
    classDiscriminator = "kind"
}

class TimerRepository(private val context: Context) {
    private val TIMERS_KEY = stringPreferencesKey("timers_list")
    private val NOTIFICATION_SETTINGS_KEY = stringPreferencesKey("notification_settings")

    val timersFlow: Flow<List<Timer>> = context.dataStore.data.map { preferences ->
        val json = preferences[TIMERS_KEY] ?: "[]"
        try {
            jsonConfig.decodeFromString<List<Timer>>(json).sortedBy { it.endDateTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val notificationSettingsFlow: Flow<List<NotificationSetting>> = context.dataStore.data.map { preferences ->
        val json = preferences[NOTIFICATION_SETTINGS_KEY]
        if (json == null) {
            getDefaultSettings()
        } else {
            try {
                jsonConfig.decodeFromString<List<NotificationSetting>>(json)
            } catch (e: Exception) {
                getDefaultSettings()
            }
        }
    }

    private fun getDefaultSettings(): List<NotificationSetting> {
        return listOf(
            NotificationSetting.PercentageSetting(percentage = 50),
            NotificationSetting.PercentageSetting(percentage = 90),
            NotificationSetting.PercentageSetting(percentage = 100),
            NotificationSetting.TimeSetting(type = TimeType.AFTER, value = 10, unit = TimeUnitType.DAYS),
            NotificationSetting.TimeSetting(type = TimeType.BEFORE, value = 1, unit = TimeUnitType.DAYS),
            NotificationSetting.TimeSetting(type = TimeType.BEFORE, value = 10, unit = TimeUnitType.MINUTES)
        )
    }

    suspend fun saveTimers(timers: List<Timer>) {
        context.dataStore.edit { preferences ->
            preferences[TIMERS_KEY] = jsonConfig.encodeToString(timers)
        }
    }

    suspend fun saveNotificationSettings(settings: List<NotificationSetting>) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SETTINGS_KEY] = jsonConfig.encodeToString(settings)
        }
    }
}
