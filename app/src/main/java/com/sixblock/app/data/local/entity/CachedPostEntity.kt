package com.sixblock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val categoryId: String,
    val authorId: String,
    val authorName: String,
    val latitude: Double,
    val longitude: Double,
    val geohash: String,
    val cityKey: String,
    val approximateArea: String,
    val imageUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val commentCount: Int,
    val reportCount: Int,
    val statusId: String,
    val cachedAt: Long = System.currentTimeMillis()
)
