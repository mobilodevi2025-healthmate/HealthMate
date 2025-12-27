package com.mobil.healthmate.ui.profile

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import java.util.Date
import java.util.Locale

enum class MacroType(val title: String, val color: Color) {
    PROTEIN("Protein", Color(0xFF4CAF50)),
    CARBS("Karb", Color(0xFF2196F3)),
    FAT("Yağ", Color(0xFFFFC107))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentSteps by viewModel.currentSteps.collectAsState()

    var selectedCategory by remember { mutableStateOf(GoalCategory.STEPS) }
    var selectedMacro by remember { mutableStateOf(MacroType.PROTEIN) }

    var stepInput by remember { mutableStateOf("") }
    var calorieInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    val pageCount = when (selectedCategory) {
        GoalCategory.WEIGHT -> 1
        GoalCategory.CALORIES -> 3
        else -> 2
    }

    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(selectedCategory) {
        pagerState.scrollToPage(0)
    }

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
                title = { Text("Hedeflerim ve Analiz") },
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
                    .height(360.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    val titleText = when {
                        selectedCategory == GoalCategory.WEIGHT -> "Mevcut Kilo Durumu"
                        pagerState.currentPage == 0 -> "Bugünün Durumu"
                        pagerState.currentPage == 1 -> "Haftalık Performans"
                        pagerState.currentPage == 2 -> "Haftalık Makro Analizi"
                        else -> "İstatistikler"
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
                        when (page) {
                            0 -> DailyCircularChart(
                                selectedCategory = selectedCategory,
                                currentSteps = currentSteps,
                                currentCalories = uiState.currentCalories,
                                uiState = uiState,
                                inputValues = Triple(stepInput, calorieInput, weightInput)
                            )
                            1 -> WeeklyBarChart(
                                weeklyData = uiState.weeklySummaries,
                                category = selectedCategory,
                                targetStr = if (selectedCategory == GoalCategory.STEPS) stepInput else calorieInput
                            )
                            2 -> if (selectedCategory == GoalCategory.CALORIES) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        MacroType.values().forEach { macro ->
                                            MacroTabButton(
                                                macro = macro,
                                                isSelected = selectedMacro == macro,
                                                onClick = { selectedMacro = macro }
                                            )
                                        }
                                    }
                                    WeeklyMacroChart(
                                        weeklyData = uiState.weeklyMacros,
                                        selectedMacro = selectedMacro
                                    )
                                }
                            }
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
                weightInput = weightInput, onWeightChange = { weightInput = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.onEvent(ProfileEvent.SaveProfile(
                        name = uiState.name, age = uiState.age, height = uiState.height, weight = uiState.weight,
                        gender = uiState.gender, activityLevel = uiState.activityLevel, targetWeight = weightInput,
                        targetCalories = calorieInput, dailyStepGoal = stepInput, sleepTargetHours = uiState.sleepTargetHours,
                        bedTime = uiState.bedTime
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
fun WeeklyMacroChart(
    weeklyData: List<DailyMacroStats>,
    selectedMacro: MacroType
) {
    if (weeklyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Veri yok", color = Color.Gray)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyData.forEach { stat ->
            val value = when(selectedMacro) {
                MacroType.PROTEIN -> stat.totalProtein
                MacroType.CARBS -> stat.totalCarbs
                MacroType.FAT -> stat.totalFat
            }

            val maxInChart = weeklyData.maxOf {
                when(selectedMacro) {
                    MacroType.PROTEIN -> it.totalProtein
                    MacroType.CARBS -> it.totalCarbs
                    MacroType.FAT -> it.totalFat
                }
            }.coerceAtLeast(50f) * 1.2f

            val barHeightRatio = (value / maxInChart).coerceIn(0.02f, 1f)
            val dayName = SimpleDateFormat("EEE", Locale("tr")).format(Date(stat.date))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = "${value.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = selectedMacro.color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight(barHeightRatio)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(selectedMacro.color)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dayName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Gray)
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
    val targetValue = targetStr.toFloatOrNull() ?: 2000f

    if (weeklyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Geçmiş veri yok", color = Color.Gray)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyData.forEach { summary ->
            val value = if (category == GoalCategory.STEPS) summary.totalSteps.toFloat() else summary.totalCaloriesConsumed.toFloat()

            val maxInChart = maxOf(targetValue, weeklyData.maxOf {
                if (category == GoalCategory.STEPS) it.totalSteps.toFloat() else it.totalCaloriesConsumed.toFloat()
            }.coerceAtLeast(1f)) * 1.2f

            val barHeightRatio = (value / maxInChart).coerceIn(0.05f, 1f)
            val dayName = SimpleDateFormat("EEE", Locale("tr")).format(Date(summary.date))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
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
fun MacroTabButton(macro: MacroType, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) macro.color.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (isSelected) macro.color else Color.LightGray, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = macro.title, style = MaterialTheme.typography.labelSmall, color = if (isSelected) macro.color else Color.Gray)
    }
}

@Composable
fun DailyCircularChart(
    selectedCategory: GoalCategory, currentSteps: Int, currentCalories: Int,
    uiState: ProfileUiState, inputValues: Triple<String, String, String>
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        val (currentValue, targetValueStr, displayUnit) = when (selectedCategory) {
            GoalCategory.STEPS -> Triple(currentSteps.toFloat(), inputValues.first, "Adım")
            GoalCategory.CALORIES -> Triple(currentCalories.toFloat(), inputValues.second, "Kcal")
            GoalCategory.WEIGHT -> Triple(uiState.weight.toFloatOrNull() ?: 0f, inputValues.third, "Kg")
        }

        val targetValue = targetValueStr.toFloatOrNull() ?: 1f
        val progress = (currentValue / targetValue).coerceIn(0f, 1f)
        val progressColor = if (selectedCategory == GoalCategory.WEIGHT && currentValue > targetValue) Color(0xFFEF6C00) else MaterialTheme.colorScheme.primary

        val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "Progress")

        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(200.dp), color = Color.LightGray.copy(alpha = 0.3f), strokeWidth = 12.dp)
        CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.size(200.dp), color = progressColor, strokeWidth = 12.dp, strokeCap = StrokeCap.Round)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (selectedCategory == GoalCategory.WEIGHT) "Mevcut" else "Bugün", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = if (selectedCategory == GoalCategory.STEPS) currentValue.toInt().toString() else String.format("%.1f", currentValue), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = progressColor)
            Text(text = "/ $targetValueStr $displayUnit Hedef", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun CategoryInputFields(category: GoalCategory, stepInput: String, onStepChange: (String) -> Unit, calorieInput: String, onCalorieChange: (String) -> Unit, weightInput: String, onWeightChange: (String) -> Unit) {
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    when (category) {
        GoalCategory.STEPS -> OutlinedTextField(value = stepInput, onValueChange = { if (it.all { c -> c.isDigit() }) onStepChange(it) }, label = { Text("Günlük Adım") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = keyboardOptions, trailingIcon = { Icon(Icons.Default.DirectionsWalk, null) })
        GoalCategory.CALORIES -> OutlinedTextField(value = calorieInput, onValueChange = { if (it.all { c -> c.isDigit() }) onCalorieChange(it) }, label = { Text("Günlük Kalori") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = keyboardOptions, trailingIcon = { Icon(Icons.Default.LocalFireDepartment, null) })
        GoalCategory.WEIGHT -> OutlinedTextField(value = weightInput, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onWeightChange(it) }, label = { Text("Hedef Kilo") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = keyboardOptions, trailingIcon = { Icon(Icons.Default.MonitorWeight, null) })
    }
}

@Composable
fun GoalSelectionCard(category: GoalCategory, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(110.dp).height(80.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick() }.border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(category.icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
            Text(category.title, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

enum class GoalCategory(val title: String, val unit: String, val icon: ImageVector) {
    STEPS("Adım", "adım", Icons.Default.DirectionsWalk),
    CALORIES("Kalori", "kcal", Icons.Default.LocalFireDepartment),
    WEIGHT("Kilo", "kg", Icons.Default.MonitorWeight)
}