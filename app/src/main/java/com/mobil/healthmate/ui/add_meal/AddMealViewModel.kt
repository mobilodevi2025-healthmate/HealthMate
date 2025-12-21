package com.mobil.healthmate.ui.add_meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.types.MealType // Enum Import
import com.mobil.healthmate.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
                _uiState.value = _uiState.value.copy(mealType = event.type)
            }
            is AddMealEvent.OnAddFood -> {
                val currentList = _uiState.value.addedFoods.toMutableList()
                currentList.add(event.food)
                _uiState.value = _uiState.value.copy(addedFoods = currentList)
            }
            is AddMealEvent.SaveMeal -> {
                saveMealToDb()
            }
        }
    }

    private fun saveMealToDb() {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value

        if (state.addedFoods.isEmpty()) return

        viewModelScope.launch {
            val newMeal = MealEntity(
                userId = uid,
                mealType = state.mealType,
                date = System.currentTimeMillis(),
                totalCalories = state.addedFoods.sumOf { it.calories }
            )

            repository.insertMealWithFoods(newMeal, state.addedFoods)
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