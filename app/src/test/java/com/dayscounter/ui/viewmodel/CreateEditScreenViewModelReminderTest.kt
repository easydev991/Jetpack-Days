package com.dayscounter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.NoopAnalyticsProvider
import com.dayscounter.data.provider.ResourceIds
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ReminderRequest
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreateEditScreenViewModelReminderTest {
    private val dispatcher = StandardTestDispatcher()
    private val analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
    private lateinit var repository: FakeItemRepository
    private lateinit var reminderManager: FakeReminderManager
    private lateinit var viewModel: CreateEditScreenViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeItemRepository()
        reminderManager = FakeReminderManager()

        viewModel =
            CreateEditScreenViewModel(
                repository = repository,
                resourceProvider = FakeResourceProvider(),
                logger = NoOpLogger(),
                savedStateHandle = SavedStateHandle(),
                analyticsService = analyticsService,
                reminderManager = reminderManager
            )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveItem_whenNewItemAndReminderEnabled_thenSavesReminderWithInsertedItemId() {
        runTest {
            // Given
            val item =
                Item(
                    id = 0L,
                    title = "Новая запись",
                    details = "Описание",
                    timestamp = 1_700_000_000_000L,
                    colorTag = null,
                    displayOption = DisplayOption.DAY
                )
            val reminderRequest =
                ReminderRequest(
                    itemId = 0L,
                    mode = ReminderMode.AT_DATE,
                    atDate = LocalDate.now().plusDays(2),
                    atTime = LocalTime.of(10, 0)
                )

            // When
            viewModel.saveItem(item, reminderRequest)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            val savedRequest = reminderManager.savedRequests.single()
            assertEquals(101L, savedRequest.itemId)
            val state = viewModel.uiState.value as CreateEditScreenState.Success
            assertEquals(101L, state.item.id)
        }
    }

    @Test
    fun saveItem_whenReminderDisabled_thenClearsReminderForSavedItem() {
        runTest {
            // Given
            val existingItem =
                Item(
                    id = 5L,
                    title = "Существующая запись",
                    details = "Описание",
                    timestamp = 1_700_000_000_000L,
                    colorTag = null,
                    displayOption = DisplayOption.DAY
                )
            repository.seed(existingItem)

            val editViewModel =
                CreateEditScreenViewModel(
                    repository = repository,
                    resourceProvider = FakeResourceProvider(),
                    logger = NoOpLogger(),
                    savedStateHandle = SavedStateHandle(mapOf("itemId" to 5L)),
                    analyticsService = analyticsService,
                    reminderManager = reminderManager
                )
            dispatcher.scheduler.advanceUntilIdle()

            // When
            editViewModel.saveItem(existingItem.copy(title = "Обновлено"), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(5L, reminderManager.lastClearedItemId)
            val state = editViewModel.uiState.value as CreateEditScreenState.Success
            assertEquals("Обновлено", state.item.title)
        }
    }

    @Test
    fun saveItem_whenReminderManagerFails_thenShowsErrorState() {
        runTest {
            // Given
            reminderManager.shouldFailOnSave = true
            val item =
                Item(
                    id = 0L,
                    title = "Новая запись",
                    details = "Описание",
                    timestamp = 1_700_000_000_000L,
                    colorTag = null,
                    displayOption = DisplayOption.DAY
                )
            val reminderRequest =
                ReminderRequest(
                    itemId = 0L,
                    mode = ReminderMode.AT_DATE,
                    atDate = LocalDate.now().plusDays(1),
                    atTime = LocalTime.of(9, 0)
                )

            // When
            viewModel.saveItem(item, reminderRequest)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is CreateEditScreenState.Error)
        }
    }

    private class FakeItemRepository : ItemRepository {
        private val items = MutableStateFlow<Map<Long, Item>>(emptyMap())
        private var nextId = 101L

        fun seed(item: Item) {
            items.value = items.value + (item.id to item)
            if (item.id >= nextId) {
                nextId = item.id + 1
            }
        }

        override fun getAllItems(): Flow<List<Item>> = flowOf(emptyList())

        override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun getItemById(id: Long): Item? = items.value[id]

        override fun getItemFlow(id: Long): Flow<Item?> = items.map { state -> state[id] }

        override fun searchItems(query: String): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun insertItem(item: Item): Long {
            val id = if (item.id == 0L) nextId++ else item.id
            items.value = items.value + (id to item.copy(id = id))
            return id
        }

        override suspend fun updateItem(item: Item) {
            items.value = items.value + (item.id to item)
        }

        override suspend fun deleteItem(item: Item) {
            items.value = items.value - item.id
        }

        override suspend fun deleteAllItems() {
            items.value = emptyMap()
        }

        override suspend fun getItemsCount(): Int = items.value.size
    }

    private class FakeReminderManager : ReminderManager {
        val savedRequests = mutableListOf<ReminderRequest>()
        var lastClearedItemId: Long? = null
        var shouldFailOnSave: Boolean = false
        private val storedReminders = mutableMapOf<Long, Reminder>()

        override suspend fun saveReminder(
            request: ReminderRequest,
            itemTitle: String
        ): Result<Unit> {
            if (shouldFailOnSave) {
                return Result.failure(IllegalStateException("save failed"))
            }

            savedRequests += request
            return Result.success(Unit)
        }

        override suspend fun clearReminder(itemId: Long) {
            lastClearedItemId = itemId
            storedReminders.remove(itemId)
        }

        override suspend fun getActiveReminder(itemId: Long): Reminder? = storedReminders[itemId]

        override suspend fun consumeReminder(itemId: Long) = Unit

        override suspend fun rescheduleFutureReminders() = Unit
    }

    private class FakeResourceProvider : ResourceProvider {
        override fun getString(
            resId: Int,
            vararg formatArgs: Any
        ): String =
            when (resId) {
                ResourceIds.EVENT_NOT_FOUND -> "Событие не найдено"
                ResourceIds.ERROR_CREATING_EVENT -> "Ошибка создания события: ${formatArgs.joinToString()}"
                ResourceIds.ERROR_UPDATING_EVENT -> "Ошибка обновления события: ${formatArgs.joinToString()}"
                ResourceIds.ERROR_LOADING_EVENT -> "Ошибка загрузки события: ${formatArgs.joinToString()}"
                else -> "Строка по умолчанию: $resId"
            }

        override fun getQuantityString(
            resId: Int,
            quantity: Int,
            vararg formatArgs: Any
        ): String = "$quantity"

        override fun getYearsString(quantity: Int): String = "$quantity год"

        override fun getMonthsString(quantity: Int): String = "$quantity месяц"
    }
}
