package com.sixblock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sixblock.app.data.local.entity.CachedCommentEntity

@Dao
interface CommentDao {
    @Query("SELECT * FROM cached_comments WHERE postId = :postId ORDER BY createdAt ASC")
    suspend fun getComments(postId: String): List<CachedCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(comments: List<CachedCommentEntity>)
}
