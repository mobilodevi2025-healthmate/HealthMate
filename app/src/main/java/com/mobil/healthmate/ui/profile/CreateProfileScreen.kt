package com.mobil.healthmate.ui.profile

import android.app.TimePickerDialog // EKLENDİ
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccessTime // EKLENDİ
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
import java.util.Calendar // EKLENDİ

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    val profileImagePath by viewModel.profileImagePath.collectAsState()

    BackHandler {
        Toast.makeText(context, "Devam etmek için profil oluşturmalısınız.", Toast.LENGTH_SHORT).show()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onProfileImageSelected(uri)
        }
    }

    // --- STATE TANIMLAMALARI ---
    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var isGenderExpanded by remember { mutableStateOf(false) }

    var targetWeight by remember { mutableStateOf("") }
    var targetCalories by remember { mutableStateOf("") }
    var targetSteps by remember { mutableStateOf("") }
    var sleepTarget by remember { mutableStateOf("") }
    var bedTime by remember { mutableStateOf("") }

    // --- TIMEPICKER MANTIĞI ---
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            // Saati her zaman iki haneli formatta ayarlar (Örn: 9:5 yerine 09:05)
            bedTime = String.format("%02d:%02d", hour, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true // 24 saat formatı
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profil Oluştur") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- PROFIL RESMİ KISMI (AYNI) ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
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
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(4.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Düzenle", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // --- KİŞİSEL BİLGİLER (AYNI) ---
            Text("Kişisel Bilgiler", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))

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

            // Cinsiyet Seçimi (AYNI)
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
                ExposedDropdownMenu(expanded = isGenderExpanded, onDismissRequest = { isGenderExpanded = false }) {
                    Gender.entries.forEach { gender ->
                        DropdownMenuItem(text = { Text(gender.displayName) }, onClick = { selectedGender = gender; isGenderExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- HEDEFLER ---
            Text("Hedefleriniz", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = targetWeight, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) targetWeight = it },
                    label = { Text("Hedef Kilo") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = targetCalories, onValueChange = { if (it.all { c -> c.isDigit() }) targetCalories = it },
                    label = { Text("Günlük Kalori") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = targetSteps, onValueChange = { if (it.all { c -> c.isDigit() }) targetSteps = it },
                    label = { Text("Günlük Adım") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = sleepTarget, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) sleepTarget = it },
                    label = { Text("Uyku (Saat)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // --- DÜZELTİLEN YER: YATIŞ SAATİ (TIMEPICKER) ---
            OutlinedTextField(
                value = bedTime,
                onValueChange = {}, // Elle yazmayı engelliyoruz
                label = { Text("Yatış Saati") },
                placeholder = { Text("Seçmek için dokun") },
                readOnly = true, // Klavye açılmasın
                modifier = Modifier
                    .fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Default.AccessTime, contentDescription = "Saat Seç")
                },
                // Kullanıcı tıkladığında TimePicker açılır
                interactionSource = remember { MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    timePickerDialog.show()
                                }
                            }
                        }
                    }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank() || height.isBlank() || weight.isBlank() || age.isBlank() ||
                        targetWeight.isBlank() || targetCalories.isBlank() || targetSteps.isBlank() ||
                        sleepTarget.isBlank() || bedTime.isBlank()) {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.onEvent(ProfileEvent.SaveProfile(
                            name = name,
                            age = age,
                            height = height,
                            weight = weight,
                            gender = selectedGender,
                            activityLevel = com.mobil.healthmate.data.local.types.ActivityLevel.MODERATELY_ACTIVE,
                            targetWeight = targetWeight,
                            targetCalories = targetCalories,
                            dailyStepGoal = targetSteps,
                            sleepTargetHours = sleepTarget,
                            bedTime = bedTime
                        ))
                        onNavigateToHome()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Kaydet ve Başla")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}