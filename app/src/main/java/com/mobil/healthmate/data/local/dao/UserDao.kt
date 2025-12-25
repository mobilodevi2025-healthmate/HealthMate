package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.mobil.healthmate.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity) : Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Transaction
    suspend fun upsertUser(user: UserEntity) {
        val id = insertUser(user)
        if (id == -1L) {
            updateUser(user)
        }
    }

    @Query("SELECT * FROM users WHERE userId = :uid")
    fun getUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>

    @Query("UPDATE users SET isSynced = 1 WHERE userId = :uid")
    suspend fun markUserAsSynced(uid: String)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getAnyUser(): UserEntity?
}