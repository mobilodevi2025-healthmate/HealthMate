package com.mobil.healthmate.ui.profile

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobil.healthmate.data.local.types.ActivityLevel
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentSteps by viewModel.currentSteps.collectAsState()

    var selectedCategory by remember { mutableStateOf(GoalCategory.STEPS) }

    var stepInput by remember { mutableStateOf("") }
    var calorieInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (!uiState.isLoading) {
            if (stepInput.isBlank()) stepInput = uiState.dailyStepGoal
            if (calorieInput.isBlank()) calorieInput = uiState.targetCalories
            if (weightInput.isBlank()) weightInput = uiState.targetWeight
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
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Hedef Türünü Seçin",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(GoalCategory.values()) { category ->
                    GoalSelectionCard(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(contentAlignment = Alignment.Center) {
                val (currentValue, targetValueStr, displayUnit) = when (selectedCategory) {
                    GoalCategory.STEPS -> Triple(currentSteps.toFloat(), stepInput, "Adım")
                    GoalCategory.CALORIES -> Triple(0f, calorieInput, "Kcal") // Kalori takibi henüz yoksa 0
                    GoalCategory.WEIGHT -> Triple(uiState.weight.toFloatOrNull() ?: 0f, weightInput, "Kg")
                }

                val targetValue = targetValueStr.toFloatOrNull() ?: 1f
                val progress = if(targetValue > 0) (currentValue / targetValue).coerceIn(0f, 1f) else 0f

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(1000),
                    label = "ProgressAnimation"
                )

                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(200.dp),
                    color = Color.LightGray.copy(alpha = 0.3f),
                    strokeWidth = 12.dp,
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(200.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedCategory == GoalCategory.WEIGHT) "Mevcut" else "Bugün",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = currentValue.toInt().toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ $targetValueStr Hedef",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "${selectedCategory.title} Hedefini Düzenle",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedCategory) {
                GoalCategory.STEPS -> {
                    OutlinedTextField(
                        value = stepInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) stepInput = it },
                        label = { Text("Günlük Adım") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Icon(Icons.Default.DirectionsWalk, null) }
                    )
                }
                GoalCategory.CALORIES -> {
                    OutlinedTextField(
                        value = calorieInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) calorieInput = it },
                        label = { Text("Günlük Kalori") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Icon(Icons.Default.LocalFireDepartment, null) }
                    )
                }
                GoalCategory.WEIGHT -> {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) weightInput = it },
                        label = { Text("Hedef Kilo") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Icon(Icons.Default.MonitorWeight, null) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.onEvent(ProfileEvent.SaveProfile(
                        name = uiState.name,
                        age = uiState.age,
                        height = uiState.height,
                        weight = uiState.weight,
                        gender = uiState.gender,
                        activityLevel = uiState.activityLevel, // Aktiviteyi değiştirmiyoruz burada

                        targetWeight = weightInput,
                        targetCalories = calorieInput,
                        dailyStepGoal = stepInput
                    ))

                    Toast.makeText(context, "${selectedCategory.title} hedefi güncellendi!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Hedefi Kaydet")
            }
        }
    }
}

@Composable
fun GoalSelectionCard(
    category: GoalCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

enum class GoalCategory(val title: String, val unit: String, val icon: ImageVector) {
    STEPS("Adım", "adım", Icons.Default.DirectionsWalk),
    CALORIES("Kalori", "kcal", Icons.Default.LocalFireDepartment),
    WEIGHT("Kilo", "kg", Icons.Default.MonitorWeight)
}