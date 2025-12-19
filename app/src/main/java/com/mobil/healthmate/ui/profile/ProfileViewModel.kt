package com.mobil.healthmate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.data.local.UserFullData
import com.mobil.healthmate.data.local.UserGoals
import com.mobil.healthmate.data.local.UserPreferencesManager
import com.mobil.healthmate.data.local.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val userFullData: StateFlow<UserFullData?> = userPreferencesManager.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveBioProfile(name: String, height: String, weight: String, age: String, gender: String) {
        viewModelScope.launch {
            val newProfile = UserProfile(
                name = name,
                height = height.toIntOrNull() ?: 170,
                weight = weight.toIntOrNull() ?: 70,
                age = age.toIntOrNull() ?: 25,
                gender = gender
            )
            userPreferencesManager.saveProfile(newProfile)
        }
    }

    fun saveStrategyGoals(goalType: String, activityLevel: String, stepGoal: String, calorieGoal: String) {
        viewModelScope.launch {
            val newGoals = UserGoals(
                goalType = goalType,
                activityLevel = activityLevel,
                stepGoal = stepGoal.toIntOrNull() ?: 5000,
                calorieGoal = calorieGoal.toIntOrNull() ?: 2000
            )
            userPreferencesManager.saveGoals(newGoals)
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            userPreferencesManager.completeOnboarding()
        }
    }
}