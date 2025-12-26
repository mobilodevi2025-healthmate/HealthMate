package com.mobil.healthmate.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure()
            Log.d("SyncWorker", "🔄 Senkronizasyon başladı: $uid")

            syncRepository.uploadAllUnsyncedData(uid)

            syncRepository.downloadAllUserData(uid)



            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Hata: ${e.localizedMessage}")
            Result.retry()
        }
    }
}