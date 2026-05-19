package com.rememberforever.plan.scheduler

import com.rememberforever.core.data.db.entity.PlanEntity
import com.rememberforever.core.data.db.entity.PlanRuleType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PlanScheduler(
    private val workdayCalendar: ChinaWorkdayCalendar
) {
    fun datesFor(
        plan: PlanEntity,
        from: LocalDate,
        to: LocalDate,
        workdayOverrides: Map<LocalDate, Boolean> = emptyMap()
    ): List<LocalDate> {
        if (to.isBefore(from) || to.isBefore(plan.startDate)) return emptyList()
        val start = maxOf(from, plan.startDate)
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .filter { matches(plan, it, workdayOverrides) }
            .toList()
    }

    fun matches(
        plan: PlanEntity,
        date: LocalDate,
        workdayOverrides: Map<LocalDate, Boolean> = emptyMap()
    ): Boolean {
        if (date.isBefore(plan.startDate) || !plan.isActive) return false
        return when (plan.ruleType) {
            PlanRuleType.Daily -> true
            PlanRuleType.Weekly -> date.dayOfWeek.value in parseInts(plan.selectedWeekdays)
            PlanRuleType.Monthly -> date.dayOfMonth in parseInts(plan.selectedMonthDays)
            PlanRuleType.EveryNDays -> {
                val interval = plan.intervalDays.coerceAtLeast(1)
                ChronoUnit.DAYS.between(plan.startDate, date) % interval == 0L
            }
            PlanRuleType.LegalWorkday -> workdayCalendar.isLegalWorkday(date, workdayOverrides)
        }
    }

    fun workdayCalendarHasYear(year: Int): Boolean = workdayCalendar.hasYear(year)

    private fun parseInts(value: String): Set<Int> =
        value.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
}
