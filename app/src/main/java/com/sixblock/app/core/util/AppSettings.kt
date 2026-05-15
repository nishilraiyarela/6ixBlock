package com.sixblock.app.core.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppSettings {
    private const val PREFS_NAME = "sixblock_settings"
    private const val KEY_RADIUS_KM = "radius_km"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LIKE_NOTIFICATIONS = "like_notifications"
    private const val KEY_COMMENT_NOTIFICATIONS = "comment_notifications"
    private const val KEY_REMINDER_NOTIFICATIONS = "reminder_notifications"
    private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    private const val KEY_FEED_SEEN_AT = "feed_seen_at"
    private const val KEY_ACTIVITY_NOTIFICATION_SHOWN_AT = "activity_notification_shown_at"
    private const val LEGACY_DARK_MODE = "dark_mode"

    const val THEME_SYSTEM = "system"
    const val THEME_ON = "on"
    const val THEME_OFF = "off"
    const val DEFAULT_RADIUS_KM = 5
    const val NOTIFICATION_LIKES = "likeNotificationsEnabled"
    const val NOTIFICATION_COMMENTS = "commentNotificationsEnabled"
    const val NOTIFICATION_REMINDERS = "reminderNotificationsEnabled"

    fun radiusKm(context: Context): Int {
        return prefs(context).getInt(KEY_RADIUS_KM, DEFAULT_RADIUS_KM).coerceIn(1, 25)
    }

    fun saveRadiusKm(context: Context, radiusKm: Int) {
        prefs(context).edit().putInt(KEY_RADIUS_KM, radiusKm.coerceIn(1, 25)).apply()
    }

    fun themeMode(context: Context): String {
        val prefs = prefs(context)
        val saved = prefs.getString(KEY_THEME_MODE, null)
        if (saved != null) return saved
        return if (prefs.getBoolean(LEGACY_DARK_MODE, false)) THEME_ON else THEME_SYSTEM
    }

    fun saveThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun applyThemeMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (themeMode(context)) {
                THEME_ON -> AppCompatDelegate.MODE_NIGHT_YES
                THEME_OFF -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun likeNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LIKE_NOTIFICATIONS, true)

    fun commentNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMMENT_NOTIFICATIONS, true)

    fun reminderNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDER_NOTIFICATIONS, true)

    fun saveLikeNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LIKE_NOTIFICATIONS, enabled).apply()
    }

    fun saveCommentNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_COMMENT_NOTIFICATIONS, enabled).apply()
    }

    fun saveReminderNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDER_NOTIFICATIONS, enabled).apply()
    }

    fun notificationPermissionAsked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)

    fun markNotificationPermissionAsked(context: Context) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply()
    }

    fun feedSeenAt(context: Context): Long {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_FEED_SEEN_AT)) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_FEED_SEEN_AT, now).apply()
            return now
        }
        return prefs.getLong(KEY_FEED_SEEN_AT, 0L)
    }

    fun markFeedSeenNow(context: Context) {
        prefs(context).edit().putLong(KEY_FEED_SEEN_AT, System.currentTimeMillis()).apply()
    }

    fun activityNotificationShownAt(context: Context): Long {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_ACTIVITY_NOTIFICATION_SHOWN_AT)) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_ACTIVITY_NOTIFICATION_SHOWN_AT, now).apply()
            return now
        }
        return prefs.getLong(KEY_ACTIVITY_NOTIFICATION_SHOWN_AT, 0L)
    }

    fun markActivityNotificationShown(context: Context, createdAt: Long) {
        prefs(context).edit().putLong(KEY_ACTIVITY_NOTIFICATION_SHOWN_AT, createdAt).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
