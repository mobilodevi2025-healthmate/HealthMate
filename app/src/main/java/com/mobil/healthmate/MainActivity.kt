package com.mobil.healthmate

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.AppDatabase
import com.mobil.healthmate.data.worker.SyncWorker
import com.mobil.healthmate.domain.manager.ConnectivityObserverStatus
import com.mobil.healthmate.ui.MainViewModel
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

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

                // 2. İNTERNET DURUMUNU DİNLE
                val status by mainViewModel.networkStatus.collectAsState()

                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    if (currentUser == null) {
                        startDestination = Screen.Login.route
                    } else {
                        startDestination = Screen.Home.route
                    }
                }

                if (startDestination != null) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        // 3. OFFLINE UYARISINI BURAYA EKLİYORUZ (ALT KISIM)
                        bottomBar = {
                            AnimatedVisibility(
                                visible = status == ConnectivityObserverStatus.Unavailable || status == ConnectivityObserverStatus.Lost,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Red)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Offline Moddasınız - Veriler cihazda saklanıyor",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->

                        // İÇERİK (NAVIGASYON)
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
                                                val firestore = FirebaseFirestore.getInstance()
                                                firestore.collection("users").document(uid).get()
                                                    .addOnSuccessListener { document ->
                                                        if (document.exists()) {
                                                            navController.navigate(Screen.Home.route) {
                                                                popUpTo(Screen.Login.route) { inclusive = true }
                                                            }
                                                        } else {
                                                            navController.navigate(Screen.CreateProfile.route) {
                                                                popUpTo(Screen.Login.route) { inclusive = true }
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        // Hata olsa bile (internet yoksa) Home'a alalım
                                                        Toast.makeText(context, "Offline giriş yapılıyor...", Toast.LENGTH_SHORT).show()
                                                        navController.navigate(Screen.Home.route) {
                                                            popUpTo(Screen.Login.route) { inclusive = true }
                                                        }
                                                    }
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

                                // --- 4. PROFILE ---
                                composable(route = Screen.Profile.route) {
                                    ProfileScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                // --- 5. ADD MEAL ---
                                composable(route = Screen.AddMeal.route) {
                                    AddMealScreen(onNavigateBack = { navController.popBackStack() })
                                }

                                // --- 6. MEAL LIST ---
                                composable(route = Screen.MealList.route) {
                                    MealListScreen(onNavigateBack = { navController.popBackStack() })
                                }

                                // --- 7. GOALS ---
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