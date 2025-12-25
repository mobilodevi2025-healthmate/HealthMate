package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mobil.healthmate.data.local.entity.FoodEntity

@Dao
interface FoodDao {
    // Besin Ekleme (UUID olduğu için ConflictStrategy.REPLACE güvenlidir)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    // Çoklu Ekleme
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    // Besin Silme
    @Delete
    suspend fun deleteFood(food: FoodEntity)

    // Besin Güncelleme
    @Update
    suspend fun updateFood(food: FoodEntity)

    // --- SENKRONİZASYON SORGULARI ---

    // 1. Gönderilmemiş (Kirli) besinleri getir
    @Query("SELECT * FROM foods WHERE isSynced = 0")
    suspend fun getUnsyncedFoods(): List<FoodEntity>

    // 2. Besini 'Senkronize' olarak işaretle (ID String/UUID)
    @Query("UPDATE foods SET isSynced = 1 WHERE foodId = :foodId")
    suspend fun markFoodAsSynced(foodId: String)
}