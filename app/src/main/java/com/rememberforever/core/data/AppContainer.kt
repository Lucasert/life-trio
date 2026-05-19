package com.rememberforever.core.data

import android.content.Context
import androidx.room.Room
import com.rememberforever.core.data.db.RememberDatabase
import com.rememberforever.core.data.repository.LedgerRepository
import com.rememberforever.core.data.repository.MemoRepository
import com.rememberforever.core.data.repository.PlanRepository
import com.rememberforever.plan.scheduler.ChinaWorkdayCalendar
import com.rememberforever.plan.scheduler.PlanScheduler

class AppContainer(context: Context) {
    val database: RememberDatabase = Room.databaseBuilder(
        context.applicationContext,
        RememberDatabase::class.java,
        "remember_forever.db"
    ).build()

    private val workdayCalendar = ChinaWorkdayCalendar.default()
    val scheduler = PlanScheduler(workdayCalendar)

    val memoRepository = MemoRepository(
        memoDao = database.memoDao(),
        tagDao = database.tagDao()
    )
    val ledgerRepository = LedgerRepository(
        ledgerDao = database.ledgerDao(),
        budgetDao = database.budgetDao()
    )
    val planRepository = PlanRepository(
        planDao = database.planDao(),
        workdayOverrideDao = database.workdayOverrideDao(),
        scheduler = scheduler
    )
}
