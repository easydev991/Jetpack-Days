package com.dayscounter.reminder

import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.usecase.ReminderRequest

/**
 * Заглушка ReminderManager для тестов и превью.
 */
object NoOpReminderManager : ReminderManager {
    override suspend fun saveReminder(
        request: ReminderRequest,
        itemTitle: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun clearReminder(itemId: Long) = Unit

    override suspend fun getActiveReminder(itemId: Long): Reminder? = null

    override suspend fun consumeReminder(itemId: Long) = Unit

    override suspend fun rescheduleFutureReminders() = Unit
}
