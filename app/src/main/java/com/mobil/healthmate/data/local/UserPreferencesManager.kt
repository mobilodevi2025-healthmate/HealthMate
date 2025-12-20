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
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_HEIGHT = intPreferencesKey("user_height")
        val USER_WEIGHT = intPreferencesKey("user_weight")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_GENDER = stringPreferencesKey("user_gender")

        val GOAL_TYPE = stringPreferencesKey("goal_type") // "LOSE_WEIGHT", "GAIN_WEIGHT", "MAINTAIN"
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level") // "SEDENTARY", "ACTIVE", etc.
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
        val DAILY_CALORIE_GOAL = intPreferencesKey("daily_calorie_goal")

        val IS_FIRST_RUN = androidx.datastore.preferences.core.booleanPreferencesKey("is_first_run")
    }

    val userData: Flow<UserFullData> = context.dataStore.data.map { preferences ->
        UserFullData(
            isFirstRun = preferences[IS_FIRST_RUN] ?: true,
            profile = UserProfile(
                name = preferences[USER_NAME] ?: "",
                height = preferences[USER_HEIGHT] ?: 170,
                weight = preferences[USER_WEIGHT] ?: 70,
                age = preferences[USER_AGE] ?: 25,
                gender = preferences[USER_GENDER] ?: "Erkek"
            ),
            goals = UserGoals(
                goalType = preferences[GOAL_TYPE] ?: "MAINTAIN",
                activityLevel = preferences[ACTIVITY_LEVEL] ?: "MODERATE",
                stepGoal = preferences[DAILY_STEP_GOAL] ?: 5000,
                calorieGoal = preferences[DAILY_CALORIE_GOAL] ?: 2000
            )
        )
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[IS_FIRST_RUN] = false }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = profile.name
            prefs[USER_HEIGHT] = profile.height
            prefs[USER_WEIGHT] = profile.weight
            prefs[USER_AGE] = profile.age
            prefs[USER_GENDER] = profile.gender
        }
    }

    suspend fun saveGoals(goals: UserGoals) {
        context.dataStore.edit { prefs ->
            prefs[GOAL_TYPE] = goals.goalType
            prefs[ACTIVITY_LEVEL] = goals.activityLevel
            prefs[DAILY_STEP_GOAL] = goals.stepGoal
            prefs[DAILY_CALORIE_GOAL] = goals.calorieGoal
        }
    }
}

data class UserFullData(
    val isFirstRun: Boolean,
    val profile: UserProfile,
    val goals: UserGoals
)

data class UserProfile(
    val name: String,
    val height: Int,
    val weight: Int,
    val age: Int,
    val gender: String
)

data class UserGoals(
    val goalType: String,
    val activityLevel: String,
    val stepGoal: Int,
    val calorieGoal: Int
)