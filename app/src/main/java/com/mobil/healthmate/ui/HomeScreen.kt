package com.mobil.healthmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToAddMeal: () -> Unit,
    onNavigateToMealList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HealthMate",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Kişisel Sağlık Asistanınız",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text("Günlük İşlemler", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToAddMeal,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("🍽️  Yeni Yemek Ekle")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToMealList,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("📋  Yemek Geçmişini Gör")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Ayarlar & Hedefler", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profil Butonu
            MenuButton(
                text = "Profil",
                icon = Icons.Default.AccountCircle,
                onClick = onNavigateToProfile,
                modifier = Modifier.weight(1f)
            )

            // Hedefler Butonu
            MenuButton(
                text = "Hedefler",
                icon = Icons.Default.DateRange,
                onClick = onNavigateToGoals,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(text)
        }
    }
}