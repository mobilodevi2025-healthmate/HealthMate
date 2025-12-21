package com.mobil.healthmate.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobil.healthmate.data.local.types.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        Toast.makeText(context, "Devam etmek için profil oluşturmalısınız.", Toast.LENGTH_SHORT).show()
    }

    // Form State
    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var isGenderExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profil Oluştur") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "HealthMate'e hoş geldiniz! Size özel bir plan oluşturabilmemiz için bilgilerinizi giriniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Adınız") }, modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = height, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) height = it },
                    label = { Text("Boy (cm)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = weight, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) weight = it },
                    label = { Text("Kilo (kg)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = age, onValueChange = { if (it.all { c -> c.isDigit() }) age = it },
                label = { Text("Yaş") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Cinsiyet Seçimi
            ExposedDropdownMenuBox(
                expanded = isGenderExpanded,
                onExpandedChange = { isGenderExpanded = !isGenderExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedGender.displayName, onValueChange = {}, readOnly = true,
                    label = { Text("Cinsiyet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isGenderExpanded,
                    onDismissRequest = { isGenderExpanded = false }
                ) {
                    Gender.entries.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.displayName) },
                            onClick = { selectedGender = gender; isGenderExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank() || height.isBlank() || weight.isBlank() || age.isBlank()) {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.onEvent(ProfileEvent.SaveProfile(
                            name = name, age = age, height = height, weight = weight, gender = selectedGender,
                            activityLevel = com.mobil.healthmate.data.local.types.ActivityLevel.MODERATELY_ACTIVE,
                            targetWeight = weight,
                            targetCalories = "2000",
                            dailyStepGoal = "10000"
                        ))
                        onNavigateToHome()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Kaydet ve Başla")
            }
        }
    }
}