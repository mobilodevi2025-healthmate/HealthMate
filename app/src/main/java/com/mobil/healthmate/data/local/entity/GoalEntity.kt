package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.GoalType
import java.util.UUID

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class GoalEntity(
    @PrimaryKey
    val goalId: String = UUID.randomUUID().toString(),

    val userId: String,

    val mainGoalType: GoalType,

    val targetWeight: Double? = null,
    val startWeight: Double? = null,

    val dailyCalorieTarget: Int? = null,
    val dailyStepTarget: Int? = null,
    val dailyWaterTarget: Int? = null,

    val dailySleepTarget: Double? = null,

    val bedTime: String? = "23:00",

    val fastingWindowStart: String? = null,
    val fastingWindowEnd: String? = null,

    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isActive: Boolean = true,

    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)