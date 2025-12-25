package com.mobil.healthmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mobil.healthmate.data.local.types.ActivityLevel
import com.mobil.healthmate.data.local.types.Gender
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String= UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val age: Int,
    val height: Double,
    val weight: Double,

    val gender: Gender, // Değiştirildi: Enum kullanımı

    val activityLevel: ActivityLevel, // Değiştirildi: Enum kullanımı

    //val lastSyncTimestamp: Long = System.currentTimeMillis()
    // --- SENKRONİZASYON İÇİN GEREKLİ ALANLAR ---

    // 1. Dirty Flag: true = Bulutla aynı, false = Değişiklik var, gönderilmeli.
    // Varsayılan 'false' yapıyoruz ki ilk oluştuğunda "gönderilecek" olarak işaretlensin.
    val isSynced: Boolean = false,

    // 2. Çakışma Çözümü: Verinin yerelde değiştirilme zamanı.
    // Buluttaki veriyle karşılaştırırken "Hangisi daha yeni?" sorusunu çözer.
    val updatedAt: Long = System.currentTimeMillis()
)