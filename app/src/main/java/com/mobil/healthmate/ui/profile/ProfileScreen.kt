package com.mobil.healthmate.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
    val profileImagePath by viewModel.profileImagePath.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onProfileImageSelected(uri)
        }
    }

    // Form States
    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var isGenderExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (isEditing) {
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
                title = { Text(if (isEditing) "Profili Düzenle" else "Profilim") },
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
                        IconButton(onClick = { viewModel.onEvent(ProfileEvent.SignOut) }) {
                            Icon(Icons.Default.Logout, contentDescription = "Çıkış")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = isEditing) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileImagePath != null) {
                    AsyncImage(
                        model = profileImagePath,
                        contentDescription = "Profil Resmi",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Değiştir",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad Soyad") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) height = it },
                            label = { Text("Boy (cm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) weight = it },
                            label = { Text("Kilo (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = age,
                        onValueChange = { if (it.all { c -> c.isDigit() }) age = it },
                        label = { Text("Yaş") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = isGenderExpanded,
                        onExpandedChange = { isGenderExpanded = !isGenderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedGender.displayName,
                            onValueChange = {},
                            readOnly = true,
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
                                    onClick = {
                                        selectedGender = gender
                                        isGenderExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { viewModel.cancelEdit() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("İptal")
                        }
                        Button(
                            onClick = {
                                viewModel.onEvent(ProfileEvent.SaveProfile(
                                    name = name,
                                    age = age,
                                    height = height,
                                    weight = weight,
                                    gender = selectedGender,
                                    activityLevel = uiState.activityLevel,
                                    targetWeight = uiState.targetWeight,
                                    targetCalories = uiState.targetCalories,
                                    dailyStepGoal = uiState.dailyStepGoal,
                                    sleepTargetHours = uiState.sleepTargetHours,
                                    bedTime = uiState.bedTime
                                ))
                                Toast.makeText(context, "Profil güncellendi", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kaydet")
                        }
                    }
                }

            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileInfoItem("Ad Soyad", uiState.name)
                    ProfileInfoItem("E-posta", uiState.email)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) { ProfileInfoItem("Boy", "${uiState.height} cm") }
                        Box(modifier = Modifier.weight(1f)) { ProfileInfoItem("Kilo", "${uiState.weight} kg") }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) { ProfileInfoItem("Yaş", uiState.age) }
                        Box(modifier = Modifier.weight(1f)) { ProfileInfoItem("Cinsiyet", uiState.gender.displayName) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value.ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}