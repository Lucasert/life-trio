package com.rememberforever.core.data.repository

import com.rememberforever.core.data.db.dao.MemoDao
import com.rememberforever.core.data.db.dao.TagDao
import com.rememberforever.core.data.db.entity.MemoEntity
import com.rememberforever.core.data.db.entity.MemoTagCrossRef
import com.rememberforever.core.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class MemoRepository(
    private val memoDao: MemoDao,
    private val tagDao: TagDao
) {
    fun observeAll(): Flow<List<MemoEntity>> = memoDao.observeAll()

    fun search(query: String): Flow<List<MemoEntity>> =
        if (query.isBlank()) observeAll() else memoDao.search(query.trim())

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeAll()

    suspend fun getMemo(id: Long): MemoEntity? = memoDao.getById(id)

    suspend fun saveMemo(
        id: Long?,
        title: String,
        body: String,
        tags: List<String>,
        isPinned: Boolean,
        imageUris: List<String>
    ): Long {
        val now = Instant.now()
        val memoId = if (id == null || id == 0L) {
            memoDao.insert(
                MemoEntity(
                    title = title.ifBlank { body.take(16).ifBlank { "未命名备忘" } },
                    body = body,
                    isPinned = isPinned,
                    imageUris = imageUris.joinToString("|"),
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val existing = memoDao.getById(id) ?: return 0
            memoDao.update(
                existing.copy(
                    title = title.ifBlank { body.take(16).ifBlank { existing.title } },
                    body = body,
                    isPinned = isPinned,
                    imageUris = imageUris.joinToString("|"),
                    updatedAt = now
                )
            )
            id
        }

        memoDao.clearTags(memoId)
        tags.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { name ->
                val insertedId = tagDao.insert(TagEntity(name = name))
                val tagId = if (insertedId == -1L) tagDao.getByName(name)?.id else insertedId
                if (tagId != null) memoDao.insertCrossRef(MemoTagCrossRef(memoId, tagId))
            }
        return memoId
    }

    suspend fun tagsForMemo(memoId: Long): List<TagEntity> = tagDao.getForMemo(memoId)
}
