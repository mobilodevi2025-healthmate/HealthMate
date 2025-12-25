package com.mobil.healthmate.data.repository

import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.dao.*
import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import com.mobil.healthmate.data.worker.SyncWorker
import com.mobil.healthmate.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val mealDao: MealDao,
    private val goalDao: GoalDao,
    private val foodDao: FoodDao,
    private val summaryDao: DailySummaryDao,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) : HealthRepository {

    override fun getUser(uid: String): Flow<UserEntity?> = userDao.getUser(uid)

    override fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>> {
        return mealDao.getMealsWithFoods(uid)
    }

    override fun getActiveGoal(uid: String): Flow<GoalEntity?> = goalDao.getActiveGoal(uid)

    override fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (7L * 24 * 60 * 60 * 1000)
        return summaryDao.getSummariesForRange(uid, startTime, endTime)
    }

    // --- YENİ EKLENEN İMPLEMENTASYON ---
    override fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?> {
        return summaryDao.getSummaryByDate(uid, date)
    }

    override suspend fun getCurrentUser(): UserEntity? {
        return userDao.getAnyUser()
    }

    override suspend fun getTodaySummary(userId: String): DailySummaryEntity? {
        return summaryDao.getLatestSummary(userId)
    }

    override suspend fun getCurrentGoal(userId: String): GoalEntity? {
        return goalDao.getCurrentGoal(userId)
    }

    override suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
        triggerImmediateSync()
    }

    override suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>) {
        mealDao.insertMeal(meal)

        val foodsWithId = foods.map {
            it.copy(
                parentMealId = meal.mealId,
                userId = meal.userId
            )
        }
        foodDao.insertFoods(foodsWithId)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = meal.date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val mealCalories = foods.sumOf { it.calories }

        val existingSummary = summaryDao.getSummaryByLatestDate(meal.userId, startOfDay)

        if (existingSummary != null) {
            // Kayıt varsa üzerine ekle
            val updatedSummary = existingSummary.copy(
                totalCaloriesConsumed = existingSummary.totalCaloriesConsumed + mealCalories,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            summaryDao.insertSummary(updatedSummary)
        } else {
            // Kayıt yoksa yeni oluştur
            val newSummary = DailySummaryEntity(
                userId = meal.userId,
                date = startOfDay,
                totalCaloriesConsumed = mealCalories,
                totalSteps = 0, // Adım sensörü ayrıca güncelleyecek
                isSynced = false
            )
            summaryDao.insertSummary(newSummary)
        }

        triggerImmediateSync()
    }

    override suspend fun insertGoal(goal: GoalEntity) {
        goalDao.insertGoal(goal)
        triggerImmediateSync()
    }

    override suspend fun insertSummary(summary: DailySummaryEntity) {
        summaryDao.insertSummary(summary)
        triggerImmediateSync()
    }

    override suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteMeal(meal)

    override suspend fun getUnsyncedUsers() = userDao.getUnsyncedUsers()
    override suspend fun getUnsyncedGoals() = goalDao.getUnsyncedGoals()
    override suspend fun getUnsyncedMeals() = mealDao.getUnsyncedMeals()
    override suspend fun getUnsyncedFoods() = foodDao.getUnsyncedFoods()
    override suspend fun getUnsyncedSummaries() = summaryDao.getUnsyncedSummaries()

    override suspend fun markUserAsSynced(uid: String) = userDao.markUserAsSynced(uid)
    override suspend fun markGoalAsSynced(goalId: String) = goalDao.markGoalAsSynced(goalId)
    override suspend fun markMealAsSynced(mealId: String) = mealDao.markMealAsSynced(mealId)
    override suspend fun markFoodAsSynced(foodId: String) = foodDao.markFoodAsSynced(foodId)
    override suspend fun markSummaryAsSynced(summaryId: String) = summaryDao.markSummaryAsSynced(summaryId)

    override suspend fun uploadUserToCloud(user: UserEntity) {
        val map = hashMapOf(
            "name" to user.name,
            "email" to user.email,
            "age" to user.age,
            "height" to user.height,
            "weight" to user.weight,
            "gender" to user.gender.name,
            "activityLevel" to user.activityLevel.name,
            "updatedAt" to user.updatedAt
        )
        firestore.collection("users").document(user.userId).set(map).await()
    }

    override suspend fun uploadGoalToCloud(goal: GoalEntity) {
        val map = hashMapOf(
            "targetWeight" to goal.targetWeight,
            "dailyCalorieTarget" to goal.dailyCalorieTarget,
            "dailyStepTarget" to goal.dailyStepTarget,
            "updatedAt" to goal.updatedAt,
            "isActive" to goal.isActive
        )
        firestore.collection("users").document(goal.userId)
            .collection("goals").document(goal.goalId).set(map).await()
    }

    override suspend fun uploadMealToCloud(meal: MealEntity) {
        val map = hashMapOf(
            "date" to meal.date,
            "totalCalories" to meal.totalCalories,
            "mealType" to meal.mealType.name,
            "updatedAt" to meal.updatedAt
        )
        firestore.collection("users").document(meal.userId)
            .collection("meals").document(meal.mealId).set(map).await()
    }

    override suspend fun uploadFoodToCloud(food: FoodEntity) {
        val map = hashMapOf(
            "name" to food.name,
            "calories" to food.calories,
            "protein" to food.protein,
            "carbs" to food.carbs,
            "fat" to food.fat,
            "updatedAt" to food.updatedAt
        )
        firestore.collection("users").document(food.userId)
            .collection("meals").document(food.parentMealId)
            .collection("foods").document(food.foodId).set(map).await()
    }

    override suspend fun uploadSummaryToCloud(summary: DailySummaryEntity) {
        val map = hashMapOf(
            "date" to summary.date,
            "totalCalories" to summary.totalCaloriesConsumed,
            "totalSteps" to summary.totalSteps,
            "updatedAt" to summary.updatedAt
        )
        firestore.collection("users").document(summary.userId)
            .collection("summaries").document(summary.summaryId).set(map).await()
    }

    override suspend fun restoreUserProfileFromCloud(uid: String): Boolean {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "GlobalImmediateSync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }
}