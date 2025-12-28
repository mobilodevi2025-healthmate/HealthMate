package com.mobil.healthmate.di

import com.mobil.healthmate.data.manager.NetworkConnectivityObserverImpl
import com.mobil.healthmate.data.repository.AuthRepositoryImpl
import com.mobil.healthmate.data.repository.HealthRepositoryImpl
import com.mobil.healthmate.data.repository.SyncRepositoryImpl
import com.mobil.healthmate.domain.manager.NetworkConnectivityObserver
import com.mobil.healthmate.domain.repository.AuthRepository
import com.mobil.healthmate.domain.repository.HealthRepository
import com.mobil.healthmate.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(
        healthRepositoryImpl: HealthRepositoryImpl
    ): HealthRepository
}