package com.mobil.healthmate.di

import android.app.Application
import androidx.room.Room
import com.mobil.healthmate.data.local.AppDatabase
import com.mobil.healthmate.data.local.MealDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Bu modül uygulama yaşadığı sürece yaşar (Singleton)
object AppModule {

    // 1. Veritabanının Kendisini Oluşturur
    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "healthmate_db" // Telefon hafızasındaki dosya adı
        )
            .fallbackToDestructiveMigration() // Veritabanı versiyonu değişirse eskisini silip yenisini kurar (Geliştirme için)
            .build()
    }

    // 2. Veritabanından DAO'yu Çıkarır ve Servis Eder
    // (ViewModel'lar genelde direkt DAO ister, bütün veritabanını istemez)
    @Provides
    @Singleton
    fun provideMealDao(db: AppDatabase): MealDao {
        return db.mealDao()
    }
}