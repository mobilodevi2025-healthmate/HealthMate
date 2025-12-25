package com.mobil.healthmate.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobil.healthmate.domain.repository.HealthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: HealthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncWorker", "🔄 Senkronizasyon başladı...")

            // 1. ADIM: KULLANICILARI SENKRONİZE ET
            val unsyncedUsers = repository.getUnsyncedUsers()
            unsyncedUsers.forEach { user ->
                repository.uploadUserToCloud(user) // Firestore'a at
                repository.markUserAsSynced(user.userId) // Localde işaretle
                Log.d("SyncWorker", "✅ User Synced: ${user.name}")
            }

            // 2. ADIM: HEDEFLERİ (GOALS) SENKRONİZE ET
            val unsyncedGoals = repository.getUnsyncedGoals()
            unsyncedGoals.forEach { goal ->
                repository.uploadGoalToCloud(goal)
                repository.markGoalAsSynced(goal.goalId)
            }

            // 3. ADIM: YEMEKLERİ (MEALS) SENKRONİZE ET
            val unsyncedMeals = repository.getUnsyncedMeals()
            unsyncedMeals.forEach { meal ->
                repository.uploadMealToCloud(meal)
                repository.markMealAsSynced(meal.mealId)
            }

            // 4. ADIM: BESİNLERİ (FOODS) SENKRONİZE ET
            // (Artık FoodEntity içinde userId olduğu için rahatça yükleyebiliriz)
            val unsyncedFoods = repository.getUnsyncedFoods()
            unsyncedFoods.forEach { food ->
                repository.uploadFoodToCloud(food)
                repository.markFoodAsSynced(food.foodId)
            }

            // 5. ADIM: GÜNLÜK ÖZETLERİ (SUMMARIES) SENKRONİZE ET
            val unsyncedSummaries = repository.getUnsyncedSummaries()
            unsyncedSummaries.forEach { summary ->
                repository.uploadSummaryToCloud(summary)
                repository.markSummaryAsSynced(summary.summaryId)
            }

            Log.d("SyncWorker", "🎉 Senkronizasyon başarıyla tamamlandı!")
            Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Senkronizasyon hatası: ${e.localizedMessage}")
            e.printStackTrace()
            // Hata olursa (örneğin internet koparsa) sonra tekrar dene
            Result.retry()
        }
    }
}