package com.sixblock.app.data.remote.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sixblock.app.R
import com.sixblock.app.domain.model.NotificationItem
import com.sixblock.app.ui.main.MainActivity

object SixBlockNotificationPresenter {
    private const val CHANNEL_ID = "sixblock_activity"

    fun showActivityNotification(context: Context, item: NotificationItem) {
        if (!canPostNotifications(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(context, manager)

        val intent = Intent(context, MainActivity::class.java).apply {
            item.postId?.let { putExtra(MainActivity.EXTRA_POST_ID, it) }
            putExtra(MainActivity.EXTRA_OPEN_ACTIVITY, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_activity_24)
            .setContentTitle(item.title)
            .setContentText(item.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(item.id.hashCode(), notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_activity),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}
