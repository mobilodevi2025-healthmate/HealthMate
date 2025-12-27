package com.mobil.healthmate.domain.repository

interface SyncRepository {
    suspend fun uploadAllUnsyncedData(uid: String)
    suspend fun downloadAllUserData(uid: String)
    suspend fun downloadUserProfile(uid: String): Boolean
    fun triggerSync()
    fun setupPeriodicSync()


}