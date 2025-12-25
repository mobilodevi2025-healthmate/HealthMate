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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class GoalEntity(
    @PrimaryKey
    val goalId: String = UUID.randomUUID().toString(),

    val userId: String,

    val mainGoalType: GoalType,

    // --- Fiziksel Hedefler (Opsiyonel) ---
    val targetWeight: Double? = null,
    val startWeight: Double? = null,

    // --- Günlük Aktivite Hedefleri ---
    val dailyCalorieTarget: Int? = null,
    val dailyStepTarget: Int? = null,
    val dailyWaterTarget: Int? = null,
    val dailySleepTarget: Double? = null,

    // --- Aralıklı Oruç (Fasting) Saatleri ---
    val fastingWindowStart: String? = null,
    val fastingWindowEnd: String? = null,

    // --- Plan Durumu ---
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isActive: Boolean = true,

    // --- SENKRONİZASYON İÇİN GEREKLİ ALANLAR (YENİ) ---

    // Dirty Flag: true = Bulutla eşit, false = Gönderilmeyi bekliyor
    val isSynced: Boolean = false,

    // Çakışma Çözümü: Son güncelleme zamanı
    val updatedAt: Long = System.currentTimeMillis()
)