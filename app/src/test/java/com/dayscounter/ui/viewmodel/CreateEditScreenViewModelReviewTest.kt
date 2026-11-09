package com.dayscounter.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.NoopAnalyticsProvider
import com.dayscounter.data.preferences.ReviewCountersDataStore
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ReminderRequest
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.LocalDate

/**
 * Unit-тесты review-хука [CreateEditScreenViewModel.saveItem]
 * (openspec: add-rustore-in-app-review, задача 7.1).
 *
 * Хранилище счётчиков — реальный [ReviewCountersDataStore] над тестовым DataStore
 * (паттерн [ReviewCountersDataStoreTest]); holder сессии подменяется лямбдой в
 * [CreateEditScreenViewModel.Deps], поэтому singleton не влияет на изоляцию тестов.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreateEditScreenViewModelReviewTest {
    private val dispatcher = StandardTestDispatcher()
    private val analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
    private val repository = FakeItemRepository()
    private val reminderManager = FakeReminderManager()
    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        dataStoreScopes.forEach { scope -> scope.cancel() }
    }

    @Test
    @DisplayName("Создание записи при isReviewEnabled=true увеличивает счётчик и планирует milestone 1")
    fun saveItem_whenCreateAndReviewEnabled_thenIncrementsCounterAndSchedulesFirstMilestone() =
        runTest {
            // Given
            val reviewCounters = createReviewCounters(testScheduler)
            val viewModel = createViewModel(isReviewEnabled = true, reviewCounters = reviewCounters)

            // When
            viewModel.saveItem(newItem(), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            val snapshot = reviewCounters.state.first()
            assertEquals(1, snapshot.createdItemsCount, "Счётчик созданных записей должен увеличиться на 1")
            assertEquals(1, snapshot.pendingMilestone, "Первый достигнутый milestone 1 должен быть запланирован")
        }

    @Test
    @DisplayName("Редактирование записи счётчик не увеличивает")
    fun saveItem_whenEditing_thenCounterNotIncremented() =
        runTest {
            // Given
            val reviewCounters = createReviewCounters(testScheduler)
            repository.seed(Item(id = 5L, title = "Существующая запись", timestamp = 1_700_000_000_000L))
            val viewModel =
                createViewModel(
                    isReviewEnabled = true,
                    reviewCounters = reviewCounters,
                    savedStateHandle = SavedStateHandle(mapOf("itemId" to 5L))
                )
            dispatcher.scheduler.advanceUntilIdle()

            // When
            viewModel.saveItem(Item(id = 5L, title = "Обновлено", timestamp = 1_700_000_000_000L), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                0,
                reviewCounters.state.first().createdItemsCount,
                "Редактирование не должно увеличивать счётчик созданных записей"
            )
        }

    @Test
    @DisplayName("Создание при isReviewEnabled=false счётчик не увеличивает")
    fun saveItem_whenCreateAndReviewDisabled_thenCounterNotIncremented() =
        runTest {
            // Given
            val reviewCounters = createReviewCounters(testScheduler)
            val viewModel = createViewModel(isReviewEnabled = false, reviewCounters = reviewCounters)

            // When
            viewModel.saveItem(newItem(), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                0,
                reviewCounters.state.first().createdItemsCount,
                "При выключенной review-фиче счётчик не должен меняться"
            )
        }

    @Test
    @DisplayName("Частичный успех (reminder упал) счётчик не увеличивает")
    fun saveItem_whenReminderFails_thenCounterNotIncremented() =
        runTest {
            // Given
            val reviewCounters = createReviewCounters(testScheduler)
            val viewModel = createViewModel(isReviewEnabled = true, reviewCounters = reviewCounters)
            reminderManager.shouldFailOnSave = true

            // When
            viewModel.saveItem(
                newItem(),
                reminderRequest =
                    ReminderRequest(
                        itemId = 0L,
                        mode = ReminderMode.AT_DATE,
                        atDate = LocalDate.of(2030, 1, 1)
                    )
            )
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is CreateEditScreenState.Error, "Ожидается состояние ошибки")
            assertEquals(
                0,
                reviewCounters.state.first().createdItemsCount,
                "Хук должен выполняться только после успешного сохранения reminder-а"
            )
        }

    @Test
    @DisplayName("Авто-запрос уже выполнялся в сессии: счётчик растёт, но pending не планируется")
    fun saveItem_whenAutoRequestAlreadyShown_thenCounterGrowsButNoPendingScheduled() =
        runTest {
            // Given
            val reviewCounters = createReviewCounters(testScheduler)
            val viewModel =
                createViewModel(
                    isReviewEnabled = true,
                    reviewCounters = reviewCounters,
                    wasAutoRequestShownThisSession = { true }
                )

            // When
            viewModel.saveItem(newItem(), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            val snapshot = reviewCounters.state.first()
            assertEquals(1, snapshot.createdItemsCount, "Счётчик инкрементируется независимо от лимита сессии")
            assertEquals(null, snapshot.pendingMilestone, "Новый pending при исчерпанном лимите сессии не планируется")
        }

    @Test
    @DisplayName("Существующий pending не перезаписывается новым решением")
    fun saveItem_whenPendingExists_thenPendingKeptAndNoNewDecision() =
        runTest {
            // Given
            val reviewCounters = createReviewCounters(testScheduler)
            reviewCounters.setPendingMilestone(1)
            val viewModel = createViewModel(isReviewEnabled = true, reviewCounters = reviewCounters)

            // When
            viewModel.saveItem(newItem(), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            val snapshot = reviewCounters.state.first()
            assertEquals(1, snapshot.createdItemsCount, "Счётчик должен инкрементироваться")
            assertEquals(1, snapshot.pendingMilestone, "Существующий pending должен остаться на месте")
        }

    @Test
    @DisplayName("Сбой файла счётчиков не ломает успешное сохранение записи")
    fun saveItem_whenCountersDataStoreFails_thenSaveStillSucceeds() =
        runTest {
            // Given: хранилище счётчиков над DataStore, падающим на любой операции
            val viewModel =
                createViewModel(
                    isReviewEnabled = true,
                    reviewCounters = ReviewCountersDataStore(ThrowingDataStore())
                )

            // When
            viewModel.saveItem(newItem(), reminderRequest = null)
            dispatcher.scheduler.advanceUntilIdle()

            // Then: IOException из DataStore проглочен review-хуком, запись сохранена
            assertTrue(
                viewModel.uiState.value !is CreateEditScreenState.Error,
                "Сбой счётчиков не должен переводить сохранение в состояние ошибки"
            )
            assertEquals(1, repository.getItemsCount(), "Запись должна быть сохранена в репозиторий")
        }

    /**
     * Создаёт [CreateEditScreenViewModel] с тестовыми зависимостями.
     */
    private fun createViewModel(
        isReviewEnabled: Boolean,
        reviewCounters: ReviewCountersDataStore,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        wasAutoRequestShownThisSession: () -> Boolean = { false }
    ): CreateEditScreenViewModel =
        CreateEditScreenViewModel(
            deps =
                CreateEditScreenViewModel.Deps(
                    repository = repository,
                    resourceProvider = FakeResourceProvider(),
                    analyticsService = analyticsService,
                    reminderManager = reminderManager,
                    logger = NoOpLogger(),
                    isReviewEnabled = isReviewEnabled,
                    reviewCountersDataStore = reviewCounters,
                    wasAutoRequestShownThisSession = wasAutoRequestShownThisSession
                ),
            savedStateHandle = savedStateHandle
        )

    private fun newItem(): Item = Item(id = 0L, title = "Новая запись", timestamp = 1_700_000_000_000L)

    /** Строит хранилище счётчиков над файлом во временном каталоге теста. */
    private fun createReviewCounters(scheduler: TestCoroutineScheduler): ReviewCountersDataStore =
        ReviewCountersDataStore(createDataStore(scheduler))

    private fun createDataStore(scheduler: TestCoroutineScheduler): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope =
                CoroutineScope(StandardTestDispatcher(scheduler) + SupervisorJob())
                    .also { dataStoreScopes.add(it) },
            produceFile = { File(tempDir, "review_counters.preferences_pb") }
        )

    /** DataStore, имитирующий сбой файла: IOException на любом чтении и записи. */
    private class ThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("disk failure") }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw IOException("disk failure")
    }

    private class FakeItemRepository : ItemRepository {
        private val items = MutableStateFlow<Map<Long, Item>>(emptyMap())
        private var nextId = 101L

        fun seed(item: Item) {
            items.value += (item.id to item)
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
            items.value += (id to item.copy(id = id))
            return id
        }

        override suspend fun updateItem(item: Item) {
            items.value += (item.id to item)
        }

        override suspend fun deleteItem(item: Item) {
            items.value -= item.id
        }

        override suspend fun deleteAllItems() {
            items.value = emptyMap()
        }

        override suspend fun getItemsCount(): Int = items.value.size
    }

    private class FakeReminderManager : ReminderManager {
        private val storedReminders = mutableMapOf<Long, Reminder>()

        var shouldFailOnSave: Boolean = false

        override suspend fun saveReminder(
            request: ReminderRequest,
            @Suppress("UNUSED_PARAMETER") itemTitle: String
        ): Result<Unit> {
            if (shouldFailOnSave) {
                return Result.failure(IllegalStateException("save failed"))
            }
            return Result.success(Unit)
        }

        override suspend fun clearReminder(itemId: Long) {
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
        ): String = "Строка по умолчанию: $resId"
    }
}
