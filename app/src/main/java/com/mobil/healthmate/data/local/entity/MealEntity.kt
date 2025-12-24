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

    val mealType: MealType, // Değiştirildi: Enum kullanımı

    val date: Long,

    val totalCalories: Int = 0,

    val notes: String? = null,
    // --- SENKRONİZASYON İÇİN GEREKLİ ALANLAR (YENİ) ---

    // Dirty Flag: true = Bulutla eşit, false = Gönderilmeyi bekliyor
    val isSynced: Boolean = false,

    // Çakışma Çözümü: Son güncelleme zamanı
    val updatedAt: Long = System.currentTimeMillis()
)