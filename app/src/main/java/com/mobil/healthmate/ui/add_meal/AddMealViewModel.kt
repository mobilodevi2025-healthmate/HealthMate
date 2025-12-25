package com.mobil.healthmate.ui.add_meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.entity.DailySummaryEntity // Import Eklendi
import com.mobil.healthmate.data.local.types.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mobil.healthmate.domain.repository.HealthRepository
import java.util.UUID
import kotlinx.coroutines.flow.update
import java.util.Calendar // Import Eklendi

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMealState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AddMealEvent) {
        when(event) {
            is AddMealEvent.OnMealTypeChange -> {
                _uiState.value = _uiState.value.copy(mealType = event.type)
            }
            is AddMealEvent.OnAddFood -> {
                val currentList = _uiState.value.addedFoods.toMutableList()
                currentList.add(event.food)
                _uiState.value = _uiState.value.copy(addedFoods = currentList)
            }
            is AddMealEvent.SaveMeal -> {
                saveMealToDatabase()
            }
        }
    }

    private fun saveMealToDatabase() {
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()

            if (currentUser != null) {
                val userId = currentUser.userId
                val currentFoods = _uiState.value.addedFoods

                if (currentFoods.isNotEmpty()) {

                    val totalCals = currentFoods.sumOf { it.calories }
                    val totalProt = currentFoods.sumOf { it.protein }
                    val totalCarb = currentFoods.sumOf { it.carbs }
                    val totalFat = currentFoods.sumOf { it.fat }

                    val meal = MealEntity(
                        mealId = UUID.randomUUID().toString(),
                        userId = userId,
                        mealType = _uiState.value.mealType,
                        date = System.currentTimeMillis(),

                        totalCalories = totalCals,
                        protein = totalProt,
                        carbs = totalCarb,
                        fat = totalFat,

                        isSynced = false
                    )

                    // 3. Yemeği Kaydet
                    repository.insertMealWithFoods(meal, currentFoods)

                    // 4. GÜNLÜK ÖZETİ GÜNCELLE (DÜZELTME 2 - Kalori Grafiği İçin)
                    updateDailySummary(userId, totalCals)

                    // 5. UI'ı Temizle
                    _uiState.update { AddMealState() }
                }
            } else {
                println("HATA: Kullanıcı bulunamadı, yemek kaydedilemedi.")
            }
        }
    }

    // YENİ EKLENEN FONKSİYON: Günlük Özet Tablosunu Günceller
    private suspend fun updateDailySummary(userId: String, caloriesToAdd: Int) {
        // Gün başlangıcını bul (Adım sayar ile aynı güne yazması için)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayDate = calendar.timeInMillis

        // O güne ait bir özet var mı diye bak
        // NOT: Repository'e getSummaryByDate fonksiyonunu eklemiş olman lazım (ProfileViewModel'de kullanmıştık)
        val existingSummary = repository.getSummaryByDateDirect(userId, todayDate) // Flow olmayan, suspend versiyonu

        if (existingSummary != null) {
            // Varsa üzerine ekle (Adım sayısını koru!)
            val updatedSummary = existingSummary.copy(
                totalCaloriesConsumed = existingSummary.totalCaloriesConsumed + caloriesToAdd,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            repository.insertSummary(updatedSummary)
        } else {
            // Yoksa sıfırdan oluştur
            val newSummary = DailySummaryEntity(
                date = todayDate, // timeInMillis değil, gün başlangıcı olmalı
                userId = userId,
                totalCaloriesConsumed = caloriesToAdd,
                totalSteps = 0, // Adım henüz yok
                isSynced = false
            )
            repository.insertSummary(newSummary)
        }
    }
}

data class AddMealState(
    val mealType: MealType = MealType.BREAKFAST,
    val addedFoods: List<FoodEntity> = emptyList(),
    val isLoading: Boolean = false
)

sealed class AddMealEvent {
    data class OnMealTypeChange(val type: MealType): AddMealEvent()
    data class OnAddFood(val food: FoodEntity): AddMealEvent()
    object SaveMeal: AddMealEvent()
}