package com.dayscounter.reminder

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.repository.ReminderRepository
import com.dayscounter.domain.usecase.BuildReminderUseCase
import com.dayscounter.domain.usecase.ReminderRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DefaultReminderManagerTest {
    private val zoneId = ZoneId.of("Europe/Moscow")
    private val fixedClock = Clock.fixed(Instant.parse("2026-04-27T10:00:00Z"), zoneId)

    @Test
    fun saveReminder_whenRequestValid_thenSavesAndSchedules() {
        runTest {
            // Given
            val reminderRepository = FakeReminderRepository()
            val scheduler = FakeReminderScheduler()
            val itemRepository = FakeItemRepository()
            val manager =
                DefaultReminderManager(
                    reminderRepository = reminderRepository,
                    itemRepository = itemRepository,
                    reminderScheduler = scheduler,
                    buildReminderUseCase = BuildReminderUseCase(clock = fixedClock)
                )

            val request =
                ReminderRequest(
                    itemId = 10L,
                    mode = ReminderMode.AT_DATE,
                    atDate = LocalDate.of(2026, 5, 2),
                    atTime = LocalTime.of(9, 0)
                )

            // When
            val result = manager.saveReminder(request, itemTitle = "Тест")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(10L, reminderRepository.lastSavedReminder?.itemId)
            assertEquals(1, scheduler.scheduled.size)
            assertEquals("Тест", scheduler.scheduled.single().itemTitle)
        }
    }

    @Test
    fun clearReminder_whenCalled_thenCancelsAndDeletes() {
        runTest {
            // Given
            val reminderRepository = FakeReminderRepository()
            val scheduler = FakeReminderScheduler()
            val manager =
                DefaultReminderManager(
                    reminderRepository = reminderRepository,
                    itemRepository = FakeItemRepository(),
                    reminderScheduler = scheduler,
                    buildReminderUseCase = BuildReminderUseCase(clock = fixedClock)
                )

            // When
            manager.clearReminder(42L)

            // Then
            assertEquals(42L, scheduler.lastCancelledItemId)
            assertEquals(42L, reminderRepository.lastDeletedItemId)
        }
    }

    @Test
    fun rescheduleFutureReminders_whenRepositoryReturnsActive_thenSchedulesWithItemTitles() {
        runTest {
            // Given
            val reminderRepository = FakeReminderRepository()
            val scheduler = FakeReminderScheduler()
            val itemRepository = FakeItemRepository()
            val reminder =
                Reminder(
                    itemId = 5L,
                    mode = ReminderMode.AFTER_INTERVAL,
                    targetEpochMillis = 1_778_000_000_000L,
                    intervalAmount = 3,
                    intervalUnit = ReminderIntervalUnit.DAY,
                    status = ReminderStatus.ACTIVE,
                    createdAt = 1_700_000_000_000L,
                    updatedAt = 1_700_000_000_000L
                )
            reminderRepository.futureReminders = listOf(reminder)
            itemRepository.itemById[5L] =
                Item(
                    id = 5L,
                    title = "Событие",
                    details = "",
                    timestamp = 1_700_000_000_000L,
                    colorTag = null,
                    displayOption = DisplayOption.DAY
                )

            val manager =
                DefaultReminderManager(
                    reminderRepository = reminderRepository,
                    itemRepository = itemRepository,
                    reminderScheduler = scheduler,
                    buildReminderUseCase = BuildReminderUseCase(clock = fixedClock),
                    currentTimeMillisProvider = { 1_700_000_000_000L }
                )

            // When
            manager.rescheduleFutureReminders()

            // Then
            assertEquals(1, scheduler.scheduled.size)
            assertEquals("Событие", scheduler.scheduled.single().itemTitle)
        }
    }

    @Test
    fun consumeReminder_whenCalled_thenMarksAsConsumedAndCancelsAlarm() {
        runTest {
            // Given
            val reminderRepository = FakeReminderRepository()
            val scheduler = FakeReminderScheduler()
            val manager =
                DefaultReminderManager(
                    reminderRepository = reminderRepository,
                    itemRepository = FakeItemRepository(),
                    reminderScheduler = scheduler,
                    buildReminderUseCase = BuildReminderUseCase(clock = fixedClock)
                )

            // When
            manager.consumeReminder(33L)

            // Then
            assertEquals(33L, reminderRepository.lastConsumedItemId)
            assertEquals(33L, scheduler.lastCancelledItemId)
        }
    }

    @Test
    fun reminderFlow_whenConsumed_thenCanBeScheduledAgain() {
        runTest {
            // Given
            val reminderRepository = FakeReminderRepository()
            val scheduler = FakeReminderScheduler()
            val manager =
                DefaultReminderManager(
                    reminderRepository = reminderRepository,
                    itemRepository = FakeItemRepository(),
                    reminderScheduler = scheduler,
                    buildReminderUseCase = BuildReminderUseCase(clock = fixedClock)
                )

            val request =
                ReminderRequest(
                    itemId = 50L,
                    mode = ReminderMode.AFTER_INTERVAL,
                    afterAmount = 2,
                    afterUnit = ReminderIntervalUnit.DAY
                )

            // When
            val firstSave = manager.saveReminder(request, itemTitle = "Задача")
            manager.consumeReminder(50L)
            val secondSave = manager.saveReminder(request, itemTitle = "Задача")

            // Then
            assertTrue(firstSave.isSuccess)
            assertTrue(secondSave.isSuccess)
            assertEquals(2, scheduler.scheduled.size)
            assertEquals(50L, reminderRepository.lastConsumedItemId)
            assertEquals(50L, scheduler.lastCancelledItemId)
        }
    }

    @Test
    fun reminderFlow_whenCleared_thenRescheduleSkipsDeletedReminder() {
        runTest {
            // Given
            val reminderRepository = FakeReminderRepository()
            val scheduler = FakeReminderScheduler()
            val itemRepository =
                FakeItemRepository().apply {
                    itemById[99L] =
                        Item(
                            id = 99L,
                            title = "Удаляемое напоминание",
                            details = "",
                            timestamp = 1_700_000_000_000L,
                            colorTag = null,
                            displayOption = DisplayOption.DAY
                        )
                }
            val manager =
                DefaultReminderManager(
                    reminderRepository = reminderRepository,
                    itemRepository = itemRepository,
                    reminderScheduler = scheduler,
                    buildReminderUseCase = BuildReminderUseCase(clock = fixedClock),
                    currentTimeMillisProvider = { 1_700_000_000_000L }
                )
            val request =
                ReminderRequest(
                    itemId = 99L,
                    mode = ReminderMode.AT_DATE,
                    atDate = LocalDate.of(2026, 5, 2),
                    atTime = LocalTime.of(10, 0)
                )

            // When
            val saveResult = manager.saveReminder(request, itemTitle = "Удаляемое напоминание")
            manager.clearReminder(99L)
            manager.rescheduleFutureReminders()

            // Then
            assertTrue(saveResult.isSuccess)
            assertEquals(99L, reminderRepository.lastDeletedItemId)
            assertEquals(1, scheduler.scheduled.size)
        }
    }

    private class FakeReminderRepository : ReminderRepository {
        var lastSavedReminder: Reminder? = null
        var lastDeletedItemId: Long? = null
        var lastConsumedItemId: Long? = null
        var futureReminders: List<Reminder> = emptyList()
        private val remindersByItemId = mutableMapOf<Long, Reminder>()

        override suspend fun getReminderByItemId(itemId: Long): Reminder? = remindersByItemId[itemId]

        override suspend fun saveReminder(reminder: Reminder) {
            lastSavedReminder = reminder
            remindersByItemId[reminder.itemId] = reminder
        }

        override suspend fun markAsConsumed(itemId: Long) {
            lastConsumedItemId = itemId
            val reminder = remindersByItemId[itemId] ?: return
            remindersByItemId[itemId] = reminder.copy(status = ReminderStatus.CONSUMED)
        }

        override suspend fun cancelReminder(itemId: Long) {
            remindersByItemId.remove(itemId)
        }

        override suspend fun deleteReminder(itemId: Long) {
            lastDeletedItemId = itemId
            remindersByItemId.remove(itemId)
        }

        override suspend fun getFutureActiveReminders(nowEpochMillis: Long): List<Reminder> =
            if (futureReminders.isNotEmpty()) {
                futureReminders
            } else {
                remindersByItemId.values.filter { reminder ->
                    reminder.status == ReminderStatus.ACTIVE && reminder.targetEpochMillis > nowEpochMillis
                }
            }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        data class Scheduled(
            val reminder: Reminder,
            val itemTitle: String
        )

        val scheduled = mutableListOf<Scheduled>()
        var lastCancelledItemId: Long? = null

        override fun schedule(
            reminder: Reminder,
            itemTitle: String
        ) {
            scheduled += Scheduled(reminder, itemTitle)
        }

        override fun cancel(itemId: Long) {
            lastCancelledItemId = itemId
        }
    }

    private class FakeItemRepository : ItemRepository {
        val itemById = mutableMapOf<Long, Item>()

        override fun getAllItems(): Flow<List<Item>> = flowOf(emptyList())

        override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun getItemById(id: Long): Item? = itemById[id]

        override fun getItemFlow(id: Long): Flow<Item?> = flowOf(itemById[id])

        override fun searchItems(query: String): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun insertItem(item: Item): Long = item.id

        override suspend fun updateItem(item: Item) = Unit

        override suspend fun deleteItem(item: Item) = Unit

        override suspend fun deleteAllItems() = Unit

        override suspend fun getItemsCount(): Int = itemById.size
    }
}
