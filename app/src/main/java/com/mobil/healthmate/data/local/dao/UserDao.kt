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
}