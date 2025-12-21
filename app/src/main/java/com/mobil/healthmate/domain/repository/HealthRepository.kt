package com.mobil.healthmate.domain.repository

import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    // --- USER ---
    fun getUser(uid: String): Flow<UserEntity?>
    suspend fun insertUser(user: UserEntity)

    // --- MEAL & FOOD ---
    suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>)
    suspend fun deleteMeal(meal: MealEntity)
    fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>>

    // --- GOAL ---
    fun getActiveGoal(uid: String): Flow<GoalEntity?>
    suspend fun insertGoal(goal: GoalEntity)

    // --- SUMMARY ---
    fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>>

    suspend fun restoreUserProfileFromCloud(uid: String): Boolean
}