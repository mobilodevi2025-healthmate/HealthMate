package com.mobil.healthmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.dao.*
import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import com.mobil.healthmate.data.local.types.ActivityLevel
import com.mobil.healthmate.data.local.types.Gender
import com.mobil.healthmate.data.local.types.GoalType
import com.mobil.healthmate.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val mealDao: MealDao,
    private val goalDao: GoalDao,
    private val summaryDao: DailySummaryDao,
    private val firestore: FirebaseFirestore
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

    override suspend fun restoreUserProfileFromCloud(uid: String): Boolean {
        return try {
            val document = firestore.collection("users").document(uid).get().await()

            if (document.exists()) {
                val data = document.data ?: return false

                val user = UserEntity(
                    userId = uid,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    age = (data["age"] as? Long)?.toInt() ?: 25,
                    height = (data["height"] as? Double) ?: 170.0,
                    weight = (data["weight"] as? Double) ?: 70.0,
                    gender = try {
                        Gender.valueOf(data["gender"] as? String ?: "MALE")
                    } catch (e: Exception) { Gender.MALE },
                    activityLevel = try {
                        ActivityLevel.valueOf(data["activityLevel"] as? String ?: "MODERATELY_ACTIVE")
                    } catch (e: Exception) { ActivityLevel.MODERATELY_ACTIVE }
                )

                val goal = GoalEntity(
                    userId = uid,
                    mainGoalType = GoalType.MAINTAIN_WEIGHT,
                    targetWeight = (data["targetWeight"] as? Double),
                    dailyCalorieTarget = (data["dailyCalorieTarget"] as? Long)?.toInt() ?: 2000,
                    dailyStepTarget = (data["dailyStepTarget"] as? Long)?.toInt() ?: 10000,
                    startDate = System.currentTimeMillis()
                )

                userDao.insertUser(user)
                goalDao.insertGoal(goal)

                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}