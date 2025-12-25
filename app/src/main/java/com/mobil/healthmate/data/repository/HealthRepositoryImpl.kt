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
    private val goalDao: GoalDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    private val dailySummaryDao: DailySummaryDao,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) : HealthRepository {

    override fun getUser(uid: String): Flow<UserEntity?> = userDao.getUser(uid)

    override suspend fun insertUser(user: UserEntity) {
        userDao.upsertUser(user)
        triggerImmediateSync()
    }

    override suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    override fun getActiveGoal(uid: String): Flow<GoalEntity?> = goalDao.getActiveGoal(uid)

    override suspend fun insertGoal(goal: GoalEntity) {
        // DAO'daki güvenli upsert metodunu çağırıyoruz
        goalDao.upsertGoal(goal)
        triggerImmediateSync()
    }

    override suspend fun getCurrentGoal(userId: String): GoalEntity? = goalDao.getCurrentGoal(userId)

    override suspend fun insertSummary(summary: DailySummaryEntity) {
        dailySummaryDao.insertSummary(summary)
        triggerImmediateSync()
    }

    override fun getWeeklySummaries(uid: String, startDate: Long): Flow<List<DailySummaryEntity>> {
        return dailySummaryDao.getSummariesFromDate(uid, startDate)
    }

    override fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?> {
        return dailySummaryDao.getSummaryByDate(uid, date)
    }

    override suspend fun getSummaryByDateDirect(userId: String, date: Long): DailySummaryEntity? {
        return dailySummaryDao.getSummaryByDateDirect(userId, date)
    }

    override suspend fun getTodaySummary(userId: String): DailySummaryEntity? {
        val todayStart = truncateToStartOfDay(System.currentTimeMillis())
        return dailySummaryDao.getTodaySummary(userId, todayStart)
    }

    override suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>) {
        mealDao.insertMeal(meal)

        val foodsWithCorrectId = foods.map { food ->
            food.copy(parentMealId = meal.mealId, userId = meal.userId)
        }
        foodDao.insertFoods(foodsWithCorrectId)

        val startOfDay = truncateToStartOfDay(meal.date)
        val mealCalories = foods.sumOf { it.calories }
        val mealProtein = foods.sumOf { it.protein }.toFloat()
        val mealCarbs = foods.sumOf { it.carbs }.toFloat()
        val mealFat = foods.sumOf { it.fat }.toFloat()

        val existingSummary = dailySummaryDao.getSummaryByDateDirect(meal.userId, startOfDay)

        if (existingSummary != null) {
            val updatedSummary = existingSummary.copy(
                totalCaloriesConsumed = existingSummary.totalCaloriesConsumed + mealCalories,
                totalProtein = existingSummary.totalProtein + mealProtein,
                totalCarbs = existingSummary.totalCarbs + mealCarbs,
                totalFat = existingSummary.totalFat + mealFat,
                updatedAt = System.currentTimeMillis()
            )
            dailySummaryDao.insertSummary(updatedSummary)
        } else {
            val newSummary = DailySummaryEntity(
                userId = meal.userId,
                date = startOfDay,
                totalCaloriesConsumed = mealCalories,
                totalProtein = mealProtein,
                totalCarbs = mealCarbs,
                totalFat = mealFat,
                isSynced = false
            )
            dailySummaryDao.insertSummary(newSummary)
        }
        triggerImmediateSync()
    }

    override suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteMeal(meal)

    override fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>> = mealDao.getMealsWithFoods(uid)

    override fun getWeeklyMeals(startDate: Long): Flow<List<MealEntity>> = mealDao.getMealsFromDate(startDate)

    // Senkronizasyon ve Diğer Metodlar
    override suspend fun getUnsyncedUsers() = userDao.getUnsyncedUsers()
    override suspend fun getUnsyncedGoals() = goalDao.getUnsyncedGoals()
    override suspend fun getUnsyncedMeals() = mealDao.getUnsyncedMeals()
    override suspend fun getUnsyncedFoods() = foodDao.getUnsyncedFoods()
    override suspend fun getUnsyncedSummaries() = dailySummaryDao.getUnsyncedSummaries()

    override suspend fun markUserAsSynced(uid: String) = userDao.markUserAsSynced(uid)
    override suspend fun markGoalAsSynced(goalId: String) = goalDao.markGoalAsSynced(goalId)
    override suspend fun markMealAsSynced(mealId: String) = mealDao.markMealAsSynced(mealId)
    override suspend fun markFoodAsSynced(foodId: String) = foodDao.markFoodAsSynced(foodId)
    override suspend fun markSummaryAsSynced(summaryId: String) = dailySummaryDao.markSummaryAsSynced(summaryId)

    override suspend fun uploadUserToCloud(user: UserEntity) {
        val map = hashMapOf(
            "name" to user.name, "email" to user.email, "age" to user.age,
            "height" to user.height, "weight" to user.weight, "gender" to user.gender.name,
            "activityLevel" to user.activityLevel.name, "updatedAt" to user.updatedAt
        )
        firestore.collection("users").document(user.userId).set(map).await()
    }

    override suspend fun uploadGoalToCloud(goal: GoalEntity) {
        val map = hashMapOf(
            "targetWeight" to goal.targetWeight, "dailyCalorieTarget" to goal.dailyCalorieTarget,
            "dailyStepTarget" to goal.dailyStepTarget, "updatedAt" to goal.updatedAt, "isActive" to goal.isActive
        )
        firestore.collection("users").document(goal.userId).collection("goals").document(goal.goalId).set(map).await()
    }

    override suspend fun uploadMealToCloud(meal: MealEntity) {
        val map = hashMapOf(
            "date" to meal.date, "totalCalories" to meal.totalCalories, "mealType" to meal.mealType.name, "updatedAt" to meal.updatedAt
        )
        firestore.collection("users").document(meal.userId).collection("meals").document(meal.mealId).set(map).await()
    }

    override suspend fun uploadFoodToCloud(food: FoodEntity) {
        val map = hashMapOf(
            "name" to food.name, "calories" to food.calories, "protein" to food.protein,
            "carbohydrate" to food.carbs, "fat" to food.fat, "updatedAt" to food.updatedAt
        )
        firestore.collection("users").document(food.userId).collection("meals").document(food.parentMealId).collection("foods").document(food.foodId).set(map).await()
    }

    override suspend fun uploadSummaryToCloud(summary: DailySummaryEntity) {
        val map = hashMapOf(
            "date" to summary.date, "totalCalories" to summary.totalCaloriesConsumed, "totalSteps" to summary.totalSteps,
            "totalProtein" to summary.totalProtein, "totalCarbs" to summary.totalCarbs, "totalFat" to summary.totalFat, "updatedAt" to summary.updatedAt
        )
        firestore.collection("users").document(summary.userId).collection("summaries").document(summary.summaryId).set(map).await()
    }

    override suspend fun restoreUserProfileFromCloud(uid: String): Boolean {
        return try {
            firestore.collection("users").document(uid).get().await().exists()
        } catch (e: Exception) { false }
    }

    private fun truncateToStartOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun triggerImmediateSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
        workManager.enqueueUniqueWork("GlobalImmediateSync", ExistingWorkPolicy.APPEND_OR_REPLACE, syncRequest)
    }
}