package com.lifetrio.plan

import com.lifetrio.core.data.db.entity.PlanEntity
import com.lifetrio.core.data.db.entity.PlanRuleType
import com.lifetrio.plan.scheduler.ChinaWorkdayCalendar
import com.lifetrio.plan.scheduler.PlanScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanSchedulerTest {
    private val scheduler = PlanScheduler(ChinaWorkdayCalendar.default())

    @Test
    fun weeklyPlanMatchesSelectedWeekdays() {
        val plan = PlanEntity(
            title = "健身",
            ruleType = PlanRuleType.Weekly,
            selectedWeekdays = "1,3",
            startDate = LocalDate.of(2026, 5, 18)
        )

        val dates = scheduler.datesFor(
            plan,
            LocalDate.of(2026, 5, 18),
            LocalDate.of(2026, 5, 24)
        )

        assertEquals(
            listOf(LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 20)),
            dates
        )
    }

    @Test
    fun everyNDaysStartsFromStartDate() {
        val plan = PlanEntity(
            title = "浇花",
            ruleType = PlanRuleType.EveryNDays,
            intervalDays = 3,
            startDate = LocalDate.of(2026, 5, 1)
        )

        assertTrue(scheduler.matches(plan, LocalDate.of(2026, 5, 7)))
        assertFalse(scheduler.matches(plan, LocalDate.of(2026, 5, 8)))
    }

    @Test
    fun legalWorkdayHonorsChinaHolidayOverrides() {
        val plan = PlanEntity(
            title = "工作日报",
            ruleType = PlanRuleType.LegalWorkday,
            startDate = LocalDate.of(2026, 1, 1)
        )

        assertFalse(scheduler.matches(plan, LocalDate.of(2026, 1, 1)))
        assertTrue(scheduler.matches(plan, LocalDate.of(2026, 2, 14)))
    }
}
