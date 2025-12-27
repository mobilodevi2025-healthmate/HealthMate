package com.mobil.healthmate.ui.add_meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import com.mobil.healthmate.data.local.types.MealType
import com.mobil.healthmate.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Calendar
import javax.inject.Inject

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
                _uiState.update { it.copy(mealType = event.type) }
            }
            is AddMealEvent.OnAddFood -> {
                val currentList = _uiState.value.addedFoods.toMutableList()
                currentList.add(event.food)
                _uiState.update { it.copy(addedFoods = currentList) }
            }
            is AddMealEvent.SaveMeal -> {
                saveMealToDatabase()
            }
        }
    }

    private fun saveMealToDatabase() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentFoods = _uiState.value.addedFoods
            if (currentFoods.isNotEmpty()) {
                val totalCals = currentFoods.sumOf { it.calories }
                val totalProt = currentFoods.sumOf { it.protein }
                val totalCarb = currentFoods.sumOf { it.carbs }
                val totalFat = currentFoods.sumOf { it.fat }

                val meal = MealEntity(
                    mealId = UUID.randomUUID().toString(),
                    userId = uid,
                    mealType = _uiState.value.mealType,
                    date = System.currentTimeMillis(),
                    totalCalories = totalCals,
                    protein = totalProt,
                    carbs = totalCarb,
                    fat = totalFat,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )

                repository.insertMealWithFoods(meal, currentFoods)
                updateDailySummary(uid, totalCals, totalProt.toFloat(), totalCarb.toFloat(), totalFat.toFloat())
                _uiState.update { AddMealState() }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateDailySummary(
        userId: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ) {
        val todayDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val existingSummary = repository.getSummaryByDateDirect(userId, todayDate)

        if (existingSummary != null) {
            val updatedSummary = existingSummary.copy(
                totalCaloriesConsumed = existingSummary.totalCaloriesConsumed + calories,
                totalProtein = existingSummary.totalProtein + protein,
                totalCarbs = existingSummary.totalCarbs + carbs,
                totalFat = existingSummary.totalFat + fat,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            repository.insertSummary(updatedSummary)
        } else {
            val newSummary = DailySummaryEntity(
                userId = userId,
                date = todayDate,
                totalCaloriesConsumed = calories,
                totalProtein = protein,
                totalCarbs = carbs,
                totalFat = fat,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
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