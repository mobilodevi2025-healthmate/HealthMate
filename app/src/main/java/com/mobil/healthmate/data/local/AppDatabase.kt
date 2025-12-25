package com.mobil.healthmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobil.healthmate.data.local.converter.AppConverters
import com.mobil.healthmate.data.local.dao.*
import com.mobil.healthmate.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        GoalEntity::class,
        MealEntity::class,
        FoodEntity::class,
        DailySummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun mealDao(): MealDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun foodDao(): FoodDao
}