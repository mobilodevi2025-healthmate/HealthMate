package com.mobil.healthmate.ui.add_meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.types.MealType // Enum Import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mobil.healthmate.domain.repository.HealthRepository
import java.util.UUID
import kotlinx.coroutines.flow.update

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
            // 1. Önce Veritabanındaki Kullanıcıyı Bul (UUID sorununu çözen yer)
            val currentUser = repository.getCurrentUser()

            if (currentUser != null) {
                // Kullanıcı bulundu, ID'sini alıyoruz
                val userId = currentUser.userId
                val currentFoods = _uiState.value.addedFoods

                if (currentFoods.isNotEmpty()) {
                    // 2. Yemeği Oluştur (Doğru UserID ile)
                    val meal = MealEntity(
                        mealId = UUID.randomUUID().toString(),
                        userId = userId, // <-- ARTIK DOĞRU ID GİDİYOR
                        mealType = _uiState.value.mealType,
                        date = System.currentTimeMillis(),
                        totalCalories = currentFoods.sumOf { it.calories },
                        isSynced = false
                    )

                    // 3. Kaydet
                    repository.insertMealWithFoods(meal, currentFoods)

                    // 4. UI'ı Temizle
                    _uiState.update { AddMealState() }
                }
            } else {
                // Kullanıcı bulunamadı (Login olunmamış olabilir)
                // Buraya log veya hata mesajı eklenebilir
                println("HATA: Kullanıcı bulunamadı, yemek kaydedilemedi.")
            }
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