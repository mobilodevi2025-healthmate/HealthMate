package com.mobil.healthmate.domain.manager

import kotlinx.coroutines.flow.Flow

enum class ConnectivityObserverStatus {
    Available,    // İnternet Var
    Unavailable,  // İnternet Yok
    Losing,       // Kopuyor
    Lost          // Koptu
}

interface NetworkConnectivityObserver {
    fun observe(): Flow<ConnectivityObserverStatus>
}