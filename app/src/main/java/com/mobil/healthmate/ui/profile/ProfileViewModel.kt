package com.mobil.healthmate.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.entity.GoalEntity
import com.mobil.healthmate.data.local.entity.UserEntity
import com.mobil.healthmate.data.local.manager.ImageStorageManager
import com.mobil.healthmate.data.local.manager.SettingsManager
import com.mobil.healthmate.data.local.types.ActivityLevel
import com.mobil.healthmate.data.local.types.Gender
import com.mobil.healthmate.data.local.types.GoalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mobil.healthmate.domain.repository.HealthRepository
import com.mobil.healthmate.domain.manager.StepSensorManager
import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.Dispatchers
import java.util.Calendar // Calendar importunu unutma!

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val imageStorageManager: ImageStorageManager,
    private val settingsManager: SettingsManager,
    private val stepSensorManager: StepSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _profileImagePath = MutableStateFlow<String?>(null)
    val profileImagePath = _profileImagePath.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    private val _isUserExisting = MutableStateFlow(true)
    val isUserExisting = _isUserExisting.asStateFlow()

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps = _currentSteps.asStateFlow()

    // EKSİK OLAN 1: Haftalık veriler için MutableStateFlow
    private val _weeklySummaries = MutableStateFlow<List<DailySummaryEntity>>(emptyList())
    val weeklySummaries = _weeklySummaries.asStateFlow()

    // EKSİK OLAN 2: Kalori için MutableStateFlow
    private val _currentCalories = MutableStateFlow(0)
    val currentCalories = _currentCalories.asStateFlow()

    init {
        loadProfileData()
        loadProfileImage()
        listenToSteps()
        listenToWeeklyData()
        listenToTodayCalories() // Kalori dinlemeyi başlat
    }

    private fun listenToSteps() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            stepSensorManager.getStepCount().collect { steps ->
                _currentSteps.value = steps
                saveStepsToDb(uid, steps)
            }
        }
    }

    private fun listenToWeeklyData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getLast7DaysSummary(uid).collect { list ->
                _weeklySummaries.value = list // Artık hata vermez
            }
        }
    }

    // YENİ EKLENEN FONKSİYON: Kaloriyi veritabanından anlık çeker
    private fun listenToTodayCalories() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // Bugünün başlangıç zamanını bul (00:00)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis

            // Repository'e eklediğimiz fonksiyonu kullanıyoruz
            repository.getSummaryByDate(uid, todayStart).collect { summary ->
                if (summary != null) {
                    _currentCalories.value = summary.totalCaloriesConsumed
                } else {
                    _currentCalories.value = 0
                }
            }
        }
    }

    private fun saveStepsToDb(uid: String, steps: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todaySummary = repository.getTodaySummary(uid)

                if (todaySummary != null) {
                    val updatedSummary = todaySummary.copy(
                        totalSteps = steps,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false
                    )
                    repository.insertSummary(updatedSummary)
                } else {
                    val newSummary = DailySummaryEntity(
                        userId = uid,
                        date = System.currentTimeMillis(),
                        totalSteps = steps,
                        isSynced = false
                    )
                    repository.insertSummary(newSummary)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        val file = imageStorageManager.getProfileImageFile(uid)
        if (file != null) {
            _profileImagePath.value = file.absolutePath
        }
    }

    fun onProfileImageSelected(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val path = imageStorageManager.saveProfileImage(uri, uid)

            if (path.isNotEmpty()) {
                _profileImagePath.value = path
            }
            settingsManager.updateLastSyncTime(System.currentTimeMillis())
        }
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

                    sleepTargetHours = goal?.dailySleepTarget?.toString() ?: "8.0",
                    bedTime = goal?.bedTime ?: "23:00",

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
                dailyCalorieTarget = event.targetCalories.toIntOrNull() ?: 2000,
                dailyStepTarget = event.dailyStepGoal.toIntOrNull() ?: 10000,

                dailySleepTarget = event.sleepTargetHours.toDoubleOrNull() ?: 8.0,
                bedTime = event.bedTime,

                startDate = System.currentTimeMillis()
            )
            repository.insertGoal(goal)

            saveUserToFirestore(user, goal)

            _isUserExisting.value = true
        }
    }

    private fun saveUserToFirestore(user: UserEntity, goal: GoalEntity) {
        val userMap = hashMapOf(
            "userId" to user.userId,
            "name" to user.name,
            "email" to user.email,
            "age" to user.age,
            "height" to user.height,
            "weight" to user.weight,
            "gender" to user.gender.name,
            "activityLevel" to user.activityLevel.name,

            "targetWeight" to (goal.targetWeight ?: 0.0),
            "dailyCalorieTarget" to goal.dailyCalorieTarget,
            "dailyStepTarget" to goal.dailyStepTarget,

            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(user.userId)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("ProfileViewModel", "Firestore: Profil başarıyla yedeklendi.")
            }
            .addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Firestore Hatası: ${e.message}")
            }
    }

    private fun signOut() {
        auth.signOut()
    }
}

// ... Data Class ve Sealed Class'lar aynı kalabilir ...
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

    val sleepTargetHours: String = "8.0",
    val bedTime: String = "23:00",

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
        val dailyStepGoal: String,
        val sleepTargetHours: String,
        val bedTime: String
    ) : ProfileEvent()

    object SignOut : ProfileEvent()
}