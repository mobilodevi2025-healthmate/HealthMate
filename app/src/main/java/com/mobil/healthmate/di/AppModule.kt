package com.mobil.healthmate.di

import android.app.Application
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobil.healthmate.data.local.AppDatabase
import com.mobil.healthmate.data.local.dao.UserDao
import com.mobil.healthmate.data.local.dao.GoalDao
import com.mobil.healthmate.data.local.dao.MealDao
import com.mobil.healthmate.data.local.dao.DailySummaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "healthmate_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao {
        return db.userDao()
    }

    @Provides
    @Singleton
    fun provideGoalDao(db: AppDatabase): GoalDao {
        return db.goalDao()
    }

    @Provides
    @Singleton
    fun provideMealDao(db: AppDatabase): MealDao {
        return db.mealDao()
    }

    @Provides
    @Singleton
    fun provideDailySummaryDao(db: AppDatabase): DailySummaryDao {
        return db.dailySummaryDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}