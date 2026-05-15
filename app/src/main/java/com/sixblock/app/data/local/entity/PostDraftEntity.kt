package com.sixblock.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_drafts")
data class PostDraftEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val categoryId: String,
    val localPhotoUri: String?,
    val latitude: Double,
    val longitude: Double,
    val approximateArea: String,
    val updatedAt: Long
)
