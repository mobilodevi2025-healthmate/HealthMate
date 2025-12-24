package com.mobil.healthmate.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mobil.healthmate.data.local.types.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    // ViewModel'den resim yolunu dinliyoruz
    val profileImagePath by viewModel.profileImagePath.collectAsState()

    // Geri tuşunu engelle (Zorunlu alan)
    BackHandler {
        Toast.makeText(context, "Devam etmek için profil oluşturmalısınız.", Toast.LENGTH_SHORT).show()
    }

    // --- FOTOĞRAF SEÇİCİ (LAUNCHER) ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onProfileImageSelected(uri)
        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Ortalamak için eklendi
        ) {

            // --- 1. PROFIL RESMİ ALANI (YENİ EKLENDİ) ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        // Tıklanınca galeriyi aç
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profileImagePath != null) {
                    // Resim seçildiyse göster
                    AsyncImage(
                        model = profileImagePath,
                        contentDescription = "Profil Resmi",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Seçilmediyse varsayılan ikon
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Küçük kalem ikonu (Edit Badge)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = Color.White
                    )
                }
            }

            Text(
                "Fotoğraf eklemek için tıklayın",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            // --- MEVCUT FORM ALANLARI ---

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