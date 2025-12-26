package com.mobil.healthmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.mobil.healthmate.data.worker.SyncWorker
import com.mobil.healthmate.domain.manager.ConnectivityObserverStatus
import com.mobil.healthmate.domain.manager.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.mobil.healthmate.domain.repository.SyncRepository // <-- Eklendi

@HiltViewModel
class MainViewModel @Inject constructor(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val workManager: WorkManager,
    private val syncRepository: SyncRepository // <-- Enjekte edildi
) : ViewModel() {

    val networkStatus = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectivityObserverStatus.Unavailable
        )

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        networkStatus.onEach { status ->
            if (status == ConnectivityObserverStatus.Available) {
                triggerImmediateSync()
            }
        }.launchIn(viewModelScope)
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
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}