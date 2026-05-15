package com.sixblock.app.domain.model

data class PostComment(
    val id: String,
    val postId: String,
    val body: String,
    val authorId: String,
    val authorName: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val edited: Boolean = false,
    val reportCount: Int = 0,
    val status: CommentStatus = CommentStatus.ACTIVE
)

enum class CommentStatus(val id: String) {
    ACTIVE("active"),
    DELETED("deleted"),
    REPORTED("reported");

    companion object {
        fun fromId(id: String?): CommentStatus = entries.firstOrNull { it.id == id } ?: ACTIVE
    }
}
