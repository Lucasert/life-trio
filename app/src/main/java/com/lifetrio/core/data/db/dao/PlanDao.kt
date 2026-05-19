package com.lifetrio.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifetrio.core.data.db.entity.OccurrenceStatus
import com.lifetrio.core.data.db.entity.PlanCompletionEntity
import com.lifetrio.core.data.db.entity.PlanEntity
import com.lifetrio.core.data.db.entity.PlanOccurrenceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class PlanWithOccurrence(
    val occurrenceId: Long,
    val planId: Long,
    val title: String,
    val note: String,
    val date: LocalDate,
    val status: OccurrenceStatus
)

data class CompletionCount(
    val date: LocalDate,
    val count: Int
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActivePlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE isActive = 1")
    suspend fun activePlansOnce(): List<PlanEntity>

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrence(occurrence: PlanOccurrenceEntity): Long

    @Query(
        """
        SELECT o.id AS occurrenceId, p.id AS planId, p.title AS title, p.note AS note,
               o.date AS date, o.status AS status
        FROM plan_occurrences o
        INNER JOIN plans p ON p.id = o.planId
        WHERE o.date = :date AND o.status IN ('Pending', 'Carried')
        ORDER BY p.createdAt DESC
        """
    )
    fun observeToday(date: LocalDate): Flow<List<PlanWithOccurrence>>

    @Query("SELECT * FROM plan_occurrences WHERE date = :date AND status IN ('Pending', 'Carried')")
    suspend fun pendingOn(date: LocalDate): List<PlanOccurrenceEntity>

    @Query("UPDATE plan_occurrences SET status = :status WHERE id = :occurrenceId")
    suspend fun updateOccurrenceStatus(occurrenceId: Long, status: OccurrenceStatus)

    @Query("SELECT * FROM plan_occurrences WHERE id = :occurrenceId LIMIT 1")
    suspend fun getOccurrence(occurrenceId: Long): PlanOccurrenceEntity?

    @Insert
    suspend fun insertCompletion(completion: PlanCompletionEntity)

    @Query(
        """
        SELECT date, COUNT(*) AS count
        FROM plan_completions
        WHERE date BETWEEN :start AND :end
        GROUP BY date
        ORDER BY date
        """
    )
    fun completionCounts(start: LocalDate, end: LocalDate): Flow<List<CompletionCount>>
}
