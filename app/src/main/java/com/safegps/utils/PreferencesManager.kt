package com.safegps.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "settings")

object PreferencesManager {
    private val INTERVAL_KEY = longPreferencesKey("update_interval")
    private const val DEFAULT_INTERVAL = 5 * 60 * 1000L // 5 minutes

    fun getInterval(context: Context): Long = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[INTERVAL_KEY] ?: DEFAULT_INTERVAL
        }.first()
    }

    suspend fun saveInterval(context: Context, interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[INTERVAL_KEY] = interval
        }
    }
}
