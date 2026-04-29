package com.dayscounter.data.repository

import com.dayscounter.data.database.dao.ReminderDao
import com.dayscounter.data.database.entity.ReminderEntity
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReminderRepositoryImplTest {
    @Test
    fun saveReminder_whenValid_thenPersistsAndReturnsFromGetByItemId() {
        runTest {
            // Given
            val dao = FakeReminderDao()
            val repository = ReminderRepositoryImpl(dao)
            val reminder =
                Reminder(
                    itemId = 15L,
                    mode = ReminderMode.AFTER_INTERVAL,
                    targetEpochMillis = 1_777_000_000_000L,
                    intervalAmount = 3,
                    intervalUnit = ReminderIntervalUnit.DAY,
                    status = ReminderStatus.ACTIVE,
                    createdAt = 1_700_000_000_000L,
                    updatedAt = 1_700_000_000_000L
                )

            // When
            repository.saveReminder(reminder)
            val stored = repository.getReminderByItemId(15L)

            // Then
            assertEquals(reminder, stored)
        }
    }

    @Test
    fun markAsConsumed_whenReminderExists_thenChangesStatus() {
        runTest {
            // Given
            val dao = FakeReminderDao()
            val repository = ReminderRepositoryImpl(dao)
            val reminder =
                Reminder(
                    itemId = 2L,
                    mode = ReminderMode.AT_DATE,
                    targetEpochMillis = 1_777_000_000_000L,
                    status = ReminderStatus.ACTIVE,
                    createdAt = 1_700_000_000_000L,
                    updatedAt = 1_700_000_000_000L
                )
            repository.saveReminder(reminder)

            // When
            repository.markAsConsumed(2L)

            // Then
            val stored = repository.getReminderByItemId(2L)
            assertEquals(ReminderStatus.CONSUMED, stored?.status)
        }
    }

    @Test
    fun deleteReminder_whenReminderExists_thenRemovesReminder() {
        runTest {
            // Given
            val dao = FakeReminderDao()
            val repository = ReminderRepositoryImpl(dao)
            val reminder =
                Reminder(
                    itemId = 100L,
                    mode = ReminderMode.AFTER_INTERVAL,
                    targetEpochMillis = 1_777_000_000_000L,
                    intervalAmount = 1,
                    intervalUnit = ReminderIntervalUnit.WEEK,
                    status = ReminderStatus.ACTIVE,
                    createdAt = 1_700_000_000_000L,
                    updatedAt = 1_700_000_000_000L
                )
            repository.saveReminder(reminder)

            // When
            repository.deleteReminder(100L)

            // Then
            assertNull(repository.getReminderByItemId(100L))
        }
    }

    private class FakeReminderDao : ReminderDao {
        private val storage = linkedMapOf<Long, ReminderEntity>()

        override suspend fun getReminderByItemId(itemId: Long): ReminderEntity? = storage[itemId]

        override suspend fun getFutureActiveReminders(nowEpochMillis: Long): List<ReminderEntity> =
            storage
                .values
                .filter {
                    it.status == ReminderStatus.ACTIVE.name &&
                        it.targetEpochMillis > nowEpochMillis
                }

        override suspend fun upsertReminder(reminder: ReminderEntity) {
            storage[reminder.itemId] = reminder
        }

        override suspend fun updateStatus(
            itemId: Long,
            status: String,
            updatedAt: Long
        ) {
            val current = storage[itemId] ?: return
            storage[itemId] = current.copy(status = status, updatedAt = updatedAt)
        }

        override suspend fun deleteByItemId(itemId: Long) {
            storage.remove(itemId)
        }
    }
}
