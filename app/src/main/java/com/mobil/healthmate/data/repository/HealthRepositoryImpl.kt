package com.mobil.healthmate.data.repository

import android.util.Log
import androidx.work.*
import com.mobil.healthmate.data.local.dao.*
import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.relation.MealWithFoods
import com.mobil.healthmate.data.worker.SyncWorker
import com.mobil.healthmate.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val goalDao: GoalDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    private val dailySummaryDao: DailySummaryDao,
    private val workManager: WorkManager
) : HealthRepository {

    override fun getUser(uid: String): Flow<UserEntity?> = userDao.getUser(uid)

    override suspend fun insertUser(user: UserEntity) {
        val userToSave = user.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        userDao.upsertUser(userToSave)
        triggerImmediateSync()
    }

    override suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    override fun getActiveGoal(uid: String): Flow<GoalEntity?> = goalDao.getActiveGoal(uid)

    override suspend fun insertGoal(goal: GoalEntity) {
        val goalToSave = goal.copy(isSynced = false, updatedAt = System.currentTimeMillis())

        Log.d("DB_KAYIT", "Hedef Kaydediliyor: ID=${goalToSave.goalId}, Adım=${goalToSave.dailyStepTarget}, isSynced=${goalToSave.isSynced}")

        goalDao.upsertGoal(goalToSave)

        val verify = goalDao.getCurrentGoal(goal.userId)
        Log.d("DB_KAYIT", "DB'den Doğrulanan: ID=${verify?.goalId}, Adım=${verify?.dailyStepTarget}, isSynced=${verify?.isSynced}")

        triggerImmediateSync()
    }

    override suspend fun getCurrentGoal(userId: String): GoalEntity? = goalDao.getCurrentGoal(userId)

    override suspend fun insertSummary(summary: DailySummaryEntity) {
        val summaryToSave = summary.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        dailySummaryDao.upsertSummary(summaryToSave)
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
        val mealToSave = meal.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        mealDao.upsertMeal(mealToSave)

        val foodsWithCorrectId = foods.map { food ->
            food.copy(
                parentMealId = meal.mealId,
                userId = meal.userId,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
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
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            dailySummaryDao.upsertSummary(updatedSummary)
        } else {
            val newSummary = DailySummaryEntity(
                userId = meal.userId,
                date = startOfDay,
                totalCaloriesConsumed = mealCalories,
                totalProtein = mealProtein,
                totalCarbs = mealCarbs,
                totalFat = mealFat,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            dailySummaryDao.upsertSummary(newSummary)
        }
        triggerImmediateSync()
    }

    override suspend fun getUserDirect(uid: String): UserEntity? {
        return userDao.getUserDirect(uid)
    }

    override suspend fun deleteMeal(meal: MealEntity) {
        mealDao.softDeleteMeal(meal.mealId)
        triggerImmediateSync()
    }

    override fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>> = mealDao.getMealsWithFoods(uid)

    override fun getWeeklyMeals(startDate: Long): Flow<List<MealEntity>> = mealDao.getMealsFromDate(startDate)

    private fun truncateToStartOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "GlobalImmediateSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}