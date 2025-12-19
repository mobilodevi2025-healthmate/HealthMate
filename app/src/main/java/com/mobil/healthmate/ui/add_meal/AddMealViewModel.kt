package com.mobil.healthmate.ui.add_meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.data.local.MealDao
import com.mobil.healthmate.data.local.MealEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealDao: MealDao
) : ViewModel() {

    fun addMeal(name: String, calorie: String, protein: String) {
        if (name.isBlank() || calorie.isBlank() || protein.isBlank()) return

        viewModelScope.launch {
            val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)

            val meal = MealEntity(
                name = name,
                calorie = calorie.toIntOrNull() ?: 0,
                protein = protein.toIntOrNull() ?: 0,
                date = currentDateTime,
                isSynced = false
            )

            mealDao.insertMeal(meal)
        }
    }
}