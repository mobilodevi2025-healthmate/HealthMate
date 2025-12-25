package com.mobil.healthmate.ui.add_meal

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.types.FoodUnit
import com.mobil.healthmate.data.local.types.MealType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    viewModel: AddMealViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var foodName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var calorie by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    // Birim Seçimi için State (Enum)
    var selectedUnit by remember { mutableStateOf(FoodUnit.PORTION) }
    var isUnitExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Öğün Oluştur") },
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
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Öğün Tipi", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = state.mealType == type,
                        onClick = { viewModel.onEvent(AddMealEvent.OnMealTypeChange(type)) },
                        label = { Text(type.displayName) } // "Kahvaltı" yazar
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Besin Ekle", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Besin Adı (Örn: Yumurta)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) quantity = it },
                    label = { Text("Miktar") },
                    modifier = Modifier.weight(0.4f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                ExposedDropdownMenuBox(
                    expanded = isUnitExpanded,
                    onExpandedChange = { isUnitExpanded = !isUnitExpanded },
                    modifier = Modifier.weight(0.6f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Birim") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isUnitExpanded,
                        onDismissRequest = { isUnitExpanded = false }
                    ) {
                        FoodUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    selectedUnit = unit
                                    isUnitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calorie,
                    onValueChange = { if (it.all { char -> char.isDigit() }) calorie = it },
                    label = { Text("Kalori") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) protein = it },
                    label = { Text("Prot(g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) carbs = it },
                    label = { Text("Karb(g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fat = it },
                    label = { Text("Yağ(g)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (foodName.isNotBlank() && calorie.isNotBlank()) {
                        val newFood = FoodEntity(
                            parentMealId = "0",
                            name = foodName,
                            userId = "",

                            quantity = quantity.toDoubleOrNull() ?: 1.0,
                            unit = selectedUnit,

                            calories = calorie.toIntOrNull() ?: 0,
                            protein = protein.toDoubleOrNull() ?: 0.0,
                            carbs = carbs.toDoubleOrNull() ?: 0.0,
                            fat = fat.toDoubleOrNull() ?: 0.0
                        )
                        viewModel.onEvent(AddMealEvent.OnAddFood(newFood))

                        foodName = ""
                        calorie = ""
                        protein = ""
                        carbs = ""
                        fat = ""
                        quantity = "1"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Besini Tabağa Ekle")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.addedFoods.isNotEmpty()) {
                Text("Tabaktakiler (${state.addedFoods.sumOf { it.calories }.toString()} kcal)", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.addedFoods) { food ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(food.name, fontWeight = FontWeight.Bold)
                                    // Miktar ve Birim Gösterimi (Örn: 2 Adet)
                                    Text(
                                        "${food.quantity} ${food.unit.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Text("${food.calories} kcal")
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Henüz besin eklenmedi", color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    viewModel.onEvent(AddMealEvent.SaveMeal)
                    Toast.makeText(context, "Öğün başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = state.addedFoods.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Öğünü Tamamla ve Kaydet")
            }
        }
    }
}