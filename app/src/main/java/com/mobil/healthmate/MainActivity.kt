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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import com.mobil.healthmate.receiver.NotificationReceiver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var db: AppDatabase

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

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

        setupPeriodicSync()

        setContent {
            HealthMateTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val currentUser = FirebaseAuth.getInstance().currentUser

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

                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination!!
                            ) {

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

                                composable(route = Screen.CreateProfile.route) {
                                    CreateProfileScreen(
                                        onNavigateToHome = {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.CreateProfile.route) { inclusive = true }
                                            }
                                        }
                                    )
                                }

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

                                composable(route = Screen.Profile.route) {
                                    ProfileScreen(
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

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

    private fun checkPermissionsAndSchedule() {
        val permissionsToRequest = mutableListOf<String>()

        // Android 13+ Bildirim İzni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 10+ Fiziksel Aktivite İzni (Adım Sayar İçin Zorunlu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Tüm izinler varsa alarmı kur
            scheduleSmartAlarms()
        }
    }

    private fun scheduleSmartAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
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
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                        set(Calendar.SECOND, 0)

                        if (timeInMillis < System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    try {
                        alarmManager.setInexactRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

                // Test alarmlarını sildim, istersen ekleyebilirsin yine.
                setAlarm(15, 0, "MEAL_CHECK", 2001)
                setAlarm(20, 0, "STEP_CHECK", 3001)
                setAlarm(hour, minute, "SLEEP", 4001)
            }
        }
    }

    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofMinutes(15))
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}