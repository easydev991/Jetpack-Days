package com.dayscounter.reminder

import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderStatus
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.repository.ReminderRepository
import com.dayscounter.domain.usecase.BuildReminderUseCase
import com.dayscounter.domain.usecase.ReminderRequest

/**
 * Рабочая реализация [ReminderManager].
 */
class DefaultReminderManager(
    private val reminderRepository: ReminderRepository,
    private val itemRepository: ItemRepository,
    private val reminderScheduler: ReminderScheduler,
    private val buildReminderUseCase: BuildReminderUseCase = BuildReminderUseCase(),
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() }
) : ReminderManager {
    override suspend fun saveReminder(
        request: ReminderRequest,
        itemTitle: String
    ): Result<Unit> {
        val reminderResult = buildReminderUseCase(request)
        if (reminderResult.isFailure) {
            return Result.failure(
                checkNotNull(reminderResult.exceptionOrNull()) {
                    "Reminder build should provide an error"
                }
            )
        }

        val reminder = reminderResult.getOrThrow()
        reminderRepository.saveReminder(reminder)
        reminderScheduler.schedule(reminder, itemTitle)
        return Result.success(Unit)
    }

    override suspend fun clearReminder(itemId: Long) {
        reminderScheduler.cancel(itemId)
        reminderRepository.deleteReminder(itemId)
    }

    override suspend fun getActiveReminder(itemId: Long): Reminder? =
        reminderRepository
            .getReminderByItemId(itemId)
            ?.takeIf { it.status == ReminderStatus.ACTIVE }

    override suspend fun consumeReminder(itemId: Long) {
        reminderRepository.markAsConsumed(itemId)
        reminderScheduler.cancel(itemId)
    }

    override suspend fun rescheduleFutureReminders() {
        val reminders = reminderRepository.getFutureActiveReminders(currentTimeMillisProvider())
        reminders.forEach { reminder ->
            val itemTitle = itemRepository.getItemById(reminder.itemId)?.title.orEmpty()
            reminderScheduler.schedule(reminder, itemTitle)
        }
    }
}
