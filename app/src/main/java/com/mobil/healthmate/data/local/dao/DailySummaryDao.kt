package com.mobil.healthmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobil.healthmate.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid AND date = :date")
    fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getSummaryByLatestDate(userId: String, date: Long): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSummary(userId: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid ORDER BY date DESC LIMIT 7")
    fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>>

    // --- EKSİK OLAN RANGE SORGUSU ---
    @Query("SELECT * FROM daily_summaries WHERE userId = :uid AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getSummariesForRange(uid: String, start: Long, end: Long): Flow<List<DailySummaryEntity>>

    // --- SENKRONİZASYON ---
    @Query("SELECT * FROM daily_summaries WHERE isSynced = 0")
    suspend fun getUnsyncedSummaries(): List<DailySummaryEntity>

    @Query("UPDATE daily_summaries SET isSynced = 1 WHERE summaryId = :summaryId")
    suspend fun markSummaryAsSynced(summaryId: String)
}