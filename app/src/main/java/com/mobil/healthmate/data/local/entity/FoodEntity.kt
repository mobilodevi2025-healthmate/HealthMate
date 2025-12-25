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
            onDelete = ForeignKey.CASCADE
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

    val unit: FoodUnit, // Değiştirildi: Enum kullanımı

    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    // --- SENKRONİZASYON İÇİN GEREKLİ ALANLAR (YENİ) ---

    // Dirty Flag: true = Bulutla eşit, false = Gönderilmeyi bekliyor
    val isSynced: Boolean = false,

    // Çakışma Çözümü: Son güncelleme zamanı
    val updatedAt: Long = System.currentTimeMillis()
)