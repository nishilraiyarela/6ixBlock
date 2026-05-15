package com.sixblock.app.data.remote.messaging

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sixblock.app.R
import com.sixblock.app.core.util.AppSettings
import com.sixblock.app.ui.main.MainActivity
import kotlin.random.Random

class SixBlockReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!AppSettings.reminderNotificationsEnabled(context)) return
        showReminder(context)
        schedule(context)
    }

    private fun showReminder(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val channelId = "sixblock_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "6ixBlock reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val messages = listOf(
            "What's happening on your block today?",
            "Check what's new near you.",
            "Share an update with your neighbourhood."
        )
        val openIntent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ACTIVITY, true)
        val pendingIntent = PendingIntent.getActivity(
            context,
            23,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_activity_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(messages[(System.currentTimeMillis() % messages.size).toInt()])
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(6001, notification)
    }

    companion object {
        private const val MIN_REMINDER_DELAY_MS = 18L * 60L * 60L * 1000L
        private const val MAX_REMINDER_DELAY_MS = 72L * 60L * 60L * 1000L

        fun schedule(context: Context) {
            if (!AppSettings.reminderNotificationsEnabled(context)) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = reminderPendingIntent(context)
            val triggerAt = System.currentTimeMillis() + Random.nextLong(MIN_REMINDER_DELAY_MS, MAX_REMINDER_DELAY_MS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(reminderPendingIntent(context))
        }

        private fun reminderPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                6001,
                Intent(context, SixBlockReminderReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
