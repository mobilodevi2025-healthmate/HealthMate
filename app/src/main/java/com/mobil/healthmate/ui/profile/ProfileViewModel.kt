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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    // --- YENİ EKLENEN BAĞIMLILIKLAR ---
    private val imageStorageManager: ImageStorageManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // --- YENİ STATE: Profil Resmi Yolu ---
    private val _profileImagePath = MutableStateFlow<String?>(null)
    val profileImagePath = _profileImagePath.asStateFlow()

    // Düzenleme modu açık mı?
    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    // Kullanıcının veritabanında kaydı var mı?
    private val _isUserExisting = MutableStateFlow(true)
    val isUserExisting = _isUserExisting.asStateFlow()

    init {
        loadProfileData()
        loadProfileImage() // Resmi yüklemeyi başlat
    }

    // --- YENİ FONKSİYON: Resmi Dahili Hafızadan Yükle ---
    private fun loadProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        // ImageStorageManager dosya kontrolü yapar
        val file = imageStorageManager.getProfileImageFile(uid)
        if (file != null) {
            _profileImagePath.value = file.absolutePath
        }
    }

    // --- YENİ FONKSİYON: Galeriden Seçilen Resmi Kaydet ---
    fun onProfileImageSelected(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // 1. Resmi Internal Storage'a güvenli şekilde kaydet
            val path = imageStorageManager.saveProfileImage(uri, uid)

            // 2. State'i güncelle (UI anında değişir)
            if (path.isNotEmpty()) {
                _profileImagePath.value = path
            }

            // 3. Son güncelleme zamanını kaydet (DataStore)
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
            // 1. User Entity Oluştur
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
            // Yerel DB'ye kaydet
            repository.insertUser(user)

            // 2. Goal Entity Oluştur
            val goal = GoalEntity(
                userId = uid,
                mainGoalType = GoalType.MAINTAIN_WEIGHT,
                targetWeight = event.targetWeight.toDoubleOrNull(),
                dailyCalorieTarget = event.targetCalories.toIntOrNull() ?: 2000,
                dailyStepTarget = event.dailyStepGoal.toIntOrNull() ?: 10000,
                startDate = System.currentTimeMillis()
            )
            // Yerel DB'ye kaydet
            repository.insertGoal(goal)

            // 3. FIRESTORE KAYDI (BACKUP/SYNC)
            saveUserToFirestore(user, goal)

            _isUserExisting.value = true
        }
    }

    // Kullanıcıyı ve hedeflerini buluta yedekler
    private fun saveUserToFirestore(user: UserEntity, goal: GoalEntity) {
        val userMap = hashMapOf(
            // Profil Bilgileri
            "userId" to user.userId,
            "name" to user.name,
            "email" to user.email,
            "age" to user.age,
            "height" to user.height,
            "weight" to user.weight,
            "gender" to user.gender.name, // Enum -> String
            "activityLevel" to user.activityLevel.name, // Enum -> String

            // Hedef Bilgileri (Aynı dökümanda tutuyoruz)
            "targetWeight" to (goal.targetWeight ?: 0.0),
            "dailyCalorieTarget" to goal.dailyCalorieTarget,
            "dailyStepTarget" to goal.dailyStepTarget,

            // Meta veri
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

// --- DATA CLASSES ---

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