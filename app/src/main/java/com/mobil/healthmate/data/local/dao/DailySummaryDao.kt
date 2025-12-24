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

    @Query("SELECT * FROM daily_summaries WHERE userId = :uid ORDER BY date DESC LIMIT 7")
    fun getLast7DaysSummary(uid: String): Flow<List<DailySummaryEntity>>
}