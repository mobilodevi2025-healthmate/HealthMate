package com.mobil.healthmate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.AppDatabase
import com.mobil.healthmate.ui.Screen
import com.mobil.healthmate.ui.add_meal.AddMealScreen
import com.mobil.healthmate.ui.auth.LoginScreen
import com.mobil.healthmate.ui.home.HomeScreen
import com.mobil.healthmate.ui.meal_list.MealListScreen
import com.mobil.healthmate.ui.profile.CreateProfileScreen
import com.mobil.healthmate.ui.profile.GoalsScreen
import com.mobil.healthmate.ui.profile.ProfileScreen
import com.mobil.healthmate.ui.theme.HealthMateTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val currentUser = FirebaseAuth.getInstance().currentUser

                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    if (currentUser == null) {
                        startDestination = Screen.Login.route
                    } else {
                        startDestination = Screen.Home.route
                    }
                }

                if (startDestination != null) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination!!
                            ) {

                                // --- 1. LOGIN ---
                                composable(route = Screen.Login.route) {
                                    LoginScreen(
                                        onNavigateToHome = {
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            if (uid != null) {
                                                Log.d("LOGIN_DEBUG", "Kullanıcı UID: $uid. Firestore kontrolü başlıyor...")

                                                val firestore = FirebaseFirestore.getInstance()
                                                firestore.collection("users").document(uid).get()
                                                    .addOnSuccessListener { document ->
                                                        if (document.exists()) {
                                                            Log.d("LOGIN_DEBUG", "Firestore: Kullanıcı VAR. Home'a gidiliyor.")
                                                            navController.navigate(Screen.Home.route) {
                                                                popUpTo(Screen.Login.route) { inclusive = true }
                                                            }
                                                        } else {
                                                            Log.d("LOGIN_DEBUG", "Firestore: Kullanıcı YOK. CreateProfile'a gidiliyor.")
                                                            navController.navigate(Screen.CreateProfile.route) {
                                                                popUpTo(Screen.Login.route) { inclusive = true }
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        // Hata mesajını loga bas
                                                        Log.e("LOGIN_DEBUG", "Firestore Hatası: ${e.localizedMessage}")

                                                        // Hata olsa bile kullanıcıyı içeri alalım (Offline mod veya hata toleransı)
                                                        Toast.makeText(context, "Profil kontrol edilemedi, ana sayfaya geçiliyor.", Toast.LENGTH_LONG).show()
                                                        navController.navigate(Screen.Home.route) {
                                                            popUpTo(Screen.Login.route) { inclusive = true }
                                                        }
                                                    }
                                            } else {
                                                Log.e("LOGIN_DEBUG", "UID null geldi!")
                                            }
                                        },
                                        onNavigateToCreateProfile = {
                                            navController.navigate(Screen.CreateProfile.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                // --- 2. CREATE PROFILE ---
                                composable(route = Screen.CreateProfile.route) {
                                    CreateProfileScreen(
                                        onNavigateToHome = {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.CreateProfile.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                // --- 3. HOME ---
                                composable(route = Screen.Home.route) {
                                    HomeScreen(
                                        onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                                        onNavigateToMealList = { navController.navigate(Screen.MealList.route) },
                                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                                        onSignOut = {
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(Screen.Home.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                // --- 4. PROFILE (SADECE OKUMA/DÜZENLEME) ---
                                composable(route = Screen.Profile.route) {
                                    ProfileScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                // Diğer ekranlar
                                composable(route = Screen.AddMeal.route) {
                                    AddMealScreen(onNavigateBack = { navController.popBackStack() })
                                }
                                composable(route = Screen.MealList.route) {
                                    MealListScreen(onNavigateBack = { navController.popBackStack() })
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
}