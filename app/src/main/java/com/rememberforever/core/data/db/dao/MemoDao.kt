package com.rememberforever.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rememberforever.core.data.db.entity.MemoEntity
import com.rememberforever.core.data.db.entity.MemoTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query(
        """
        SELECT DISTINCT m.* FROM memos m
        LEFT JOIN memo_tag_cross_refs mt ON m.id = mt.memoId
        LEFT JOIN tags t ON mt.tagId = t.id
        WHERE m.title LIKE '%' || :query || '%'
           OR m.body LIKE '%' || :query || '%'
           OR t.name LIKE '%' || :query || '%'
        ORDER BY m.isPinned DESC, m.updatedAt DESC
        """
    )
    fun search(query: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: Long): MemoEntity?

    @Insert
    suspend fun insert(memo: MemoEntity): Long

    @Update
    suspend fun update(memo: MemoEntity)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM memo_tag_cross_refs WHERE memoId = :memoId")
    suspend fun clearTags(memoId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: MemoTagCrossRef)
}
