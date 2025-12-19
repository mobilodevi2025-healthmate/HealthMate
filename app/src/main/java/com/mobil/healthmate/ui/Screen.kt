package com.mobil.healthmate.ui

sealed class Screen(val route: String) {
    data object Home : Screen("home_screen")
    data object AddMeal : Screen("add_meal_screen")
    data object MealList : Screen("meal_list_screen")
}