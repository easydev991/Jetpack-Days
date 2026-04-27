package com.dayscounter.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dayscounter.domain.model.Reminder

/**
 * Реализация [ReminderScheduler] через AlarmManager.
 */
class AlarmReminderScheduler(
    private val context: Context
) : ReminderScheduler {
    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(
        reminder: Reminder,
        itemTitle: String
    ) {
        val alarm = alarmManager ?: return
        val pendingIntent = buildPendingIntent(reminder.itemId, itemTitle)
        cancel(reminder.itemId)

        try {
            alarm.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetEpochMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fallback на случай отсутствия права exact alarm.
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetEpochMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(itemId: Long) {
        val alarm = alarmManager ?: return
        alarm.cancel(buildPendingIntent(itemId, itemTitle = null))
    }

    private fun buildPendingIntent(
        itemId: Long,
        itemTitle: String?
    ): PendingIntent {
        val intent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderIntentContract.ACTION_FIRE_REMINDER
                putExtra(ReminderIntentContract.EXTRA_ITEM_ID, itemId)
                if (itemTitle != null) {
                    putExtra(ReminderIntentContract.EXTRA_ITEM_TITLE, itemTitle)
                }
            }

        return PendingIntent.getBroadcast(
            context,
            ReminderIntentContract.requestCodeForItem(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
