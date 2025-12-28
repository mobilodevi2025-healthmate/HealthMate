package com.mobil.healthmate.data.local.dao

import androidx.room.*
import com.mobil.healthmate.data.local.entity.FoodEntity

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFood(food: FoodEntity): Long

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Transaction
    suspend fun upsertFood(food: FoodEntity) {
        val id = insertFood(food)
        if (id == -1L) updateFood(food)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE isSynced = 0")
    suspend fun getUnsyncedFoods(): List<FoodEntity>

    @Query("UPDATE foods SET isSynced = 1 WHERE foodId = :foodId")
    suspend fun markFoodAsSynced(foodId: String)

    @Query("SELECT * FROM foods WHERE parentMealId = :mealId")
    suspend fun getFoodsByMealId(mealId: String): List<FoodEntity>
}