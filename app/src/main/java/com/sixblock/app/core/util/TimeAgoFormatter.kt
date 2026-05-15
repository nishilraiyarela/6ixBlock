package com.sixblock.app.core.util

import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

object TimeAgoFormatter {
    fun format(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = (nowMillis - timestampMillis).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val weeks = days / 7

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 2 -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days < 2 -> "Yesterday"
            days < 7 -> "$days days ago"
            weeks < 2 -> "1 week ago"
            weeks < 4 -> "$weeks weeks ago"
            else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestampMillis))
        }
    }
}
