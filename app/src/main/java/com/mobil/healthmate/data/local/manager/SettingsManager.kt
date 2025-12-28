package com.mobil.healthmate.data.local.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "healthmate_settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val IS_DARK_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("is_dark_mode")
    }

    val lastSyncTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_SYNC_TIME] ?: 0L
        }

    suspend fun updateLastSyncTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = timestamp
        }
    }
}