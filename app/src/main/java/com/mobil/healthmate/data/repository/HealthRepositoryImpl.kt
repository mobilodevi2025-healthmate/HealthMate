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
    private val foodDao: FoodDao,
    private val summaryDao: DailySummaryDao,
    private val firestore: FirebaseFirestore
) : HealthRepository {

    override fun getUser(uid: String): Flow<UserEntity?> = userDao.getUser(uid)

    override suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    // --- DÜZELTİLEN KISIM: UUID MANTIĞI ---
    override suspend fun insertMealWithFoods(meal: MealEntity, foods: List<FoodEntity>) {
        // 1. Yemeği kaydet (Dönen long ID'yi kullanmıyoruz çünkü UUID var)
        mealDao.insertMeal(meal)

        // 2. Besinleri kopyala: Parent ID (String UUID) ve User ID set et
        val foodsWithId = foods.map {
            it.copy(
                parentMealId = meal.mealId, // MealEntity'den gelen UUID String
                userId = meal.userId        // Yemeğin sahibi
            )
        }

        // 3. Besinleri FoodDao ile kaydet
        foodDao.insertFoods(foodsWithId)
    }

    override suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteMeal(meal)

    override fun getMealsWithFoods(uid: String): Flow<List<MealWithFoods>> {
        return mealDao.getMealsWithFoods(uid)
    }

    override fun getActiveGoal(uid: String): Flow<GoalEntity?> = goalDao.getActiveGoal(uid)

    override suspend fun insertGoal(goal: GoalEntity) = goalDao.insertGoal(goal)

    // --- DÜZELTİLEN KISIM: RANGE SORGUSU ---
    override fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (7L * 24 * 60 * 60 * 1000)
        // Artık DAO'da bu fonksiyon var
        return summaryDao.getSummariesForRange(uid, startTime, endTime)
    }

    override suspend fun restoreUserProfileFromCloud(uid: String): Boolean {
        // ... (Bu kısım aynı kalabilir, kod kalabalığı yapmamak için kısalttım) ...
        // Senin kodundaki restoreUserProfileFromCloud mantığı aynen geçerli.
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                val data = document.data ?: return false
                // ... Entity mapping işlemleri ...
                // Buradaki kodun doğru, sadece en alta isSynced = true eklemeyi unutma.
                return true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // =========================================================================
    //  SENKRONİZASYON (ARTIK HATA VERMEYECEK)
    // =========================================================================

    // A. GETİR
    override suspend fun getUnsyncedUsers() = userDao.getUnsyncedUsers()
    override suspend fun getUnsyncedGoals() = goalDao.getUnsyncedGoals()
    override suspend fun getUnsyncedMeals() = mealDao.getUnsyncedMeals()
    override suspend fun getUnsyncedFoods() = foodDao.getUnsyncedFoods()
    override suspend fun getUnsyncedSummaries() = summaryDao.getUnsyncedSummaries()

    // B. İŞARETLE
    override suspend fun markUserAsSynced(uid: String) = userDao.markUserAsSynced(uid)
    override suspend fun markGoalAsSynced(goalId: String) = goalDao.markGoalAsSynced(goalId)
    override suspend fun markMealAsSynced(mealId: String) = mealDao.markMealAsSynced(mealId)
    override suspend fun markFoodAsSynced(foodId: String) = foodDao.markFoodAsSynced(foodId)
    override suspend fun markSummaryAsSynced(summaryId: String) = summaryDao.markSummaryAsSynced(summaryId)

    // C. YÜKLE
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
    // BU FONKSİYONU EKLE (En alta ekleyebilirsin)
    override suspend fun getCurrentUser(): UserEntity? {
        return userDao.getAnyUser()
    }
}