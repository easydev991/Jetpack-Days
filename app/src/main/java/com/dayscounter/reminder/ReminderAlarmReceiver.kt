package com.dayscounter.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dayscounter.MainActivity
import com.dayscounter.R

/**
 * Receiver, который показывает уведомление в момент срабатывания alarm.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != ReminderIntentContract.ACTION_FIRE_REMINDER) {
            return
        }

        val itemId = intent.getLongExtra(ReminderIntentContract.EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) {
            return
        }

        val itemTitle = intent.getStringExtra(ReminderIntentContract.EXTRA_ITEM_TITLE).orEmpty()

        createNotificationChannel(context)

        val openIntent =
            Intent(context, MainActivity::class.java).apply {
                action = ReminderIntentContract.ACTION_OPEN_FROM_REMINDER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderIntentContract.EXTRA_ITEM_ID, itemId)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                ReminderIntentContract.requestCodeForItem(itemId),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val title =
            if (itemTitle.isNotBlank()) {
                context.getString(R.string.reminder_notification_title_with_item, itemTitle)
            } else {
                context.getString(R.string.reminder_notification_title)
            }

        val notification =
            NotificationCompat
                .Builder(context, ReminderIntentContract.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.reminder_notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(ReminderIntentContract.notificationIdForItem(itemId), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel =
            NotificationChannel(
                ReminderIntentContract.CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
            }
        manager.createNotificationChannel(channel)
    }
}
