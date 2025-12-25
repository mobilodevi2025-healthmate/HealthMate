package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Transaction
    @Query("SELECT * FROM meals WHERE userId = :uid ORDER BY date DESC")
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meals WHERE userId = :uid AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getMealsByDate(uid: String, start: Long, end: Long): Flow<List<MealWithFoods>>

    // --- YENİ EKLENEN SENKRONİZASYON SORGULARI ---

    // 1. Gönderilmemiş YEMEKLERİ getir
    @Query("SELECT * FROM meals WHERE isSynced = 0")
    suspend fun getUnsyncedMeals(): List<MealEntity>

    // 2. Yemeği 'Senkronize' olarak işaretle (ID'nin String/UUID olduğunu varsayıyoruz)
    @Query("UPDATE meals SET isSynced = 1 WHERE mealId = :mealId")
    suspend fun markMealAsSynced(mealId: String)

    // 3. Gönderilmemiş BESİNLERİ getir (Yemek gönderilse bile içine yeni besin eklenmiş olabilir)
    @Query("SELECT * FROM foods WHERE isSynced = 0")
    suspend fun getUnsyncedFoods(): List<FoodEntity>

    // 4. Besini 'Senkronize' olarak işaretle
    @Query("UPDATE foods SET isSynced = 1 WHERE foodId = :foodId")
    suspend fun markFoodAsSynced(foodId: String)
}