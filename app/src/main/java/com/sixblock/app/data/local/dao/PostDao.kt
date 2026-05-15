package com.sixblock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sixblock.app.data.local.entity.CachedPostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM cached_posts ORDER BY createdAt DESC")
    suspend fun getRecentPosts(): List<CachedPostEntity>

    @Query("SELECT * FROM cached_posts WHERE id = :postId LIMIT 1")
    suspend fun getPost(postId: String): CachedPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<CachedPostEntity>)

    @Query("DELETE FROM cached_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
}
