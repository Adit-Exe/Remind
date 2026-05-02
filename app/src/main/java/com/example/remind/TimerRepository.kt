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

class TimerRepository(private val context: Context) {
    private val TIMERS_KEY = stringPreferencesKey("timers_list")

    val timersFlow: Flow<List<Timer>> = context.dataStore.data.map { preferences ->
        val json = preferences[TIMERS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<Timer>>(json).sortedBy { it.endDateTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTimers(timers: List<Timer>) {
        context.dataStore.edit { preferences ->
            preferences[TIMERS_KEY] = Json.encodeToString(timers)
        }
    }
}
