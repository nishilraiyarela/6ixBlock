package com.sixblock.app.domain.model

data class PostDraft(
    val id: String,
    val title: String,
    val body: String,
    val category: PostCategory,
    val localPhotoUri: String?,
    val location: GeoPoint,
    val approximateArea: String,
    val updatedAt: Long
)
