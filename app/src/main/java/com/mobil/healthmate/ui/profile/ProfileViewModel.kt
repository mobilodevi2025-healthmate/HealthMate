package com.mobil.healthmate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.entity.GoalEntity
import com.mobil.healthmate.data.local.entity.UserEntity
import com.mobil.healthmate.data.local.types.ActivityLevel
import com.mobil.healthmate.data.local.types.Gender
import com.mobil.healthmate.data.local.types.GoalType
import com.mobil.healthmate.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // Düzenleme modu açık mı?
    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    // Kullanıcının veritabanında kaydı var mı?
    private val _isUserExisting = MutableStateFlow(true)
    val isUserExisting = _isUserExisting.asStateFlow()

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            combine(
                repository.getUser(uid),
                repository.getActiveGoal(uid)
            ) { user, goal ->
                _isUserExisting.value = user != null

                if (user == null) {
                    _isEditing.value = true
                }

                ProfileUiState(
                    name = user?.name ?: "",
                    email = user?.email ?: auth.currentUser?.email ?: "",
                    age = user?.age?.toString() ?: "",
                    height = user?.height?.toString() ?: "",
                    weight = user?.weight?.toString() ?: "",
                    gender = user?.gender ?: Gender.MALE,
                    activityLevel = user?.activityLevel ?: ActivityLevel.MODERATELY_ACTIVE,

                    targetWeight = goal?.targetWeight?.toString() ?: "",
                    targetCalories = goal?.dailyCalorieTarget?.toString() ?: "",
                    dailyStepGoal = goal?.dailyStepTarget?.toString() ?: "10000",

                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun toggleEditMode() {
        _isEditing.value = !_isEditing.value
    }

    fun cancelEdit() {
        _isEditing.value = false
        loadProfileData()
    }

    fun onEvent(event: ProfileEvent) {
        when(event) {
            is ProfileEvent.SaveProfile -> {
                saveAllData(event)
                _isEditing.value = false
            }
            is ProfileEvent.SignOut -> signOut()
        }
    }

    private fun saveAllData(event: ProfileEvent.SaveProfile) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = UserEntity(
                userId = uid,
                name = event.name,
                email = auth.currentUser?.email ?: "",
                age = event.age.toIntOrNull() ?: 25,
                height = event.height.toDoubleOrNull() ?: 170.0,
                weight = event.weight.toDoubleOrNull() ?: 70.0,
                gender = event.gender,
                activityLevel = event.activityLevel
            )
            repository.insertUser(user)

            val goal = GoalEntity(
                userId = uid,
                mainGoalType = GoalType.MAINTAIN_WEIGHT,
                targetWeight = event.targetWeight.toDoubleOrNull(),
                // Event'ten gelen targetCalories'i dailyCalorieTarget'a yazıyoruz
                dailyCalorieTarget = event.targetCalories.toIntOrNull() ?: 2000,
                dailyStepTarget = event.dailyStepGoal.toIntOrNull() ?: 10000,
                startDate = System.currentTimeMillis()
            )
            repository.insertGoal(goal)

            _isUserExisting.value = true
        }
    }

    private fun signOut() {
        auth.signOut()
    }
}

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val gender: Gender = Gender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,

    val targetWeight: String = "",
    val targetCalories: String = "",
    val dailyStepGoal: String = "",

    val isLoading: Boolean = true
)

sealed class ProfileEvent {
    data class SaveProfile(
        val name: String,
        val age: String,
        val height: String,
        val weight: String,
        val gender: Gender,
        val activityLevel: ActivityLevel,
        val targetWeight: String,
        val targetCalories: String,
        val dailyStepGoal: String
    ) : ProfileEvent()

    object SignOut : ProfileEvent()
}