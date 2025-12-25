package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.MealType
import java.util.UUID

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class MealEntity(
    @PrimaryKey
    val mealId: String = UUID.randomUUID().toString(),
    val userId: String,
    val mealType: MealType,
    val date: Long,
    val totalCalories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)