package com.rememberforever.core.data.repository

import com.rememberforever.core.data.db.dao.CompletionCount
import com.rememberforever.core.data.db.dao.PlanDao
import com.rememberforever.core.data.db.dao.PlanWithOccurrence
import com.rememberforever.core.data.db.dao.WorkdayOverrideDao
import com.rememberforever.core.data.db.entity.CarryStrategy
import com.rememberforever.core.data.db.entity.OccurrenceStatus
import com.rememberforever.core.data.db.entity.PlanCompletionEntity
import com.rememberforever.core.data.db.entity.PlanEntity
import com.rememberforever.core.data.db.entity.PlanOccurrenceEntity
import com.rememberforever.core.data.db.entity.PlanRuleType
import com.rememberforever.core.data.db.entity.WorkdayOverrideEntity
import com.rememberforever.plan.scheduler.PlanScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PlanRepository(
    private val planDao: PlanDao,
    private val workdayOverrideDao: WorkdayOverrideDao,
    private val scheduler: PlanScheduler
) {
    fun observePlans(): Flow<List<PlanEntity>> = planDao.observeActivePlans()

    fun observeToday(date: LocalDate = LocalDate.now()): Flow<List<PlanWithOccurrence>> =
        planDao.observeToday(date)

    fun observeHeatmap(start: LocalDate, end: LocalDate): Flow<List<CompletionCount>> =
        planDao.completionCounts(start, end)

    fun observeWorkdayOverrides(): Flow<List<WorkdayOverrideEntity>> =
        workdayOverrideDao.observeAll()

    suspend fun addPlan(
        title: String,
        note: String,
        ruleType: PlanRuleType,
        weekdays: Set<Int>,
        monthDays: Set<Int>,
        intervalDays: Int,
        startDate: LocalDate,
        carryStrategy: CarryStrategy,
        sourceMemoId: Long? = null
    ): Long {
        val id = planDao.insertPlan(
            PlanEntity(
                title = title,
                note = note,
                ruleType = ruleType,
                selectedWeekdays = weekdays.sorted().joinToString(","),
                selectedMonthDays = monthDays.sorted().joinToString(","),
                intervalDays = intervalDays.coerceAtLeast(1),
                startDate = startDate,
                carryStrategy = carryStrategy,
                sourceMemoId = sourceMemoId
            )
        )
        generateOccurrences(LocalDate.now(), LocalDate.now().plusDays(31))
        return id
    }

    suspend fun generateOccurrences(from: LocalDate, to: LocalDate) {
        val overrides = workdayOverrideDao.allOnce().associate { it.date to it.isWorkday }
        planDao.activePlansOnce().forEach { plan ->
            scheduler.datesFor(plan, from, to, overrides).forEach { date ->
                planDao.insertOccurrence(PlanOccurrenceEntity(planId = plan.id, date = date))
            }
        }
    }

    suspend fun carryUnfinished(fromDate: LocalDate, toDate: LocalDate = fromDate.plusDays(1)) {
        planDao.pendingOn(fromDate).forEach { occurrence ->
            val plan = planDao.activePlansOnce().firstOrNull { it.id == occurrence.planId } ?: return@forEach
            if (plan.carryStrategy == CarryStrategy.CarryNextDay) {
                planDao.insertOccurrence(
                    PlanOccurrenceEntity(
                        planId = occurrence.planId,
                        date = toDate,
                        status = OccurrenceStatus.Carried,
                        generatedFrom = occurrence.generatedFrom
                    )
                )
                planDao.updateOccurrenceStatus(occurrence.id, OccurrenceStatus.Skipped)
            } else {
                planDao.updateOccurrenceStatus(occurrence.id, OccurrenceStatus.Skipped)
            }
        }
    }

    suspend fun completeOccurrence(occurrenceId: Long, date: LocalDate = LocalDate.now()) {
        val occurrence = planDao.getOccurrence(occurrenceId) ?: return
        planDao.updateOccurrenceStatus(occurrenceId, OccurrenceStatus.Done)
        planDao.insertCompletion(PlanCompletionEntity(planId = occurrence.planId, date = date))
    }

    suspend fun skipOccurrence(occurrenceId: Long) {
        planDao.updateOccurrenceStatus(occurrenceId, OccurrenceStatus.Skipped)
    }

    fun hasWorkdayCalendarFor(year: Int): Boolean = scheduler.workdayCalendarHasYear(year)

    suspend fun setWorkdayOverride(date: LocalDate, isWorkday: Boolean) {
        workdayOverrideDao.upsert(WorkdayOverrideEntity(date, isWorkday))
    }

    suspend fun clearWorkdayOverride(date: LocalDate) {
        workdayOverrideDao.delete(date)
    }
}
