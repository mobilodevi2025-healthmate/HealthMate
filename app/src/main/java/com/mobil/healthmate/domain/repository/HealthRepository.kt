package com.mobil.healthmate.domain.repository

import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.GoalEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.entity.UserEntity
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

interface HealthRepository {

    // --- MEVCUT İŞLEMLER ---
    fun getUser(uid: String): Flow<UserEntity?>
    suspend fun insertUser(user: UserEntity)

    fun getActiveGoal(uid: String): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity)

    suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>)
    suspend fun deleteMeal(meal: MealEntity)
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>

    fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>>

    suspend fun restoreUserProfileFromCloud(uid: String): Boolean

    // --- SENKRONİZASYON (EKSİK OLAN KISIMLAR) ---

    // 1. GETİR
    suspend fun getUnsyncedUsers(): List<UserEntity>
    suspend fun getUnsyncedGoals(): List<GoalEntity>
    suspend fun getUnsyncedMeals(): List<MealEntity>
    suspend fun getUnsyncedFoods(): List<FoodEntity>
    suspend fun getUnsyncedSummaries(): List<DailySummaryEntity>

    suspend fun getCurrentUser(): UserEntity?

    // 2. İŞARETLE
    suspend fun markUserAsSynced(uid: String)
    suspend fun markGoalAsSynced(goalId: String)
    suspend fun markMealAsSynced(mealId: String)
    suspend fun markFoodAsSynced(foodId: String)
    suspend fun markSummaryAsSynced(summaryId: String)

    // 3. YÜKLE
    suspend fun uploadUserToCloud(user: UserEntity)
    suspend fun uploadGoalToCloud(goal: GoalEntity)
    suspend fun uploadMealToCloud(meal: MealEntity)
    suspend fun uploadFoodToCloud(food: FoodEntity)
    suspend fun uploadSummaryToCloud(summary: DailySummaryEntity)
}