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
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.ui.Screen
import com.mobil.healthmate.ui.add_meal.AddMealScreen
import com.mobil.healthmate.ui.auth.LoginScreen
import com.mobil.healthmate.ui.home.HomeScreen
import com.mobil.healthmate.ui.meal_list.MealListScreen
import com.mobil.healthmate.ui.profile.GoalsScreen
import com.mobil.healthmate.ui.profile.ProfileScreen
import com.mobil.healthmate.ui.theme.HealthMateTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                // 1. Kullanıcı Kontrolü (Firebase)
                // Kullanıcı null ise Login, değilse Home ekranı başlangıç noktası olur.
                val startDestination = if (FirebaseAuth.getInstance().currentUser == null) {
                    Screen.Login.route
                } else {
                    Screen.Home.route
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {

                            // --- 1. LOGIN EKRANI (YENİ EKLENDİ) ---
                            composable(route = Screen.Login.route) {
                                LoginScreen(
                                    onNavigateToHome = {
                                        // Başarılı girişte Home'a git ve geri tuşuyla Login'e dönülmesini engelle
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // --- 2. HOME EKRANI ---
                            composable(route = Screen.Home.route) {
                                HomeScreen(
                                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                                    onNavigateToMealList = { navController.navigate(Screen.MealList.route) },
                                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                                    onSignOut = {
                                        FirebaseAuth.getInstance().signOut()

                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                        val googleSignInClient = GoogleSignIn.getClient(this@MainActivity, gso)
                                        googleSignInClient.signOut()

                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // --- 3. DİĞER EKRANLAR (MEVCUT YAPI KORUNDU) ---

                            composable(route = Screen.AddMeal.route) {
                                AddMealScreen(onNavigateBack = { navController.popBackStack() })
                            }

                            composable(route = Screen.MealList.route) {
                                MealListScreen(onNavigateBack = { navController.popBackStack() })
                            }

                            composable(route = Screen.Profile.route) {
                                ProfileScreen(onNavigateBack = { navController.popBackStack() })
                            }

                            composable(route = Screen.Goals.route) {
                                GoalsScreen(onNavigateBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}