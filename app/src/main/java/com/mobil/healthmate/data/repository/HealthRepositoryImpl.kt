package com.mobil.healthmate.data.repository

import com.mobil.healthmate.data.local.dao.*
import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import com.mobil.healthmate.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val mealDao: MealDao,
    private val goalDao: GoalDao,
    private val summaryDao: DailySummaryDao
) : HealthRepository {

    override fun getUser(uid: String): Flow<UserEntity?> = userDao.getUser(uid)

    override suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    override suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>) {
        val mealId = mealDao.insertMeal(meal)
        val foodsWithId = foods.map { it.copy(parentMealId = mealId.toInt()) }
        mealDao.insertFoods(foodsWithId)
    }

    override suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteMeal(meal)

    override fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>> {
        return mealDao.getMealsWithFoods(uid)
    }

    override fun getActiveGoal(uid: String): Flow<GoalEntity?> = goalDao.getActiveGoal(uid)

    override suspend fun insertGoal(goal: GoalEntity) = goalDao.insertGoal(goal)

    override fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>> {
        return summaryDao.getLast7DaysSummary(uid)
    }
}