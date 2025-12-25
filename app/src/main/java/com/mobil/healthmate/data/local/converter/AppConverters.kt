package com.mobil.healthmate.data.local.converter

import androidx.room.TypeConverter
import com.mobil.healthmate.data.local.types.*

class AppConverters {

    // --- MealType ---
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name
    @TypeConverter
    fun toMealType(value: String): MealType = runCatching { MealType.valueOf(value) }.getOrDefault(MealType.SNACK)

    // --- FoodUnit ---
    @TypeConverter
    fun fromFoodUnit(value: FoodUnit): String = value.name
    @TypeConverter
    fun toFoodUnit(value: String): FoodUnit = runCatching { FoodUnit.valueOf(value) }.getOrDefault(FoodUnit.PIECE)

    // --- Gender ---
    @TypeConverter
    fun fromGender(value: Gender): String = value.name
    @TypeConverter
    fun toGender(value: String): Gender = runCatching { Gender.valueOf(value) }.getOrDefault(Gender.MALE)

    // --- ActivityLevel ---
    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel): String = value.name
    @TypeConverter
    fun toActivityLevel(value: String): ActivityLevel = runCatching { ActivityLevel.valueOf(value) }.getOrDefault(ActivityLevel.MODERATELY_ACTIVE)

    // --- GoalType ---
    @TypeConverter
    fun fromGoalType(value: GoalType): String = value.name
    @TypeConverter
    fun toGoalType(value: String): GoalType = runCatching { GoalType.valueOf(value) }.getOrDefault(GoalType.MAINTAIN_WEIGHT)
}