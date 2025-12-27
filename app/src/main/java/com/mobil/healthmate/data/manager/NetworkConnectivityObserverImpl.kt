package com.mobil.healthmate.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.mobil.healthmate.domain.manager.ConnectivityObserverStatus
import com.mobil.healthmate.domain.manager.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

class NetworkConnectivityObserverImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityObserverStatus> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(ConnectivityObserverStatus.Available) }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    launch { send(ConnectivityObserverStatus.Losing) }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(ConnectivityObserverStatus.Lost) }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(ConnectivityObserverStatus.Unavailable) }
                }
            }

            val initialStatus = getCurrentConnectivityStatus()
            launch { send(initialStatus) }

            connectivityManager.registerDefaultNetworkCallback(callback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }

    private fun getCurrentConnectivityStatus(): ConnectivityObserverStatus {
        val network = connectivityManager.activeNetwork ?: return ConnectivityObserverStatus.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectivityObserverStatus.Unavailable

        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityObserverStatus.Available
        } else {
            ConnectivityObserverStatus.Unavailable
        }
    }
}