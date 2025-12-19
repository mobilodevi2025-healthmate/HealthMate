package com.mobil.healthmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobil.healthmate.ui.HomeScreen
import com.mobil.healthmate.ui.Screen
import com.mobil.healthmate.ui.add_meal.AddMealScreen
import com.mobil.healthmate.ui.meal_list.MealListScreen
import com.mobil.healthmate.ui.profile.GoalsScreen
import com.mobil.healthmate.ui.profile.ProfileScreen
import com.mobil.healthmate.ui.profile.ProfileViewModel
import com.mobil.healthmate.ui.theme.HealthMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                // 1. ViewModel ve Veri Akışını Bağla
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val userData by profileViewModel.userFullData.collectAsState()

                // 2. Veri Yükleniyor Kontrolü (Null Check)
                if (userData == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // 3. Karar Mekanizması: İlk kez mi açılıyor?
                    // Evet (isFirstRun = true) -> Profil Ekranına git.
                    // Hayır -> Ana Menüye git.
                    val startDestination = if (userData!!.isFirstRun) Screen.Profile.route else Screen.Home.route

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val navController = rememberNavController()

                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination
                            ) {
                                // 1. Ana Menü
                                composable(route = Screen.Home.route) {
                                    HomeScreen(
                                        onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                                        onNavigateToMealList = { navController.navigate(Screen.MealList.route) },
                                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) }
                                    )
                                }

                                // 2. Yemek Ekleme
                                composable(route = Screen.AddMeal.route) {
                                    AddMealScreen(onNavigateBack = { navController.popBackStack() })
                                }

                                // 3. Yemek Listesi
                                composable(route = Screen.MealList.route) {
                                    MealListScreen(onNavigateBack = { navController.popBackStack() })
                                }

                                // 4. Profil Ekranı (ÖZEL MANTIK EKLENDİ)
                                composable(route = Screen.Profile.route) {
                                    ProfileScreen(
                                        onNavigateBack = {
                                            // Eğer burası başlangıç noktasıysa (Onboarding),
                                            // geri dönünce uygulamadan çıkmasın, Ana Menüye gitsin.
                                            if (startDestination == Screen.Profile.route) {
                                                navController.navigate(Screen.Home.route) {
                                                    // Geri tuşuna basınca tekrar kurulum ekranına dönmesin diye geçmişi siliyoruz.
                                                    popUpTo(Screen.Profile.route) { inclusive = true }
                                                }
                                            } else {
                                                // Normal ayarlardan geldiyse sadece geri dön.
                                                navController.popBackStack()
                                            }
                                        }
                                    )
                                }

                                // 5. Hedefler Ekranı
                                composable(route = Screen.Goals.route) {
                                    GoalsScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}