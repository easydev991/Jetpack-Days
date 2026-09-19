package com.dayscounter.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dayscounter.MainActivity
import com.dayscounter.R

/**
 * Receiver, который показывает уведомление в момент срабатывания alarm.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    // Guard-клаузы в BroadcastReceiver — идиома; гасим ReturnCount вместо
    // выворачивания трёх ранних выходов в один return в ущерб читаемости.
    @Suppress("ReturnCount")
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

        // Без POST_NOTIFICATIONS (API 33+) система всё равно молча отбрасывает
        // уведомление — гвард убирает lint MissingPermission, ничего не теряем.
        // Прямой вызов checkSelfPermission: межпроцедурный анализ линта не видит
        // гвард через extension hasPostNotificationsPermission().
        // InlinedApi: константа инлайнится компилятором, значение стабильно на
        // всех уровнях API; на API < 33 checkSelfPermission вернёт granted.
        @SuppressLint("InlinedApi")
        val hasNotificationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) {
            return
        }

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
            itemTitle.ifBlank {
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
        // minSdk = 26: каналы доступны всегда, гвард SDK_INT не нужен
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
