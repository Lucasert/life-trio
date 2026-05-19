package com.lifetrio.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifetrio.core.data.db.dao.BudgetDao
import com.lifetrio.core.data.db.dao.LedgerDao
import com.lifetrio.core.data.db.dao.MemoDao
import com.lifetrio.core.data.db.dao.PlanDao
import com.lifetrio.core.data.db.dao.TagDao
import com.lifetrio.core.data.db.dao.WorkdayOverrideDao
import com.lifetrio.core.data.db.entity.BudgetEntity
import com.lifetrio.core.data.db.entity.LedgerEntryEntity
import com.lifetrio.core.data.db.entity.MemoEntity
import com.lifetrio.core.data.db.entity.MemoTagCrossRef
import com.lifetrio.core.data.db.entity.PlanCompletionEntity
import com.lifetrio.core.data.db.entity.PlanEntity
import com.lifetrio.core.data.db.entity.PlanOccurrenceEntity
import com.lifetrio.core.data.db.entity.TagEntity
import com.lifetrio.core.data.db.entity.WorkdayOverrideEntity

@Database(
    entities = [
        MemoEntity::class,
        TagEntity::class,
        MemoTagCrossRef::class,
        LedgerEntryEntity::class,
        BudgetEntity::class,
        PlanEntity::class,
        PlanOccurrenceEntity::class,
        PlanCompletionEntity::class,
        WorkdayOverrideEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LifeTrioDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun tagDao(): TagDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun budgetDao(): BudgetDao
    abstract fun planDao(): PlanDao
    abstract fun workdayOverrideDao(): WorkdayOverrideDao
}
