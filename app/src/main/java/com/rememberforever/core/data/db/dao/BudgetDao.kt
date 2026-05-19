package com.rememberforever.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rememberforever.core.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    fun observeMonth(month: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month LIMIT 1")
    suspend fun getMonth(month: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)
}
