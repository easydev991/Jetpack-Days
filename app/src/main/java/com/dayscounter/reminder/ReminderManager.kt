package com.dayscounter.reminder

import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.usecase.ReminderRequest

/**
 * Координатор жизненного цикла напоминаний.
 */
interface ReminderManager {
    suspend fun saveReminder(
        request: ReminderRequest,
        itemTitle: String
    ): Result<Unit>

    suspend fun clearReminder(itemId: Long)

    suspend fun getActiveReminder(itemId: Long): Reminder?

    suspend fun consumeReminder(itemId: Long)

    suspend fun rescheduleFutureReminders()
}
