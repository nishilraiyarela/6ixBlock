package com.sixblock.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimeAgoFormatterTest {
    @Test
    fun format_recentPost_usesMinutes() {
        val now = 10_000_000L
        val postedAt = now - TimeUnit.MINUTES.toMillis(12)

        assertEquals("12 min ago", TimeAgoFormatter.format(postedAt, now))
    }

    @Test
    fun format_oldPost_usesWeekLabel() {
        val now = 10_000_000_000L
        val postedAt = now - TimeUnit.DAYS.toMillis(8)

        assertEquals("1 week ago", TimeAgoFormatter.format(postedAt, now))
    }
}
