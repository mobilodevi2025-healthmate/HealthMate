package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.FoodUnit

@Entity(
    tableName = "foods",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["mealId"],
            childColumns = ["parentMealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentMealId"])]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val foodId: Int = 0,

    val parentMealId: Int,

    val name: String,
    val quantity: Double,

    val unit: FoodUnit, // Değiştirildi: Enum kullanımı

    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)