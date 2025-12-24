package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobil.healthmate.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // Kullanıcı varsa güncelle, yoksa ekle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // ID'ye göre kullanıcıyı getir
    @Query("SELECT * FROM users WHERE userId = :uid")
    fun getUser(uid: String): Flow<UserEntity?>

    // 1. Senkronize olmamış (isSynced = false/0) kullanıcıları getir
    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>

    // 2. Başarılı gönderim sonrası veriyi 'Senkronize Oldu' (1) olarak işaretle
    @Query("UPDATE users SET isSynced = 1 WHERE userId = :uid")
    suspend fun markUserAsSynced(uid: String)

    // BU FONKSİYONU EKLE:
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getAnyUser(): UserEntity?
}