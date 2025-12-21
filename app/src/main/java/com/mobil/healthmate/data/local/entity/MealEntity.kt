package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.MealType

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
    @PrimaryKey(autoGenerate = true)
    val mealId: Int = 0,

    val userId: String,

    val mealType: MealType, // Değiştirildi: Enum kullanımı

    val date: Long,

    val totalCalories: Int = 0,

    val notes: String? = null
)