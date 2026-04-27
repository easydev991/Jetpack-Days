package com.dayscounter.domain.repository

import com.dayscounter.domain.model.Reminder

/**
 * Репозиторий для работы с одноразовыми напоминаниями.
 */
interface ReminderRepository {
    suspend fun getReminderByItemId(itemId: Long): Reminder?

    suspend fun saveReminder(reminder: Reminder)

    suspend fun markAsConsumed(itemId: Long)

    suspend fun cancelReminder(itemId: Long)

    suspend fun deleteReminder(itemId: Long)

    suspend fun getFutureActiveReminders(nowEpochMillis: Long): List<Reminder>
}
