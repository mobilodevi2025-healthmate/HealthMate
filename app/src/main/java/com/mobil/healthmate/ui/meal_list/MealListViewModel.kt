package com.mobil.healthmate.ui.meal_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.data.local.MealDao
import com.mobil.healthmate.data.local.MealEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MealListViewModel @Inject constructor(
    private val mealDao: MealDao
) : ViewModel() {
    val meals: StateFlow<List<MealEntity>> = mealDao.getAllMeals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}