package com.mobil.healthmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val calorie: Int,
    val protein: Int,
    val date: String,
    val imageUrl: String? = null,
    val isSynced: Boolean = false
)