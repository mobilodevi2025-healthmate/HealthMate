package com.mobil.healthmate.ui.meal_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.relation.MealWithFoods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mobil.healthmate.domain.repository.HealthRepository


@HiltViewModel
class MealListViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _meals = MutableStateFlow<List<MealWithFoods>>(emptyList())
    val meals = _meals.asStateFlow()

    init {
        loadMeals()
    }

    private fun loadMeals() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.getMealsWithFoods(uid).collect { mealList ->
                _meals.value = mealList
            }
        }
    }

    fun deleteMeal(mealWithFoods: MealWithFoods) {
        viewModelScope.launch {
            repository.deleteMeal(mealWithFoods.meal)
        }
    }
}