package com.mobil.healthmate.domain.repository

import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import com.mobil.healthmate.data.local.entity.FoodEntity
import com.mobil.healthmate.data.local.entity.GoalEntity
import com.mobil.healthmate.data.local.entity.MealEntity
import com.mobil.healthmate.data.local.entity.UserEntity
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

interface HealthRepository {

    fun getUser(uid: String): Flow<UserEntity?>
    suspend fun insertUser(user: UserEntity)

    fun getActiveGoal(uid: String): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity)
    suspend fun insertSummary(summary: DailySummaryEntity)

    suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>)
    suspend fun deleteMeal(meal: MealEntity)
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>

    fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>>

    fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?>

    suspend fun restoreUserProfileFromCloud(uid: String): Boolean

    suspend fun getTodaySummary(userId: String): DailySummaryEntity?
    suspend fun getCurrentGoal(userId: String): GoalEntity?

    suspend fun getUnsyncedUsers(): List<UserEntity>
    suspend fun getUnsyncedGoals(): List<GoalEntity>
    suspend fun getUnsyncedMeals(): List<MealEntity>
    suspend fun getUnsyncedFoods(): List<FoodEntity>
    suspend fun getUnsyncedSummaries(): List<DailySummaryEntity>

    suspend fun getCurrentUser(): UserEntity?

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