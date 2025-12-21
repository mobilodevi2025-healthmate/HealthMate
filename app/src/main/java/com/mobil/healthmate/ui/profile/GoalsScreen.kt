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
import com.mobil.healthmate.data.local.types.ActivityLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var stepGoal by remember { mutableStateOf("") }
    var calorieGoal by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf(ActivityLevel.MODERATELY_ACTIVE) }

    var isActivityExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (!uiState.isLoading) {
            if (stepGoal.isBlank()) stepGoal = uiState.dailyStepGoal
            if (calorieGoal.isBlank()) calorieGoal = uiState.targetCalories
            if (targetWeight.isBlank()) targetWeight = uiState.targetWeight
            selectedActivity = uiState.activityLevel
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

            ExposedDropdownMenuBox(
                expanded = isActivityExpanded,
                onExpandedChange = { isActivityExpanded = !isActivityExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedActivity.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Aktivite Seviyesi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isActivityExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isActivityExpanded,
                    onDismissRequest = { isActivityExpanded = false }
                ) {
                    ActivityLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(level.displayName, style = MaterialTheme.typography.bodyLarge)
                                    // Katsayıyı küçük bilgi olarak gösterelim
                                    Text("x${level.factor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            },
                            onClick = {
                                selectedActivity = level
                                isActivityExpanded = false
                            }
                        )
                    }
                }
            }

            Divider()

            Text("Sayısal Hedefler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = targetWeight,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) targetWeight = it },
                label = { Text("Hedef Kilo (kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

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
                    viewModel.onEvent(ProfileEvent.SaveProfile(
                        name = uiState.name,
                        age = uiState.age,
                        height = uiState.height,
                        weight = uiState.weight,
                        gender = uiState.gender,

                        activityLevel = selectedActivity,
                        targetWeight = targetWeight,
                        targetCalories = calorieGoal,
                        dailyStepGoal = stepGoal
                    ))

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