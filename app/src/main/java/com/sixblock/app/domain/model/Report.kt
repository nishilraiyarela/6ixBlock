package com.sixblock.app.domain.model

data class Report(
    val id: String,
    val targetId: String,
    val targetType: ReportTargetType,
    val reason: String,
    val reporterId: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReportTargetType(val id: String) {
    POST("post"),
    COMMENT("comment")
}
