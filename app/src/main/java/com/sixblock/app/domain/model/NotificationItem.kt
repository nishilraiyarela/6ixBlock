package com.sixblock.app.domain.model

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val postId: String?,
    val type: NotificationType,
    val createdAt: Long,
    val read: Boolean
)

enum class NotificationType(val id: String) {
    COMMENT("comment"),
    LIKE("like"),
    URGENT_NEARBY("urgent_nearby"),
    SYSTEM("system");

    companion object {
        fun fromId(id: String?): NotificationType = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
