package com.mobil.healthmate.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity

data class MealWithFoods(
    @Embedded val meal: MealEntity, // Ana öğün bilgisi (Kahvaltı, Saat vs.)

    @Relation(
        parentColumn = "mealId", // MealEntity'deki ID
        entityColumn = "parentMealId" // FoodEntity'deki referans ID
    )
    val foods: List<FoodEntity> // Ve o öğüne ait besinlerin listesi
)