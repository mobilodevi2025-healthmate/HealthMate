package com.mobil.healthmate.data.local.dao

import androidx.room.*
import com.mobil.healthmate.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Transaction
    suspend fun upsertUser(user: UserEntity) {
        val id = insertUser(user)
        if (id == -1L) updateUser(user)
    }

    @Query("SELECT * FROM users WHERE userId = :uid")
    fun getUser(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :uid LIMIT 1")
    suspend fun getUserDirect(uid: String): UserEntity?

    @Query("SELECT COUNT(*) > 0 FROM users WHERE userId = :uid")
    fun observeUserExists(uid: String): Flow<Boolean>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>

    @Query("UPDATE users SET isSynced = 1 WHERE userId = :uid")
    suspend fun markUserAsSynced(uid: String)
}