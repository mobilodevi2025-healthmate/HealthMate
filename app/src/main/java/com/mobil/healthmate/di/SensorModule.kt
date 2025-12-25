package com.mobil.healthmate.di

import android.content.Context
import com.mobil.healthmate.domain.manager.StepSensorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Uygulama yaşadığı sürece yaşasın
object SensorModule {

    @Provides
    @Singleton
    fun provideStepSensorManager(
        @ApplicationContext context: Context
    ): StepSensorManager {
        return StepSensorManager(context)
    }
}