package com.mobil.healthmate.domain.repository

import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun getUser(uid: String): Flow<UserEntity?>
    suspend fun insertUser(user: UserEntity)
    suspend fun getCurrentUser(): UserEntity?
    suspend fun getUserDirect(uid: String): UserEntity?

    fun getActiveGoal(uid: String): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity)
    suspend fun getCurrentGoal(userId: String): GoalEntity?

    suspend fun insertSummary(summary: DailySummaryEntity)
    fun getWeeklySummaries(uid: String, startDate: Long): Flow<List<DailySummaryEntity>>
    fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?>
    suspend fun getSummaryByDateDirect(userId: String, date: Long): DailySummaryEntity?
    suspend fun getTodaySummary(userId: String): DailySummaryEntity?

    suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>)
    suspend fun getAiRecommendation() : String
    suspend fun deleteMeal(meal: MealEntity)
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>
    fun getWeeklyMeals(startDate: Long): Flow<List<MealEntity>>

    fun triggerImmediateSync()
}