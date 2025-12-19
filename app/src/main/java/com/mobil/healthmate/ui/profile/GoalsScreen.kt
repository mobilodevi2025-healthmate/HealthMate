package com.mobil.healthmate.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val fullData by viewModel.userFullData.collectAsState()

    var stepGoal by remember { mutableStateOf("") }
    var calorieGoal by remember { mutableStateOf("") }
    var goalType by remember { mutableStateOf("Kilo Koru") }
    var activityLevel by remember { mutableStateOf("Orta") }

    LaunchedEffect(fullData) {
        fullData?.goals?.let {
            stepGoal = it.stepGoal.toString()
            calorieGoal = it.calorieGoal.toString()
            goalType = it.goalType
            activityLevel = it.activityLevel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hedeflerim") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Günlük Hedefleriniz", style = MaterialTheme.typography.titleMedium)

            // Hedef Tipi
            OutlinedTextField(
                value = goalType,
                onValueChange = { goalType = it },
                label = { Text("Hedef (Kilo Ver/Al/Koru)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Aktivite
            OutlinedTextField(
                value = activityLevel,
                onValueChange = { activityLevel = it },
                label = { Text("Aktivite (Düşük/Orta/Yüksek)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            Text("Sayısal Hedefler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = stepGoal,
                onValueChange = { if (it.all { c -> c.isDigit() }) stepGoal = it },
                label = { Text("Günlük Adım Hedefi") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = calorieGoal,
                onValueChange = { if (it.all { c -> c.isDigit() }) calorieGoal = it },
                label = { Text("Günlük Kalori Limiti") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveStrategyGoals(goalType, activityLevel, stepGoal, calorieGoal)
                    Toast.makeText(context, "Hedefler güncellendi!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Hedefleri Güncelle")
            }
        }
    }
}