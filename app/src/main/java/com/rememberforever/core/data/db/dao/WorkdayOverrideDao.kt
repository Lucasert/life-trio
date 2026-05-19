package com.rememberforever.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rememberforever.core.data.db.entity.WorkdayOverrideEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkdayOverrideDao {
    @Query("SELECT * FROM workday_overrides ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkdayOverrideEntity>>

    @Query("SELECT * FROM workday_overrides")
    suspend fun allOnce(): List<WorkdayOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: WorkdayOverrideEntity)

    @Query("DELETE FROM workday_overrides WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
