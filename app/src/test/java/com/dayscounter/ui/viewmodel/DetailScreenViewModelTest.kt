package com.dayscounter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ReminderRequest
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для DetailScreenViewModel.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailScreenViewModelTest {
    private lateinit var repository: FakeItemRepository
    private lateinit var reminderManager: FakeReminderManager
    private lateinit var viewModel: DetailScreenViewModel
    private lateinit var testDispatcher: TestDispatcher

    private val testItemId = 1L
    private val testItem =
        Item(
            id = testItemId,
            title = "Тестовое событие",
            details = "Описание события",
            timestamp = System.currentTimeMillis(),
            colorTag = null,
            displayOption = com.dayscounter.domain.model.DisplayOption.DAY
        )

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
        reminderManager = FakeReminderManager()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenviewmodelcreated_thenstartswithloadingstate() {
        runTest {
            // Given - ViewModel создан без элемента
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = savedStateHandle,
                    reminderManager = reminderManager
                )

            // Then - Начальное состояние должно быть Loading
            val initialState = viewModel.uiState.value
            assertTrue(
                initialState is DetailScreenState.Loading,
                "Начальное состояние должно быть Loading"
            )
        }
    }

    @Test
    fun whenitemnotfound_thenremainsinloadingstate() {
        runTest {
            // Given - Repository не содержит элемент
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = savedStateHandle,
                    reminderManager = reminderManager
                )

            // When - Пытаемся загрузить несуществующий элемент
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Должно остаться в состоянии Loading
            val currentState = viewModel.uiState.value
            assertTrue(
                currentState is DetailScreenState.Loading,
                "Должно остаться в состоянии Loading"
            )
        }
    }

    @Test
    fun whenitemloadedsuccessfully_thenupdatestosuccessstate() {
        runTest {
            // Given - Repository содержит элемент
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))

            // When - Создаем ViewModel
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = savedStateHandle,
                    reminderManager = reminderManager
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Состояние должно измениться на Success с загруженным элементом
            val currentState = viewModel.uiState.value
            assertTrue(
                currentState is DetailScreenState.Success,
                "Состояние должно быть Success, фактическое: $currentState"
            )
            val successState = currentState as DetailScreenState.Success
            assertEquals(testItem, successState.item, "Элемент должен совпадать")
            assertEquals(null, successState.reminder, "Активного напоминания быть не должно")
        }
    }

    @Test
    fun whenfuturereminderexists_thensuccessstatecontainsreminder() {
        runTest {
            val nowMillis = 1_800_000_000_000L
            val futureReminder =
                Reminder(
                    itemId = testItemId,
                    mode = ReminderMode.AT_DATE,
                    targetEpochMillis = nowMillis + 3_600_000L,
                    selectedDateEpochMillis = nowMillis + 3_600_000L,
                    selectedHour = 12,
                    selectedMinute = 30,
                    status = ReminderStatus.ACTIVE,
                    createdAt = nowMillis - 1_000L,
                    updatedAt = nowMillis - 1_000L
                )

            repository.setItem(testItem)
            reminderManager.activeReminder = futureReminder
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId)),
                    reminderManager = reminderManager,
                    currentTimeMillisProvider = { nowMillis }
                )

            testDispatcher.scheduler.advanceUntilIdle()

            val currentState = viewModel.uiState.value as DetailScreenState.Success
            assertEquals(futureReminder, currentState.reminder, "Будущее напоминание должно отображаться")
        }
    }

    @Test
    fun whenreminderispast_thensuccessstatedoesnotcontainreminder() {
        runTest {
            val nowMillis = 1_800_000_000_000L
            val pastReminder =
                Reminder(
                    itemId = testItemId,
                    mode = ReminderMode.AT_DATE,
                    targetEpochMillis = nowMillis - 60_000L,
                    selectedDateEpochMillis = nowMillis - 60_000L,
                    selectedHour = 12,
                    selectedMinute = 30,
                    status = ReminderStatus.ACTIVE,
                    createdAt = nowMillis - 120_000L,
                    updatedAt = nowMillis - 120_000L
                )

            repository.setItem(testItem)
            reminderManager.activeReminder = pastReminder
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId)),
                    reminderManager = reminderManager,
                    currentTimeMillisProvider = { nowMillis }
                )

            testDispatcher.scheduler.advanceUntilIdle()

            val currentState = viewModel.uiState.value as DetailScreenState.Success
            assertEquals(null, currentState.reminder, "Прошедшее напоминание не должно отображаться")
        }
    }

    @Test
    fun whenrequestdelete_thenshowsdeletedialog() {
        runTest {
            // Given - ViewModel с загруженным элементом
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = savedStateHandle,
                    reminderManager = reminderManager
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Запрашиваем удаление
            viewModel.requestDelete()

            // Then - Диалог удаления должен быть показан
            assertTrue(viewModel.showDeleteDialog.value, "Диалог удаления должен быть показан")
        }
    }

    @Test
    fun whenconfirmdelete_thendeletesitem() {
        runTest {
            // Given - ViewModel с загруженным элементом
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = savedStateHandle,
                    reminderManager = reminderManager
                )
            // Ждем загрузки элемента
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Подтверждаем удаление
            viewModel.confirmDelete()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть удален и диалог скрыт
            assertFalse(
                repository.containsItem(testItemId),
                "Элемент должен быть удален из repository"
            )
            assertEquals(listOf(testItemId), reminderManager.clearedItemIds, "Reminder должен очищаться при удалении")
            assertFalse(viewModel.showDeleteDialog.value, "Диалог удаления должен быть скрыт")
            val currentState = viewModel.uiState.value
            assertTrue(currentState is DetailScreenState.Success, "После удаления состояние остается Success")
        }
    }

    @Test
    fun whenstaleemissionafterdelete_thenreminderisnotrestored() {
        runTest {
            val nowMillis = 1_800_000_000_000L
            val futureReminder =
                Reminder(
                    itemId = testItemId,
                    mode = ReminderMode.AT_DATE,
                    targetEpochMillis = nowMillis + 60_000L,
                    selectedDateEpochMillis = nowMillis + 60_000L,
                    selectedHour = 12,
                    selectedMinute = 30,
                    status = ReminderStatus.ACTIVE,
                    createdAt = nowMillis - 1_000L,
                    updatedAt = nowMillis - 1_000L
                )

            repository.setItem(testItem)
            reminderManager.activeReminder = futureReminder
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId)),
                    reminderManager = reminderManager,
                    currentTimeMillisProvider = { nowMillis }
                )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.confirmDelete()
            testDispatcher.scheduler.advanceUntilIdle()

            // Имитируем "позднюю" повторную эмиссию элемента из потока.
            repository.setItem(testItem)
            testDispatcher.scheduler.advanceUntilIdle()

            val currentState = viewModel.uiState.value
            assertTrue(currentState is DetailScreenState.Success, "Состояние должно оставаться Success")
            val successState = currentState as DetailScreenState.Success
            assertEquals(null, successState.reminder, "Напоминание не должно восстанавливаться")
            assertEquals(listOf(testItemId), reminderManager.clearedItemIds, "Reminder должен очищаться один раз")
        }
    }

    @Test
    fun whencanceldelete_thenhidesdeletedialog() {
        runTest {
            // Given - ViewModel с показанным диалогом
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel =
                DetailScreenViewModel(
                    repository = repository,
                    logger = NoOpLogger(),
                    savedStateHandle = savedStateHandle,
                    reminderManager = reminderManager
                )
            viewModel.requestDelete()

            // When - Отменяем удаление
            viewModel.cancelDelete()

            // Then - Диалог должен быть скрыт, элемент должен остаться
            assertFalse(viewModel.showDeleteDialog.value, "Диалог удаления должен быть скрыт")
            assertTrue(repository.containsItem(testItemId), "Элемент должен остаться в repository")
        }
    }

    /**
     * Fake repository для тестов DetailScreenViewModel.
     * Минимальная реализация ItemRepository.
     */
    private class FakeItemRepository : ItemRepository {
        private val items = MutableStateFlow<List<Item>>(emptyList())

        fun setItem(item: Item) {
            items.value = listOf(item)
        }

        fun containsItem(id: Long): Boolean = items.value.any { it.id == id }

        override fun getAllItems(): Flow<List<Item>> = items

        override fun getAllItems(sortOrder: com.dayscounter.domain.model.SortOrder): Flow<List<Item>> = items

        override suspend fun getItemById(id: Long): Item? {
            // Без задержки для тестов - getItemFlow используется для загрузки в ViewModel
            return items.value.find { it.id == id }
        }

        override fun getItemFlow(id: Long): Flow<Item?> =
            items.map { itemList ->
                itemList.find { it.id == id }
            }

        override fun searchItems(query: String): Flow<List<Item>> =
            items.map { itemList ->
                itemList.filter { item ->
                    item.title.contains(query, ignoreCase = true) ||
                        item.details.contains(query, ignoreCase = true)
                }
            }

        override suspend fun insertItem(item: Item): Long {
            items.value = items.value + item
            return item.id
        }

        override suspend fun updateItem(item: Item) {
            items.value = items.value.map { if (it.id == item.id) item else it }
        }

        override suspend fun deleteItem(item: Item) {
            items.value = items.value.filterNot { it.id == item.id }
        }

        override suspend fun deleteAllItems() {
            items.value = emptyList()
        }

        override suspend fun getItemsCount(): Int = items.value.size
    }

    private class FakeReminderManager : ReminderManager {
        var activeReminder: Reminder? = null
        val clearedItemIds = mutableListOf<Long>()

        override suspend fun saveReminder(
            request: ReminderRequest,
            itemTitle: String
        ): Result<Unit> = Result.success(Unit)

        override suspend fun clearReminder(itemId: Long) {
            clearedItemIds += itemId
            activeReminder = null
        }

        override suspend fun getActiveReminder(itemId: Long): Reminder? = activeReminder?.takeIf { it.itemId == itemId }

        override suspend fun consumeReminder(itemId: Long) = Unit

        override suspend fun rescheduleFutureReminders() = Unit
    }
}
