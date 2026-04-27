package com.dayscounter.reminder

import com.dayscounter.domain.model.Reminder

/**
 * Планировщик системных уведомлений для напоминаний.
 */
interface ReminderScheduler {
    fun schedule(
        reminder: Reminder,
        itemTitle: String
    )

    fun cancel(itemId: Long)
}
