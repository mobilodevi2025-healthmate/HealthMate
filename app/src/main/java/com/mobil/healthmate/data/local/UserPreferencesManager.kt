package com.mobil.healthmate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val USER_HEIGHT = intPreferencesKey("user_height") // cm
        val USER_WEIGHT = intPreferencesKey("user_weight") // kg
        val USER_AGE = intPreferencesKey("user_age")
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
        val DAILY_CALORIE_GOAL = intPreferencesKey("daily_calorie_goal")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    suspend fun saveUserProfile(
        name: String,
        height: Int,
        weight: Int,
        age: Int,
        stepGoal: Int,
        calorieGoal: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_HEIGHT] = height
            preferences[USER_WEIGHT] = weight
            preferences[USER_AGE] = age
            preferences[DAILY_STEP_GOAL] = stepGoal
            preferences[DAILY_CALORIE_GOAL] = calorieGoal
        }
    }

    val userData: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        UserProfile(
            name = preferences[USER_NAME] ?: "Kullanıcı",
            height = preferences[USER_HEIGHT] ?: 170, // Varsayılan: 170cm
            weight = preferences[USER_WEIGHT] ?: 70,  // Varsayılan: 70kg
            age = preferences[USER_AGE] ?: 25,        // Varsayılan: 25 yaş
            stepGoal = preferences[DAILY_STEP_GOAL] ?: 5000, // Varsayılan hedef
            calorieGoal = preferences[DAILY_CALORIE_GOAL] ?: 2000 // Varsayılan hedef
        )
    }
}

data class UserProfile(
    val name: String,
    val height: Int,
    val weight: Int,
    val age: Int,
    val stepGoal: Int,
    val calorieGoal: Int
)