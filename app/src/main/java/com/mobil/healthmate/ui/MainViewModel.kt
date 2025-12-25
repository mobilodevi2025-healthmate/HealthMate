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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val connectivityObserver: NetworkConnectivityObserver,
    private val workManager: WorkManager
) : ViewModel() {

    // UI'ın dinleyeceği internet durumu
    val networkStatus = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectivityObserverStatus.Unavailable
        )

    init {
        // ViewModel başladığında interneti dinlemeye başla
        observeNetwork()
    }

    private fun observeNetwork() {
        networkStatus.onEach { status ->
            if (status == ConnectivityObserverStatus.Available) {
                // İNTERNET GELDİ! -> HEMEN SENKRONİZASYON BAŞLAT
                triggerImmediateSync()
            }
        }.launchIn(viewModelScope)
    }

    private fun triggerImmediateSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "ImmediateSync",
            ExistingWorkPolicy.KEEP, // Zaten çalışıyorsa elleme
            syncRequest
        )
    }
}