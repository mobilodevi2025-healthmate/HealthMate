package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Transaction
    suspend fun upsertMeal(meal: MealEntity) {
        val id = insertMeal(meal)
        if (id == -1L) updateMeal(meal)
    }

    @Query("SELECT * FROM meals WHERE date >= :startDate")
    fun getMealsFromDate(startDate: Long): Flow<List<MealEntity>>

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Transaction
    @Query("SELECT * FROM meals WHERE userId = :uid AND isDeleted = 0 ORDER BY date DESC")
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>

    @Query("SELECT * FROM meals WHERE isSynced = 0")
    suspend fun getUnsyncedMeals(): List<MealEntity>

    @Query("UPDATE meals SET isSynced = 1 WHERE mealId = :mealId")
    suspend fun markMealAsSynced(mealId: String)

    // SOFT DELETE (İşaretle ama silme)
    @Query("UPDATE meals SET isDeleted = 1, isSynced = 0, updatedAt = :timestamp WHERE mealId = :mealId")
    suspend fun softDeleteMeal(mealId: String, timestamp: Long = System.currentTimeMillis())

    // HARD DELETE (Sync sonrası tamamen silmek için)
    @Query("DELETE FROM meals WHERE mealId = :mealId")
    suspend fun hardDeleteMeal(mealId: String)
}