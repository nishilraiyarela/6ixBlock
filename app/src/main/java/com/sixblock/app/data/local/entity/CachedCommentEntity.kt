package com.sixblock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_comments")
data class CachedCommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val body: String,
    val authorId: String,
    val authorName: String,
    val createdAt: Long,
    val reportCount: Int,
    val statusId: String
)
