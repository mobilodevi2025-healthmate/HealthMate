package com.mobil.healthmate.ui

sealed class Screen(val route: String) {
    data object Login : Screen("login_screen")
    data object Home : Screen("home_screen")
    data object AddMeal : Screen("add_meal_screen")
    data object MealList : Screen("meal_list_screen")
    data object Profile : Screen("profile_screen")
    data object Goals : Screen("goals_screen")
}