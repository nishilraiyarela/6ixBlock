package com.sixblock.app.domain.repository

import com.sixblock.app.core.model.Resource
import com.sixblock.app.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<Resource<List<NotificationItem>>>
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
    suspend fun setNotificationPreference(key: String, enabled: Boolean)
}
