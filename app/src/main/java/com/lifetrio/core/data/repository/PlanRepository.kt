package com.lifetrio.core.data.repository

import com.lifetrio.core.data.db.dao.OccurrenceCount
import com.lifetrio.core.data.db.dao.PlanDao
import com.lifetrio.core.data.db.dao.PlanWithOccurrence
import com.lifetrio.core.data.db.dao.WorkdayOverrideDao
import com.lifetrio.core.data.db.entity.CarryStrategy
import com.lifetrio.core.data.db.entity.OccurrenceStatus
import com.lifetrio.core.data.db.entity.PlanCompletionEntity
import com.lifetrio.core.data.db.entity.PlanEntity
import com.lifetrio.core.data.db.entity.PlanOccurrenceEntity
import com.lifetrio.core.data.db.entity.PlanRuleType
import com.lifetrio.core.data.db.entity.WorkdayOverrideEntity
import com.lifetrio.plan.scheduler.PlanScheduler
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

class PlanRepository(
    private val planDao: PlanDao,
    private val workdayOverrideDao: WorkdayOverrideDao,
    private val scheduler: PlanScheduler
) {
    fun observePlans(): Flow<List<PlanEntity>> = planDao.observeActivePlans()

    fun observeToday(date: LocalDate = LocalDate.now()): Flow<List<PlanWithOccurrence>> =
        planDao.observeToday(date)

    fun observeOccurrenceCounts(start: LocalDate, end: LocalDate): Flow<List<OccurrenceCount>> =
        planDao.occurrenceCounts(start, end)

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
        generateOccurrences(LocalDate.now().minusDays(179), LocalDate.now().plusDays(31))
        return id
    }

    suspend fun updatePlan(
        plan: PlanEntity,
        title: String,
        note: String,
        ruleType: PlanRuleType,
        weekdays: Set<Int>,
        monthDays: Set<Int>,
        intervalDays: Int,
        carryStrategy: CarryStrategy
    ) {
        planDao.updatePlan(
            plan.copy(
                title = title,
                note = note,
                ruleType = ruleType,
                selectedWeekdays = weekdays.sorted().joinToString(","),
                selectedMonthDays = monthDays.sorted().joinToString(","),
                intervalDays = intervalDays.coerceAtLeast(1),
                carryStrategy = carryStrategy
            )
        )
        val today = LocalDate.now()
        planDao.deletePendingOccurrencesFrom(plan.id, today)
        generateOccurrences(today.minusDays(179), today.plusDays(31))
    }

    suspend fun deletePlan(planId: Long) {
        planDao.deletePlan(planId)
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

    fun legalWorkdayOverrides(start: LocalDate, end: LocalDate): Map<LocalDate, Boolean> {
        if (end.isBefore(start)) return emptyMap()
        val result = linkedMapOf<LocalDate, Boolean>()
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .forEach { date ->
                val regularWorkday = date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
                val legalWorkday = scheduler.isLegalWorkday(date)
                if (regularWorkday != legalWorkday) {
                    result[date] = legalWorkday
                }
            }
        return result
    }

    suspend fun setWorkdayOverride(date: LocalDate, isWorkday: Boolean) {
        workdayOverrideDao.upsert(WorkdayOverrideEntity(date, isWorkday))
    }

    suspend fun clearWorkdayOverride(date: LocalDate) {
        workdayOverrideDao.delete(date)
    }
}
