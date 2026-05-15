package com.sixblock.app.domain.model

enum class PostCategory(val id: String, val label: String, val isUrgent: Boolean = false) {
    LOST_PET("lost_pet", "Lost pet", true),
    LOCAL_EVENT("local_event", "Local event"),
    FREE_STUFF("free_stuff", "Free stuff"),
    HELP_REQUEST("help_request", "Help request"),
    SAFETY_ALERT("safety_alert", "Safety alert", true),
    RECOMMENDATION("recommendation", "Recommendation");

    companion object {
        fun fromId(id: String?): PostCategory {
            return entries.firstOrNull { it.id == id } ?: HELP_REQUEST
        }
    }
}
