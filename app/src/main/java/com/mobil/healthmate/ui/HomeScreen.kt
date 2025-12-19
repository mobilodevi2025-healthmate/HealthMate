package com.mobil.healthmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToAddMeal: () -> Unit,
    onNavigateToMealList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HealthMate'e Hoş Geldiniz",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Yemek Ekle Butonu
        Button(
            onClick = onNavigateToAddMeal,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Yeni Yemek Ekle")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Listeyi Gör Butonu
        OutlinedButton(
            onClick = onNavigateToMealList,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Yemek Geçmişini Görüntüle")
        }
    }
}