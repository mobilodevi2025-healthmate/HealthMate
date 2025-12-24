package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobil.healthmate.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE userId = :uid AND isActive = 1 ORDER BY startDate DESC LIMIT 1")
    fun getActiveGoal(uid: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE userId = :uid ORDER BY startDate DESC")
    fun getAllGoals(uid: String): Flow<List<GoalEntity>>

    // --- SENKRONİZASYON SORGULARI (YENİ) ---

    // 1. Buluta gitmemiş hedefleri getir
    @Query("SELECT * FROM goals WHERE isSynced = 0")
    suspend fun getUnsyncedGoals(): List<GoalEntity>

    // 2. Hedefi 'Senkronize' olarak işaretle
    @Query("UPDATE goals SET isSynced = 1 WHERE goalId = :goalId")
    suspend fun markGoalAsSynced(goalId: String)
}