package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_summaries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "date"], unique = true)]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val summaryId: Int = 0,

    val userId: String,

    val date: Long,

    val totalCaloriesConsumed: Int = 0,
    val totalCaloriesBurned: Int = 0,

    val totalWaterIntake: Int = 0,
    val totalSteps: Int = 0,
    val sleepDuration: Double = 0.0,

    val currentWeight: Double? = null,
    val mood: String? = null
)