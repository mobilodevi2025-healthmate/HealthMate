package com.mobil.healthmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobil.healthmate.ui.HomeScreen
import com.mobil.healthmate.ui.Screen
import com.mobil.healthmate.ui.add_meal.AddMealScreen
import com.mobil.healthmate.ui.meal_list.MealListScreen
import com.mobil.healthmate.ui.theme.HealthMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route
                        ) {
                            // Ana Menü
                            composable(route = Screen.Home.route) {
                                HomeScreen(
                                    onNavigateToAddMeal = {
                                        navController.navigate(Screen.AddMeal.route)
                                    },
                                    onNavigateToMealList = {
                                        navController.navigate(Screen.MealList.route)
                                    }
                                )
                            }

                            // Yemek Ekleme Ekranı
                            composable(route = Screen.AddMeal.route) {
                                AddMealScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // Yemek Listesi Ekranı
                            composable(route = Screen.MealList.route) {
                                MealListScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}