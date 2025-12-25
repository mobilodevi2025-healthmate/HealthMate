package com.mobil.healthmate.domain.repository

import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

interface HealthRepository {

    // Kullanıcı İşlemleri
    fun getUser(uid: String): Flow<UserEntity?>
    suspend fun insertUser(user: UserEntity)
    suspend fun getCurrentUser(): UserEntity?

    // Hedef İşlemleri
    fun getActiveGoal(uid: String): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity)
    suspend fun getCurrentGoal(userId: String): GoalEntity?

    // Özet (Grafik) İşlemleri
    suspend fun insertSummary(summary: DailySummaryEntity)
    fun getWeeklySummaries(uid: String, startDate: Long): Flow<List<DailySummaryEntity>>
    fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?>
    suspend fun getSummaryByDateDirect(userId: String, date: Long): DailySummaryEntity?
    suspend fun getTodaySummary(userId: String): DailySummaryEntity?

    // Yemek ve Besin İşlemleri
    suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>)
    suspend fun deleteMeal(meal: MealEntity)
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>
    fun getWeeklyMeals(startDate: Long): Flow<List<MealEntity>>

    // Bulut ve Senkronizasyon
    suspend fun restoreUserProfileFromCloud(uid: String): Boolean
    suspend fun getUnsyncedUsers(): List<UserEntity>
    suspend fun getUnsyncedGoals(): List<GoalEntity>
    suspend fun getUnsyncedMeals(): List<MealEntity>
    suspend fun getUnsyncedFoods(): List<FoodEntity>
    suspend fun getUnsyncedSummaries(): List<DailySummaryEntity>

    suspend fun markUserAsSynced(uid: String)
    suspend fun markGoalAsSynced(goalId: String)
    suspend fun markMealAsSynced(mealId: String)
    suspend fun markFoodAsSynced(foodId: String)
    suspend fun markSummaryAsSynced(summaryId: String)

    suspend fun uploadUserToCloud(user: UserEntity)
    suspend fun uploadGoalToCloud(goal: GoalEntity)
    suspend fun uploadMealToCloud(meal: MealEntity)
    suspend fun uploadFoodToCloud(food: FoodEntity)
    suspend fun uploadSummaryToCloud(summary: DailySummaryEntity)
}