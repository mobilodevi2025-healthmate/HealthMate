package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.ActivityLevel
import com.mobil.healthmate.data.local.types.Gender

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val age: Int,
    val height: Double,
    val weight: Double,

    val gender: Gender, // Değiştirildi: Enum kullanımı

    val activityLevel: ActivityLevel, // Değiştirildi: Enum kullanımı

    val lastSyncTimestamp: Long = System.currentTimeMillis()
)