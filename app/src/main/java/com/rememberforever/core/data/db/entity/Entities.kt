package com.rememberforever.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val isPinned: Boolean = false,
    val imageUris: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "memo_tag_cross_refs",
    primaryKeys = ["memoId", "tagId"],
    foreignKeys = [
        ForeignKey(MemoEntity::class, ["id"], ["memoId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("memoId"), Index("tagId")]
)
data class MemoTagCrossRef(
    val memoId: Long,
    val tagId: Long
)

@Entity(tableName = "ledger_entries", indices = [Index("date"), Index("category")])
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LedgerType,
    val category: String,
    val amountCents: Long,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val createdAt: Instant = Instant.now()
)

enum class LedgerType { Expense, Income }

@Entity(tableName = "budgets", indices = [Index(value = ["month"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val amountCents: Long,
    val warningRatio: Float = 0.8f
)

@Entity(tableName = "plans", indices = [Index("isActive")])
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val ruleType: PlanRuleType,
    val selectedWeekdays: String = "",
    val selectedMonthDays: String = "",
    val intervalDays: Int = 1,
    val startDate: LocalDate = LocalDate.now(),
    val isActive: Boolean = true,
    val carryStrategy: CarryStrategy = CarryStrategy.CarryNextDay,
    val sourceMemoId: Long? = null,
    val createdAt: Instant = Instant.now()
)

enum class PlanRuleType { Daily, Weekly, Monthly, EveryNDays, LegalWorkday }

enum class CarryStrategy { CarryNextDay, Skip }

@Entity(
    tableName = "plan_occurrences",
    foreignKeys = [ForeignKey(PlanEntity::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planId"), Index("date"), Index(value = ["planId", "date"], unique = true)]
)
data class PlanOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val date: LocalDate,
    val status: OccurrenceStatus = OccurrenceStatus.Pending,
    val generatedFrom: LocalDate = date
)

enum class OccurrenceStatus { Pending, Done, Skipped, Carried }

@Entity(
    tableName = "plan_completions",
    foreignKeys = [ForeignKey(PlanEntity::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planId"), Index("date")]
)
data class PlanCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val date: LocalDate,
    val completedAt: Instant = Instant.now()
)

@Entity(tableName = "workday_overrides")
data class WorkdayOverrideEntity(
    @PrimaryKey val date: LocalDate,
    val isWorkday: Boolean
)

fun String.toAmountCents(): Long {
    val normalized = trim().ifBlank { "0" }
    return BigDecimal(normalized).movePointRight(2).toLong()
}

fun Long.toYuanText(): String = BigDecimal(this).movePointLeft(2).stripTrailingZeros().toPlainString()
