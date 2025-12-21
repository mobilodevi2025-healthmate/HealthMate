package com.mobil.healthmate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            isFirstRun = preferences[IS_FIRST_RUN] ?: true,
            isDarkMode = preferences[IS_DARK_MODE] ?: false,
            lastSyncTimestamp = preferences[LAST_SYNC_TIMESTAMP] ?: 0L
        )
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[IS_FIRST_RUN] = false }
    }

    suspend fun setDarkMode(isEnabled: Boolean) {
        context.dataStore.edit { it[IS_DARK_MODE] = isEnabled }
    }

    suspend fun updateLastSyncTime(timestamp: Long) {
        context.dataStore.edit { it[LAST_SYNC_TIMESTAMP] = timestamp }
    }
}

data class AppSettings(
    val isFirstRun: Boolean,
    val isDarkMode: Boolean,
    val lastSyncTimestamp: Long
)