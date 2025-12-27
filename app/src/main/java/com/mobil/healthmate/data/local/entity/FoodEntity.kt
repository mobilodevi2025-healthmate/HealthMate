package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.FoodUnit
import java.util.UUID

@Entity(
    tableName = "foods",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["mealId"],
            childColumns = ["parentMealId"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [Index(value = ["parentMealId"])]
)
data class FoodEntity(
    @PrimaryKey
    val foodId: String = UUID.randomUUID().toString(),

    val parentMealId: String,

    val name: String,
    val quantity: Double,

    val userId: String,

    val unit: FoodUnit,

    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,

    val isSynced: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)