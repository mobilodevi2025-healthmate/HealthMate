package com.mobil.healthmate.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()

    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var isGenderExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState, isEditing) {
        if (!uiState.isLoading && (!isEditing || name.isBlank())) {
            name = uiState.name
            height = uiState.height
            weight = uiState.weight
            age = uiState.age
            selectedGender = uiState.gender
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Profili Düzenle" else "Kişisel Profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { viewModel.toggleEditMode() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Adınız") }, modifier = Modifier.fillMaxWidth(),
                readOnly = !isEditing, enabled = isEditing
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = height, onValueChange = { height = it },
                    label = { Text("Boy (cm)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    readOnly = !isEditing, enabled = isEditing
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it },
                    label = { Text("Kilo (kg)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    readOnly = !isEditing, enabled = isEditing
                )
            }

            OutlinedTextField(
                value = age, onValueChange = { age = it },
                label = { Text("Yaş") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                readOnly = !isEditing, enabled = isEditing
            )

            ExposedDropdownMenuBox(
                expanded = isGenderExpanded && isEditing,
                onExpandedChange = { if (isEditing) isGenderExpanded = !isGenderExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedGender.displayName, onValueChange = {},
                    readOnly = true, enabled = isEditing,
                    label = { Text("Cinsiyet") },
                    trailingIcon = { if (isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isGenderExpanded, onDismissRequest = { isGenderExpanded = false }
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

            // BUTONLAR
            if (isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.cancelEdit() },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text("Vazgeç") }

                    Button(
                        onClick = {
                            viewModel.onEvent(ProfileEvent.SaveProfile(
                                name = name, age = age, height = height, weight = weight, gender = selectedGender,
                                activityLevel = uiState.activityLevel,
                                targetWeight = uiState.targetWeight,
                                targetCalories = uiState.targetCalories,
                                dailyStepGoal = uiState.dailyStepGoal
                            ))
                            Toast.makeText(context, "Profil güncellendi!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text("Kaydet") }
                }
            }
        }
    }
}