package com.mobil.healthmate.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import com.mobil.healthmate.data.local.entity.GoalEntity
import com.mobil.healthmate.data.local.entity.UserEntity
import com.mobil.healthmate.data.local.types.ActivityLevel
import com.mobil.healthmate.data.local.types.Gender
import com.mobil.healthmate.data.local.types.GoalType
import com.mobil.healthmate.data.local.manager.ImageStorageManager
import com.mobil.healthmate.data.local.manager.SettingsManager
import com.mobil.healthmate.domain.manager.StepSensorManager
import com.mobil.healthmate.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class DailyMacroStats(
    val date: Long,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val imageStorageManager: ImageStorageManager,
    private val settingsManager: SettingsManager,
    private val stepSensorManager: StepSensorManager
) : ViewModel() {

    private val uid = auth.currentUser?.uid ?: ""

    private val _profileImagePath = MutableStateFlow<String?>(null)
    val profileImagePath = _profileImagePath.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps = _currentSteps.asStateFlow()

    private val _isUserExisting = MutableStateFlow(true)
    val isUserExisting = _isUserExisting.asStateFlow()

    // UI State: Veritabanındaki tüm değişimleri anlık olarak birleştirip UI'ya sunar.
    val uiState: StateFlow<ProfileUiState> = combine(
        repository.getUser(uid),
        repository.getActiveGoal(uid),
        getWeeklySummariesFlow(),
        getWeeklyMacrosFlow(),
        getTodaySummaryFlow()
    ) { user, goal, summaries, macros, todaySummary ->

        _isUserExisting.value = user != null

        // Senkronizasyon sırasında grafiklerin sönmemesi için liste kontrolü
        val finalSummaries = summaries.ifEmpty { emptyList() }
        val finalMacros = macros.ifEmpty { emptyList() }

        ProfileUiState(
            name = user?.name ?: "",
            email = user?.email ?: auth.currentUser?.email ?: "",
            age = user?.age?.toString() ?: "",
            height = user?.height?.toString() ?: "",
            weight = user?.weight?.toString() ?: "",
            gender = user?.gender ?: Gender.MALE,
            activityLevel = user?.activityLevel ?: ActivityLevel.MODERATELY_ACTIVE,
            targetWeight = goal?.targetWeight?.toString() ?: "",
            targetCalories = goal?.dailyCalorieTarget?.toString() ?: "2000",
            dailyStepGoal = goal?.dailyStepTarget?.toString() ?: "10000",
            sleepTargetHours = goal?.dailySleepTarget?.toString() ?: "8.0",
            bedTime = goal?.bedTime ?: "23:00",
            weeklySummaries = finalSummaries,
            weeklyMacros = finalMacros,
            currentCalories = todaySummary?.totalCaloriesConsumed ?: 0,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

    init {
        loadProfileImage()
        listenToSteps()
    }

    private fun getWeeklySummariesFlow(): Flow<List<DailySummaryEntity>> {
        val startOfWeek = getStartOfWeekMillis()
        return repository.getWeeklySummaries(uid, startOfWeek).map { dbList ->
            generateFullWeekSummaries(dbList, startOfWeek)
        }
    }

    private fun getWeeklyMacrosFlow(): Flow<List<DailyMacroStats>> {
        val startOfWeek = getStartOfWeekMillis()
        return repository.getWeeklyMeals(startOfWeek).map { meals ->
            generateFullWeekMacros(meals, startOfWeek)
        }
    }

    private fun getTodaySummaryFlow(): Flow<DailySummaryEntity?> {
        return repository.getSummaryByDate(uid, getStartOfDayMillis())
    }

    private fun generateFullWeekSummaries(dbList: List<DailySummaryEntity>, startOfWeek: Long): List<DailySummaryEntity> {
        return (0..6).map { offset ->
            val dayMillis = startOfWeek + (offset * 24 * 60 * 60 * 1000L)
            dbList.find { isSameDay(it.date, dayMillis) } ?: DailySummaryEntity(
                date = dayMillis, userId = uid, totalCaloriesConsumed = 0, totalSteps = 0
            )
        }
    }

    private fun generateFullWeekMacros(meals: List<com.mobil.healthmate.data.local.entity.MealEntity>, startOfWeek: Long): List<DailyMacroStats> {
        // Tarihleri normalleştirerek grupla (Birden fazla Perşembe oluşmasını engeller)
        val grouped = meals.groupBy { truncateToStartOfDay(it.date) }
        return (0..6).map { offset ->
            val dayMillis = startOfWeek + (offset * 24 * 60 * 60 * 1000L)
            val dayMeals = grouped[dayMillis] ?: emptyList()
            DailyMacroStats(
                date = dayMillis,
                totalProtein = dayMeals.sumOf { it.protein }.toFloat(),
                totalCarbs = dayMeals.sumOf { it.carbs }.toFloat(),
                totalFat = dayMeals.sumOf { it.fat }.toFloat()
            )
        }
    }

    private fun listenToSteps() {
        viewModelScope.launch {
            stepSensorManager.getStepCount().collect { steps ->
                _currentSteps.value = steps
                saveStepsToDb(uid, steps)
            }
        }
    }

    private fun saveStepsToDb(uid: String, steps: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStart = getStartOfDayMillis()
            val todaySummary = repository.getSummaryByDateDirect(uid, todayStart)
            if (todaySummary != null) {
                // Upsert mantığı ile güncelle
                repository.insertSummary(todaySummary.copy(totalSteps = steps, updatedAt = System.currentTimeMillis()))
            } else {
                repository.insertSummary(DailySummaryEntity(userId = uid, date = todayStart, totalSteps = steps))
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when(event) {
            is ProfileEvent.SaveProfile -> saveAllData(event)
            is ProfileEvent.SignOut -> auth.signOut()
        }
    }

    private fun saveAllData(event: ProfileEvent.SaveProfile) {
        viewModelScope.launch {
            val user = UserEntity(
                userId = uid, name = event.name, email = auth.currentUser?.email ?: "",
                age = event.age.toIntOrNull() ?: 25, height = event.height.toDoubleOrNull() ?: 170.0,
                weight = event.weight.toDoubleOrNull() ?: 70.0, gender = event.gender, activityLevel = event.activityLevel
            )
            val goal = GoalEntity(
                userId = uid, mainGoalType = GoalType.MAINTAIN_WEIGHT, targetWeight = event.targetWeight.toDoubleOrNull(),
                dailyCalorieTarget = event.targetCalories.toIntOrNull() ?: 2000, dailyStepTarget = event.dailyStepGoal.toIntOrNull() ?: 10000,
                dailySleepTarget = event.sleepTargetHours.toDoubleOrNull() ?: 8.0, bedTime = event.bedTime, startDate = System.currentTimeMillis()
            )

            // DAO'larda 'upsert' (Insert IGNORE + Update) mantığı çalıştığı için
            // ilişkili veriler (yemek geçmişi) silinmez.
            repository.insertUser(user)
            repository.insertGoal(goal)

            launch(Dispatchers.IO) { saveUserToFirestore(user, goal) }

            _isEditing.value = false
        }
    }

    fun toggleEditMode() { _isEditing.value = !_isEditing.value }

    fun cancelEdit() { _isEditing.value = false }

    // --- YARDIMCI ZAMAN FONKSİYONLARI (Rasyonel Normalizasyon İçin) ---

    private fun getStartOfWeekMillis(): Long {
        return Calendar.getInstance(Locale("tr")).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOfDayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun truncateToStartOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun loadProfileImage() {
        imageStorageManager.getProfileImageFile(uid)?.let { _profileImagePath.value = it.absolutePath }
    }

    fun onProfileImageSelected(uri: Uri) {
        viewModelScope.launch {
            val path = imageStorageManager.saveProfileImage(uri, uid)
            if (path.isNotEmpty()) _profileImagePath.value = path
        }
    }

    private fun saveUserToFirestore(user: UserEntity, goal: GoalEntity) {
        val userMap = hashMapOf(
            "userId" to user.userId, "name" to user.name, "email" to user.email,
            "targetWeight" to (goal.targetWeight ?: 0.0), "dailyCalorieTarget" to goal.dailyCalorieTarget,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(user.userId).set(userMap)
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
    val sleepTargetHours: String = "8.0",
    val bedTime: String = "23:00",
    val currentCalories: Int = 0,
    val weeklySummaries: List<DailySummaryEntity> = emptyList(),
    val weeklyMacros: List<DailyMacroStats> = emptyList(),
    val isLoading: Boolean = true
)

sealed class ProfileEvent {
    data class SaveProfile(
        val name: String, val age: String, val height: String, val weight: String,
        val gender: Gender, val activityLevel: ActivityLevel, val targetWeight: String,
        val targetCalories: String, val dailyStepGoal: String, val sleepTargetHours: String, val bedTime: String
    ) : ProfileEvent()
    object SignOut : ProfileEvent()
}