package com.lifetrio.core.data.repository

import com.lifetrio.core.data.db.dao.BudgetDao
import com.lifetrio.core.data.db.dao.CategoryTotal
import com.lifetrio.core.data.db.dao.LedgerDao
import com.lifetrio.core.data.db.dao.MonthTotal
import com.lifetrio.core.data.db.entity.BudgetEntity
import com.lifetrio.core.data.db.entity.LedgerEntryEntity
import com.lifetrio.core.data.db.entity.LedgerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

data class BudgetState(
    val budgetCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val warningRatio: Float,
    val isWarning: Boolean
)

class LedgerRepository(
    private val ledgerDao: LedgerDao,
    private val budgetDao: BudgetDao
) {
    fun observeEntries(): Flow<List<LedgerEntryEntity>> = ledgerDao.observeAll()

    fun observeThisMonthEntries(today: LocalDate = LocalDate.now()): Flow<List<LedgerEntryEntity>> {
        val month = YearMonth.from(today)
        return ledgerDao.observeBetween(month.atDay(1), month.atEndOfMonth())
    }

    suspend fun addEntry(type: LedgerType, category: String, amountCents: Long, note: String, date: LocalDate) {
        ledgerDao.insert(
            LedgerEntryEntity(
                type = type,
                category = category,
                amountCents = amountCents,
                note = note,
                date = date
            )
        )
    }

    fun observeCategoryTotals(month: YearMonth): Flow<List<CategoryTotal>> =
        ledgerDao.categoryTotals(LedgerType.Expense, month.atDay(1), month.atEndOfMonth())

    fun observeYearTotals(year: Int): Flow<List<MonthTotal>> =
        ledgerDao.monthlyTotals(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))

    fun observeBudgetState(month: YearMonth): Flow<BudgetState?> {
        return combine(
            budgetDao.observeMonth(month.toString()),
            ledgerDao.observeBetween(month.atDay(1), month.atEndOfMonth())
        ) { budget, entries ->
            budget ?: return@combine null
            val spent = entries.filter { it.type == LedgerType.Expense }.sumOf { it.amountCents }
            BudgetState(
                budgetCents = budget.amountCents,
                spentCents = spent,
                remainingCents = budget.amountCents - spent,
                warningRatio = budget.warningRatio,
                isWarning = spent >= budget.amountCents * budget.warningRatio
            )
        }
    }

    suspend fun setBudget(month: YearMonth, amountCents: Long, warningRatio: Float = 0.8f) {
        budgetDao.upsert(BudgetEntity(month = month.toString(), amountCents = amountCents, warningRatio = warningRatio))
    }
}
