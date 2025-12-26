package com.mobil.healthmate.data.repository

import android.util.Log
import androidx.room.withTransaction
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.AppDatabase
import com.mobil.healthmate.data.local.dao.*
import com.mobil.healthmate.data.local.entity.*
import com.mobil.healthmate.data.local.types.*
import com.mobil.healthmate.data.worker.SyncWorker
import com.mobil.healthmate.domain.repository.SyncRepository
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val userDao: UserDao,
    private val goalDao: GoalDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    private val dailySummaryDao: DailySummaryDao,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) : SyncRepository {

    override suspend fun uploadAllUnsyncedData(uid: String) {
        val userRef = firestore.collection("users").document(uid)
        Log.d("Sync", "Senkronizasyon başlatıldı. UID: $uid")

        val unsyncedUsers = userDao.getUnsyncedUsers()
        Log.d("Sync", "Senkronize edilecek kullanıcı sayısı: ${unsyncedUsers.size}")
        unsyncedUsers.forEach { user ->
            try {
                Log.d("Sync", "Kullanıcı yükleniyor: ${user.userId}")
                userRef.set(user.toHashMap()).await()
                userDao.markUserAsSynced(user.userId)
                Log.d("Sync", "Kullanıcı başarıyla senkronize edildi.")
            } catch (e: Exception) {
                Log.e("Sync", "Kullanıcı senkronizasyon hatası: ${e.message}")
            }
        }

        val unsyncedGoals = goalDao.getUnsyncedGoals()
        Log.d("senkronizasyon", "Senkronize edilecek hedef sayısı: ${unsyncedGoals.size}")
        unsyncedGoals.forEach { goal ->
            try {
                Log.d("senkronizasyon", "Hedef yazılıyor: ID=${goal.goalId}, Adım=${goal.dailyStepTarget}")
                userRef.collection("goals").document(goal.goalId).set(goal.toHashMap()).await()
                goalDao.markGoalAsSynced(goal.goalId)
                Log.d("senkronizasyon", "Hedef başarıyla senkronize edildi.")
            } catch (e: Exception) {
                Log.e("senkronizasyon", "Hedef senkronizasyon hatası: ${e.message}")
            }
        }

        val unsyncedMeals = mealDao.getUnsyncedMeals()
        Log.d("senkronizasyon", "Senkronize edilecek öğün sayısı: ${unsyncedMeals.size}")
        unsyncedMeals.forEach { meal ->
            try {
                if (meal.isDeleted) {
                    // Firebase'den sil
                    userRef.collection("meals").document(meal.mealId).delete().await()
                    // Telefondan tamamen sil
                    mealDao.hardDeleteMeal(meal.mealId)
                } else {
                    // Güncelleme
                    Log.d("senkronizasyon", "Öğün yazılıyor: ${meal.mealId}")
                    userRef.collection("meals").document(meal.mealId).set(meal.toHashMap()).await()
                    mealDao.markMealAsSynced(meal.mealId)
                    Log.d("senkronizasyon", "Öğün başarıyla senkronize edildi.")
                }

            } catch (e: Exception) {
                Log.e("senkronizasyon", "Öğün senkronizasyon hatası: ${e.message}")
            }
        }

        val unsyncedFoods = foodDao.getUnsyncedFoods()
        Log.d("senkronizasyon", "Senkronize edilecek yiyecek sayısı: ${unsyncedFoods.size}")
        unsyncedFoods.forEach { food ->
            try {
                Log.d("senkronizasyon", "Yiyecek yazılıyor: ${food.foodId} (Meal: ${food.parentMealId})")
                userRef.collection("meals").document(food.parentMealId)
                    .collection("foods").document(food.foodId).set(food.toHashMap()).await()
                foodDao.markFoodAsSynced(food.foodId)
                Log.d("senkronizasyon", "Yiyecek başarıyla senkronize edildi.")
            } catch (e: Exception) {
                Log.e("senkronizasyon", "Yiyecek senkronizasyon hatası: ${e.message}")
            }
        }

        val unsyncedSummaries = dailySummaryDao.getUnsyncedSummaries()
        Log.d("senkronizasyon", "Senkronize edilecek özet sayısı: ${unsyncedSummaries.size}")
        unsyncedSummaries.forEach { summary ->
            try {
                Log.d("senkronizasyon", "Özet yazılıyor: ${summary.summaryId}, Adım=${summary.totalSteps}")
                userRef.collection("summaries").document(summary.summaryId).set(summary.toHashMap()).await()
                dailySummaryDao.markSummaryAsSynced(summary.summaryId)
                Log.d("senkronizasyon", "Özet başarıyla senkronize edildi.")
            } catch (e: Exception) {
                Log.e("senkronizasyon", "Özet senkronizasyon hatası: ${e.message}")
            }
        }

        Log.d("senkronizasyon", "Senkronizasyon işlemi tamamlandı.")
    }

    override suspend fun downloadUserProfile(uid: String): Boolean {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                val user = UserEntity(
                    userId = uid,
                    name = snapshot.getString("name") ?: "",
                    email = snapshot.getString("email") ?: "",
                    age = snapshot.getLong("age")?.toInt() ?: 25,
                    height = snapshot.getDouble("height") ?: 170.0,
                    weight = snapshot.getDouble("weight") ?: 70.0,
                    gender = Gender.valueOf(snapshot.getString("gender") ?: "MALE"),
                    activityLevel = ActivityLevel.valueOf(snapshot.getString("activityLevel") ?: "MODERATELY_ACTIVE"),
                    updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis(),
                    isSynced = true
                )
                userDao.upsertUser(user)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun downloadAllUserData(uid: String) {
        try {
            val userRestored = downloadUserProfile(uid)
            if (!userRestored) return

            val userRef = firestore.collection("users").document(uid)

            appDatabase.withTransaction {
                val goalsSnapshot = userRef.collection("goals").get().await()
                goalsSnapshot.forEach { doc ->
                    val goal = GoalEntity(
                        goalId = doc.id,
                        userId = uid,
                        mainGoalType = GoalType.valueOf(doc.getString("mainGoalType") ?: "MAINTAIN_WEIGHT"),
                        dailyCalorieTarget = doc.getLong("dailyCalorieTarget")?.toInt(),
                        dailyStepTarget = doc.getLong("dailyStepTarget")?.toInt(),
                        targetWeight = doc.getDouble("targetWeight"),
                        dailySleepTarget = doc.getDouble("dailySleepTarget") ?: 8.0,
                        dailyWaterTarget = doc.getLong("dailyWaterTarget")?.toInt() ?: 2500,
                        bedTime = doc.getString("bedTime") ?: "23:00",
                        isActive = doc.getBoolean("isActive") ?: true,
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        isSynced = true
                    )
                    goalDao.upsertGoal(goal)
                }

                val mealsSnapshot = userRef.collection("meals").get().await()
                mealsSnapshot.forEach { mealDoc ->
                    val meal = MealEntity(
                        mealId = mealDoc.id,
                        userId = uid,
                        date = mealDoc.getLong("date") ?: 0L,
                        totalCalories = mealDoc.getLong("totalCalories")?.toInt() ?: 0,
                        mealType = MealType.valueOf(mealDoc.getString("mealType") ?: "BREAKFAST"),
                        updatedAt = mealDoc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        isSynced = true
                    )
                    mealDao.upsertMeal(meal)

                    val foodsSnapshot = userRef.collection("meals").document(meal.mealId)
                        .collection("foods").get().await()

                    foodsSnapshot.forEach { foodDoc ->
                        val food = FoodEntity(
                            foodId = foodDoc.id,
                            parentMealId = meal.mealId,
                            userId = uid,
                            name = foodDoc.getString("name") ?: "",
                            quantity = foodDoc.getDouble("quantity") ?: 1.0,
                            unit = FoodUnit.valueOf(foodDoc.getString("unit") ?: "GRAM"),
                            calories = foodDoc.getLong("calories")?.toInt() ?: 0,
                            protein = foodDoc.getDouble("protein") ?: 0.0,
                            carbs = foodDoc.getDouble("carbs") ?: 0.0,
                            fat = foodDoc.getDouble("fat") ?: 0.0,
                            updatedAt = foodDoc.getLong("updatedAt") ?: System.currentTimeMillis(),
                            isSynced = true
                        )
                        foodDao.upsertFood(food)
                    }
                }

                val summariesSnapshot = userRef.collection("summaries").get().await()
                summariesSnapshot.forEach { doc ->
                    val summary = DailySummaryEntity(
                        summaryId = doc.id,
                        userId = uid,
                        date = doc.getLong("date") ?: 0L,
                        totalCaloriesConsumed = doc.getLong("totalCaloriesConsumed")?.toInt() ?: 0,
                        totalSteps = doc.getLong("totalSteps")?.toInt() ?: 0,
                        totalProtein = (doc.getDouble("totalProtein") ?: 0.0).toFloat(),
                        totalCarbs = (doc.getDouble("totalCarbs") ?: 0.0).toFloat(),
                        totalFat = (doc.getDouble("totalFat") ?: 0.0).toFloat(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        isSynced = true
                    )
                    dailySummaryDao.upsertSummary(summary)
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Download error: ${e.message}")
            throw e
        }
    }

    override fun triggerSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork("GlobalImmediateSync", ExistingWorkPolicy.APPEND_OR_REPLACE, syncRequest)
    }

    override fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "PeriodicCloudSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    private fun UserEntity.toHashMap() = hashMapOf(
        "name" to name, "email" to email, "age" to age, "height" to height,
        "weight" to weight, "gender" to gender.name, "activityLevel" to activityLevel.name, "updatedAt" to updatedAt
    )

    private fun GoalEntity.toHashMap() = hashMapOf(
        "mainGoalType" to mainGoalType.name,
        "dailyCalorieTarget" to (dailyCalorieTarget ?: 0),
        "dailyStepTarget" to (dailyStepTarget ?: 0),
        "targetWeight" to (targetWeight ?: 0.0),
        "dailySleepTarget" to dailySleepTarget,
        "dailyWaterTarget" to (dailyWaterTarget ?: 0),
        "bedTime" to (bedTime ?: "23:00"),
        "isActive" to isActive,
        "updatedAt" to updatedAt
    )

    private fun MealEntity.toHashMap() = hashMapOf(
        "date" to date, "totalCalories" to totalCalories, "mealType" to mealType.name, "updatedAt" to updatedAt
    )

    private fun FoodEntity.toHashMap() = hashMapOf(
        "name" to name, "quantity" to quantity, "unit" to unit.name, "calories" to calories,
        "protein" to protein, "carbs" to carbs, "fat" to fat, "updatedAt" to updatedAt
    )

    private fun DailySummaryEntity.toHashMap() = hashMapOf(
        "date" to date, "totalCaloriesConsumed" to totalCaloriesConsumed, "totalSteps" to totalSteps,
        "totalProtein" to totalProtein, "totalCarbs" to totalCarbs, "totalFat" to totalFat, "updatedAt" to updatedAt
    )
}