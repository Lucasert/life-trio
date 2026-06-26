package com.lifetrio.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifetrio.core.data.db.entity.LedgerEntryEntity
import com.lifetrio.core.data.db.entity.LedgerType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class CategoryTotal(
    val category: String,
    val totalCents: Long
)

data class MonthTotal(
    val month: String,
    val expenseCents: Long,
    val incomeCents: Long
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE date BETWEEN :start AND :end ORDER BY date DESC, createdAt DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<LedgerEntryEntity>>

    @Insert
    suspend fun insert(entry: LedgerEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<LedgerEntryEntity>)

    @Query("DELETE FROM ledger_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, createdAt DESC")
    suspend fun getAll(): List<LedgerEntryEntity>

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM ledger_entries WHERE type = :type AND date BETWEEN :start AND :end")
    suspend fun totalFor(type: LedgerType, start: LocalDate, end: LocalDate): Long

    @Query(
        """
        SELECT category, COALESCE(SUM(amountCents), 0) AS totalCents
        FROM ledger_entries
        WHERE type = :type AND date BETWEEN :start AND :end
        GROUP BY category
        ORDER BY totalCents DESC
        """
    )
    fun categoryTotals(type: LedgerType, start: LocalDate, end: LocalDate): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT substr(date, 1, 7) AS month,
               COALESCE(SUM(CASE WHEN type = 'Expense' THEN amountCents ELSE 0 END), 0) AS expenseCents,
               COALESCE(SUM(CASE WHEN type = 'Income' THEN amountCents ELSE 0 END), 0) AS incomeCents
        FROM ledger_entries
        WHERE date BETWEEN :start AND :end
        GROUP BY substr(date, 1, 7)
        ORDER BY month
        """
    )
    fun monthlyTotals(start: LocalDate, end: LocalDate): Flow<List<MonthTotal>>
}
