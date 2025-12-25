package com.mobil.healthmate.ui.profile

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentSteps by viewModel.currentSteps.collectAsState()
    val weeklySummaries by viewModel.weeklySummaries.collectAsState()
    val currentCalories by viewModel.currentCalories.collectAsState()

    var selectedCategory by remember { mutableStateOf(GoalCategory.STEPS) }

    // Input States
    var stepInput by remember { mutableStateOf("") }
    var calorieInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var sleepInput by remember { mutableStateOf("") }
    var bedTimeInput by remember { mutableStateOf("") }

    val pageCount = if (selectedCategory == GoalCategory.WEIGHT) 1 else 2
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(selectedCategory) {
        pagerState.scrollToPage(0)
    }

    LaunchedEffect(uiState) {
        if (!uiState.isLoading) {
            if (stepInput.isBlank()) stepInput = uiState.dailyStepGoal
            if (calorieInput.isBlank()) calorieInput = uiState.targetCalories
            if (weightInput.isBlank()) weightInput = uiState.targetWeight
            if (sleepInput.isBlank()) sleepInput = uiState.sleepTargetHours
            if (bedTimeInput.isBlank()) bedTimeInput = uiState.bedTime
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

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Başlık Mantığı
                    val titleText = when {
                        selectedCategory == GoalCategory.WEIGHT -> "Mevcut Kilo Durumu"
                        pagerState.currentPage == 0 -> "Bugünün Durumu"
                        else -> "Son 7 Günlük Performans"
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        if (page == 0) {
                            DailyCircularChart(
                                selectedCategory = selectedCategory,
                                currentSteps = currentSteps,
                                currentCalories = currentCalories,
                                uiState = uiState,
                                inputValues = Triple(stepInput, calorieInput, weightInput),
                                sleepInput = sleepInput
                            )
                        } else {
                            WeeklyBarChart(
                                weeklyData = weeklySummaries,
                                category = selectedCategory,
                                targetStr = when(selectedCategory) {
                                    GoalCategory.STEPS -> stepInput
                                    GoalCategory.CALORIES -> calorieInput
                                    GoalCategory.SLEEP -> sleepInput
                                    GoalCategory.WEIGHT -> weightInput
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (pageCount > 1) {
                        Row(
                            Modifier.wrapContentHeight().fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                                Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${selectedCategory.title} Hedefini Düzenle",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            CategoryInputFields(
                category = selectedCategory,
                stepInput = stepInput, onStepChange = { stepInput = it },
                calorieInput = calorieInput, onCalorieChange = { calorieInput = it },
                weightInput = weightInput, onWeightChange = { weightInput = it },
                sleepInput = sleepInput, onSleepChange = { sleepInput = it },
                bedTimeInput = bedTimeInput, onBedTimeChange = { bedTimeInput = it }
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
                        activityLevel = uiState.activityLevel,
                        targetWeight = weightInput,
                        targetCalories = calorieInput,
                        dailyStepGoal = stepInput,
                        sleepTargetHours = sleepInput,
                        bedTime = bedTimeInput
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
fun DailyCircularChart(
    selectedCategory: GoalCategory,
    currentSteps: Int,
    currentCalories: Int,
    uiState: com.mobil.healthmate.ui.profile.ProfileUiState,
    inputValues: Triple<String, String, String>,
    sleepInput: String
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {

        // Veri Hazırlığı
        val (currentValue, targetValueStr, displayUnit) = when (selectedCategory) {
            GoalCategory.STEPS -> Triple(currentSteps.toFloat(), inputValues.first, "Adım")
            GoalCategory.CALORIES -> Triple(currentCalories.toFloat(), inputValues.second, "Kcal")
            GoalCategory.WEIGHT -> Triple(uiState.weight.toFloatOrNull() ?: 0f, inputValues.third, "Kg")
            GoalCategory.SLEEP -> Triple(0f, sleepInput, "Saat")
        }

        val targetValue = targetValueStr.toFloatOrNull() ?: 1f

        var progress = 0f
        var progressColor = MaterialTheme.colorScheme.primary

        if (targetValue > 0) {
            if (selectedCategory == GoalCategory.WEIGHT) {
                if (currentValue > targetValue) {
                    progress = (targetValue / currentValue).coerceIn(0f, 1f)

                    progressColor = Color(0xFFEF6C00) // Orange800
                } else {
                    progress = (currentValue / targetValue).coerceIn(0f, 1f)
                    progressColor = MaterialTheme.colorScheme.primary
                }
            } else {
                progress = (currentValue / targetValue).coerceIn(0f, 1f)
                progressColor = MaterialTheme.colorScheme.primary
            }
        }

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
            color = progressColor,
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
                text = if (selectedCategory == GoalCategory.STEPS) currentValue.toInt().toString() else currentValue.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor // Metin rengini de duruma göre değiştirebiliriz
            )
            Text(
                text = "/ $targetValueStr $displayUnit Hedef",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            // Kilo için ek bilgi metni
            if (selectedCategory == GoalCategory.WEIGHT && targetValue > 0) {
                val diff = currentValue - targetValue
                val infoText = if (diff > 0) {
                    "${String.format("%.1f", diff)} kg fazlan var"
                } else if (diff < 0) {
                    "${String.format("%.1f", -diff)} kg alman lazım"
                } else {
                    "Hedefe ulaştın!"
                }

                Text(
                    text = infoText,
                    style = MaterialTheme.typography.labelSmall,
                    color = progressColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    weeklyData: List<DailySummaryEntity>,
    category: GoalCategory,
    targetStr: String
) {
    val targetValue = targetStr.toFloatOrNull() ?: 100f

    if (weeklyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Henüz geçmiş veri yok", color = Color.Gray)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyData.takeLast(7).forEach { summary ->
            val value = when(category) {
                GoalCategory.STEPS -> summary.totalSteps.toFloat()
                GoalCategory.CALORIES -> summary.totalCaloriesConsumed.toFloat()
                else -> summary.totalSteps.toFloat()
            }

            val maxInChart = maxOf(targetValue, weeklyData.maxOf {
                when(category) {
                    GoalCategory.STEPS -> it.totalSteps.toFloat()
                    GoalCategory.CALORIES -> it.totalCaloriesConsumed.toFloat()
                    else -> 1f
                }
            }) * 1.2f

            val barHeightRatio = (value / maxInChart).coerceIn(0.05f, 1f)
            val date = Date(summary.date)
            val dayName = SimpleDateFormat("EEE", Locale("tr")).format(date)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight(barHeightRatio)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (value >= targetValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dayName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun CategoryInputFields(
    category: GoalCategory,
    stepInput: String, onStepChange: (String) -> Unit,
    calorieInput: String, onCalorieChange: (String) -> Unit,
    weightInput: String, onWeightChange: (String) -> Unit,
    sleepInput: String, onSleepChange: (String) -> Unit,
    bedTimeInput: String, onBedTimeChange: (String) -> Unit
) {
    val context = LocalContext.current

    when (category) {
        GoalCategory.STEPS -> {
            OutlinedTextField(
                value = stepInput,
                onValueChange = { if (it.all { c -> c.isDigit() }) onStepChange(it) },
                label = { Text("Günlük Adım") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Icon(Icons.Default.DirectionsWalk, null) }
            )
        }
        GoalCategory.CALORIES -> {
            OutlinedTextField(
                value = calorieInput,
                onValueChange = { if (it.all { c -> c.isDigit() }) onCalorieChange(it) },
                label = { Text("Günlük Kalori") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Icon(Icons.Default.LocalFireDepartment, null) }
            )
        }
        GoalCategory.WEIGHT -> {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onWeightChange(it) },
                label = { Text("Hedef Kilo") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Icon(Icons.Default.MonitorWeight, null) }
            )
        }
        GoalCategory.SLEEP -> {
            OutlinedTextField(
                value = sleepInput,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onSleepChange(it) },
                label = { Text("Günlük Uyku Hedefi (Saat)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Icon(Icons.Default.Bedtime, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val calendar = Calendar.getInstance()
            val currentHour = bedTimeInput.split(":").getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = bedTimeInput.split(":").getOrNull(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                context,
                { _, hour: Int, minute: Int ->
                    onBedTimeChange(String.format("%02d:%02d", hour, minute))
                },
                currentHour,
                currentMinute,
                true
            )

            OutlinedTextField(
                value = bedTimeInput,
                onValueChange = {},
                readOnly = true,
                label = { Text("Yatış Saati") },
                placeholder = { Text("Seçmek için dokun") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.AccessTime, contentDescription = "Saat Seç") },
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
    WEIGHT("Kilo", "kg", Icons.Default.MonitorWeight),
    SLEEP("Uyku", "saat", Icons.Default.Bedtime)
}