package com.sixblock.app.domain.model

data class CommunityPost(
    val id: String,
    val title: String,
    val body: String,
    val category: PostCategory,
    val authorId: String,
    val authorName: String,
    val location: GeoPoint,
    val geohash: String,
    val cityKey: String,
    val approximateArea: String,
    val imageUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val commentCount: Int,
    val likeCount: Int = 0,
    val likedByCurrentUser: Boolean = false,
    val savedByCurrentUser: Boolean = false,
    val reportCount: Int,
    val status: PostStatus,
    val distanceKm: Double? = null
)

enum class PostStatus(val id: String) {
    ACTIVE("active"),
    DELETED("deleted"),
    REPORTED("reported");

    companion object {
        fun fromId(id: String?): PostStatus = entries.firstOrNull { it.id == id } ?: ACTIVE
    }
}
