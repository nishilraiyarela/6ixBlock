package com.sixblock.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "hidden_content", primaryKeys = ["targetId", "targetType"])
data class HiddenContentEntity(
    val targetId: String,
    val targetType: String,
    val hiddenAt: Long = System.currentTimeMillis()
)
