package com.dayscounter.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dayscounter.crash.CrashlyticsHelper
import com.dayscounter.domain.model.Reminder
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger

/**
 * Реализация [ReminderScheduler] через AlarmManager.
 */
class AlarmReminderScheduler(
    private val context: Context,
    private val logger: Logger = AndroidLogger()
) : ReminderScheduler {
    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(
        reminder: Reminder,
        itemTitle: String
    ) {
        val alarm = alarmManager ?: return
        cancel(reminder.itemId)
        val pendingIntent =
            buildPendingIntent(
                itemId = reminder.itemId,
                itemTitle = itemTitle,
                flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ) ?: return

        try {
            alarm.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetEpochMillis,
                pendingIntent
            )
        } catch (securityException: SecurityException) {
            // Fallback на случай отсутствия права exact alarm.
            // Не глотаем молча: разработчик и Crashlytics увидят причину
            // задержки уведомлений в минуты/часы (Doze) при отключённом permission.
            val message =
                "Нет permission SCHEDULE_EXACT_ALARM — fallback на setAndAllowWhileIdle. " +
                    "Уведомления могут приходить с задержкой."
            logger.w(TAG, message, securityException)
            // Тот же текст, что и в logcat: поиск по ключевым словам работает
            // и в logcat, и в консоли Crashlytics
            CrashlyticsHelper.logException(securityException, message)
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetEpochMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(itemId: Long) {
        val alarm = alarmManager ?: return
        val pendingIntent =
            buildPendingIntent(
                itemId = itemId,
                itemTitle = null,
                flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
        alarm.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        itemId: Long,
        itemTitle: String?,
        flags: Int
    ): PendingIntent? {
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
            flags
        )
    }

    private companion object {
        const val TAG = "AlarmReminderScheduler"
    }
}
