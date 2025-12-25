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

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid AND date = :date LIMIT 1")
    fun getSummaryByDate(uid: String, date: Long): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid AND date = :date LIMIT 1")
    suspend fun getSummaryByDateDirect(uid: String, date: Long): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid AND date >= :startDate ORDER BY date ASC")
    fun getSummariesFromDate(uid: String, startDate: Long): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid AND date = :date LIMIT 1")
    suspend fun getTodaySummary(uid: String, date: Long): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries WHERE isSynced = 0")
    suspend fun getUnsyncedSummaries(): List<DailySummaryEntity>

    @Query("UPDATE daily_summaries SET isSynced = 1 WHERE summaryId = :summaryId")
    suspend fun markSummaryAsSynced(summaryId: String)
}