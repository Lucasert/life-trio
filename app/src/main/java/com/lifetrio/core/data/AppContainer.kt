package com.lifetrio.core.data

import android.content.Context
import androidx.room.Room
import com.lifetrio.core.data.db.LifeTrioDatabase
import com.lifetrio.core.data.repository.LedgerRepository
import com.lifetrio.core.data.repository.MemoRepository
import com.lifetrio.core.data.repository.PlanRepository
import com.lifetrio.plan.scheduler.ChinaWorkdayCalendar
import com.lifetrio.plan.scheduler.PlanScheduler

class AppContainer(context: Context) {
    val database: LifeTrioDatabase = Room.databaseBuilder(
        context.applicationContext,
        LifeTrioDatabase::class.java,
        "life_trio.db"
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
