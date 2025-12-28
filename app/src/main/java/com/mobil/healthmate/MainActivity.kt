package com.mobil.healthmate

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.data.local.AppDatabase
import com.mobil.healthmate.domain.manager.ConnectivityObserverStatus
import com.mobil.healthmate.domain.repository.SyncRepository
import com.mobil.healthmate.receiver.NotificationReceiver
import com.mobil.healthmate.ui.MainViewModel
import com.mobil.healthmate.ui.Screen
import com.mobil.healthmate.ui.add_meal.AddMealScreen
import com.mobil.healthmate.ui.ai_advice.AiAdviceScreen
import com.mobil.healthmate.ui.auth.LoginScreen
import com.mobil.healthmate.ui.home.HomeScreen
import com.mobil.healthmate.ui.meal_list.MealListScreen
import com.mobil.healthmate.ui.profile.CreateProfileScreen
import com.mobil.healthmate.ui.profile.GoalsScreen
import com.mobil.healthmate.ui.profile.ProfileScreen
import com.mobil.healthmate.ui.theme.HealthMateTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var syncRepository: SyncRepository

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else { true }

        val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false

        if (notificationGranted) {
            scheduleSmartAlarms()
        } else {
            Toast.makeText(this, "Bildirim izni verilmedi.", Toast.LENGTH_SHORT).show()
        }

        if (!activityGranted) {
            Toast.makeText(this, "Adım sayar için fiziksel aktivite izni gerekli.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermissionsAndSchedule()
        syncRepository.setupPeriodicSync()

        setContent {
            HealthMateTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val currentUser = FirebaseAuth.getInstance().currentUser
                val status by mainViewModel.networkStatus.collectAsState()

                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    startDestination = if (currentUser == null) Screen.Login.route else Screen.Home.route
                }

                if (currentUser != null) {
                    val userExists by db.userDao().observeUserExists(currentUser.uid).collectAsState(initial = false)
                    LaunchedEffect(userExists) {
                        if (userExists) {
                            scheduleSmartAlarms()
                        }
                    }
                }

                if (startDestination != null) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            AnimatedVisibility(
                                visible = status == ConnectivityObserverStatus.Unavailable || status == ConnectivityObserverStatus.Lost,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().background(Color.Red).padding(8.dp),
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
                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(navController = navController, startDestination = startDestination!!) {

                                // --- 1. LOGIN EKRANI (GÜNCELLENDİ) ---
                                composable(route = Screen.Login.route) {
                                    LoginScreen(
                                        onNavigateToHome = {
                                            // AuthViewModel verileri indirdi ve "Kullanıcı Var" dedi.
                                            // Direkt Home'a gidiyoruz.
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        },
                                        onNavigateToCreateProfile = {
                                            // AuthViewModel verileri indirdi ama "Profil Yok" dedi.
                                            navController.navigate(Screen.CreateProfile.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                composable(route = Screen.CreateProfile.route) {
                                    CreateProfileScreen(onNavigateToHome = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.CreateProfile.route) { inclusive = true }
                                        }
                                    })
                                }

                                composable(route = Screen.Home.route) {
                                    HomeScreen(
                                        onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                                        onNavigateToMealList = { navController.navigate(Screen.MealList.route) },
                                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                                        onNavigateToAi = { navController.navigate("ai_advice") },
                                        onSignOut = {
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(Screen.Home.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable(route = Screen.Profile.route) { ProfileScreen(onNavigateBack = { navController.popBackStack() }) }
                                composable(route = Screen.AddMeal.route) { AddMealScreen(onNavigateBack = { navController.popBackStack() }) }
                                composable(route = Screen.MealList.route) { MealListScreen(onNavigateBack = { navController.popBackStack() }) }
                                composable(route = Screen.Goals.route) { GoalsScreen(onNavigateBack = { navController.popBackStack() }) }
                                composable("ai_advice") {
                                    AiAdviceScreen(onNavigateBack = { navController.popBackStack() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndSchedule() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            scheduleSmartAlarms()
        }
    }

    private fun scheduleSmartAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val user = db.userDao().getUserDirect(uid)
            if (user == null) return@launch
            val goal = db.goalDao().getCurrentGoal(uid)
            val bedTime = goal?.bedTime ?: "23:00"
            val parts = bedTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            withContext(Dispatchers.Main) {
                fun setAlarm(h: Int, m: Int, type: String, reqCode: Int) {
                    val intent = Intent(this@MainActivity, NotificationReceiver::class.java).apply {
                        putExtra("TYPE", type)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        this@MainActivity, reqCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
                        if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                    }
                    try {
                        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
                    } catch (e: Exception) { e.printStackTrace() }
                }

                setAlarm(11, 0, "WATER", 1001)
                setAlarm(12, 0, "WATER", 1002)
                setAlarm(13, 0, "WATER", 1003)
                setAlarm(14, 0, "WATER", 1004)
                setAlarm(15, 0, "WATER", 1005)
                setAlarm(16, 0, "WATER", 1006)
                setAlarm(17, 0, "WATER", 1007)
                setAlarm(18, 0, "WATER", 1008)
                setAlarm(19, 0, "WATER", 1009)

                setAlarm(15, 0, "MEAL_CHECK", 2001)
                setAlarm(20, 0, "STEP_CHECK", 3001)
                setAlarm(hour, minute, "SLEEP", 4001)
            }
        }
    }
}