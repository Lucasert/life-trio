package com.rememberforever.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rememberforever.core.data.db.dao.BudgetDao
import com.rememberforever.core.data.db.dao.LedgerDao
import com.rememberforever.core.data.db.dao.MemoDao
import com.rememberforever.core.data.db.dao.PlanDao
import com.rememberforever.core.data.db.dao.TagDao
import com.rememberforever.core.data.db.dao.WorkdayOverrideDao
import com.rememberforever.core.data.db.entity.BudgetEntity
import com.rememberforever.core.data.db.entity.LedgerEntryEntity
import com.rememberforever.core.data.db.entity.MemoEntity
import com.rememberforever.core.data.db.entity.MemoTagCrossRef
import com.rememberforever.core.data.db.entity.PlanCompletionEntity
import com.rememberforever.core.data.db.entity.PlanEntity
import com.rememberforever.core.data.db.entity.PlanOccurrenceEntity
import com.rememberforever.core.data.db.entity.TagEntity
import com.rememberforever.core.data.db.entity.WorkdayOverrideEntity

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
abstract class RememberDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun tagDao(): TagDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun budgetDao(): BudgetDao
    abstract fun planDao(): PlanDao
    abstract fun workdayOverrideDao(): WorkdayOverrideDao
}
