@file:Suppress("TooGenericExceptionCaught")

package com.dayscounter.data.repository

import com.dayscounter.data.database.dao.ReminderDao
import com.dayscounter.data.database.toDomain
import com.dayscounter.data.database.toEntity
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderStatus
import com.dayscounter.domain.repository.ReminderRepository
import com.dayscounter.util.CrashReporter

/**
 * Реализация [ReminderRepository] поверх Room.
 */
class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val crashReporter: CrashReporter
) : ReminderRepository {
    override suspend fun getReminderByItemId(itemId: Long): Reminder? =
        try {
            reminderDao.getReminderByItemId(itemId)?.toDomain()
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка получения напоминания itemId=$itemId")
            throw e
        }

    override suspend fun saveReminder(reminder: Reminder) {
        try {
            reminderDao.upsertReminder(reminder.toEntity())
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка сохранения напоминания itemId=${reminder.itemId}")
            throw e
        }
    }

    override suspend fun markAsConsumed(itemId: Long) {
        try {
            reminderDao.updateStatus(
                itemId = itemId,
                status = ReminderStatus.CONSUMED.name,
                updatedAt = currentTimeMillisProvider()
            )
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка consume напоминания itemId=$itemId")
            throw e
        }
    }

    override suspend fun cancelReminder(itemId: Long) {
        try {
            reminderDao.updateStatus(
                itemId = itemId,
                status = ReminderStatus.CANCELLED.name,
                updatedAt = currentTimeMillisProvider()
            )
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка cancel напоминания itemId=$itemId")
            throw e
        }
    }

    override suspend fun deleteReminder(itemId: Long) {
        try {
            reminderDao.deleteByItemId(itemId)
        } catch (e: Exception) {
            crashReporter.logException(e, "Ошибка удаления напоминания itemId=$itemId")
            throw e
        }
    }

    override suspend fun getFutureActiveReminders(nowEpochMillis: Long): List<Reminder> =
        try {
            reminderDao
                .getFutureActiveReminders(nowEpochMillis)
                .map { it.toDomain() }
        } catch (e: Exception) {
            crashReporter.logException(
                e,
                "Ошибка получения активных напоминаний now=$nowEpochMillis"
            )
            throw e
        }
}
