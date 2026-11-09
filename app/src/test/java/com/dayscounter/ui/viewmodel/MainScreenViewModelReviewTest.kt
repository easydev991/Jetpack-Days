package com.dayscounter.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsProvider
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.ReviewAutoRequestOutcome
import com.dayscounter.analytics.ReviewErrorType
import com.dayscounter.data.preferences.ReviewCountersDataStore
import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.ReviewFlowOutcome
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ReviewFlowLauncher
import com.dayscounter.review.ReviewSessionHolder
import com.dayscounter.util.Logger
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

/**
 * Unit-тесты отложенного показа pending-milestone review-флоу на Events-экране
 * (openspec: add-rustore-in-app-review, задача 7.2).
 *
 * Виртуальное время coroutines-test покрывает delay перед запуском флоу.
 * Лаунчер — fake, хранилище счётчиков — реальный [ReviewCountersDataStore]
 * над тестовым DataStore (паттерн ReviewCountersDataStoreTest).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainScreenViewModelReviewTest {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: FakeItemRepository
    private lateinit var dataStore: com.dayscounter.data.preferences.AppSettingsDataStore
    private lateinit var logger: Logger
    private lateinit var analyticsEvents: MutableList<AnalyticsEvent>
    private lateinit var fakeLauncher: FakeReviewFlowLauncher

    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        ReviewSessionHolder.reset()
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repository = FakeItemRepository()
        dataStore = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        analyticsEvents = mutableListOf()
        fakeLauncher = FakeReviewFlowLauncher(ReviewFlowOutcome.Shown)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        dataStoreScopes.forEach { scope -> scope.cancel() }
    }

    @Test
    @DisplayName("Pending-milestone запускает флоу после delay: milestone пройден, pending снят, лог SHOWN")
    fun whenPendingMilestone_thenFlowLaunchedAfterDelayAndMilestonePassed() =
        runTest(testDispatcher) {
            // Given: pending запланирован, авто-запрос в сессии ещё не выполнялся
            val counters = createCounters()
            counters.setPendingMilestone(1)
            val viewModel = createViewModel(counters)

            // When: сразу после создания флоу ещё не запускался (delay 800 мс не истёк)
            runCurrent()

            // Then
            assertEquals(0, fakeLauncher.launchCalls, "До истечения delay флоу не должен запускаться")

            // When: виртуальное время прошло
            advanceTimeBy(1_000)
            runCurrent()

            // Then
            assertEquals(1, fakeLauncher.launchCalls, "Флоу должен запуститься один раз")
            assertTrue(ReviewSessionHolder.wasAutoRequestShown(), "Флаг сессии должен быть установлен до запуска")
            val snapshot = counters.state.first()
            assertTrue(
                snapshot.passedMilestones.contains(1),
                "Milestone должен быть помечен пройденным при запуске флоу"
            )
            assertNull(snapshot.pendingMilestone, "Pending должен быть снят")
            assertEquals(
                listOf(AnalyticsEvent.ReviewAutoRequest(ReviewAutoRequestOutcome.SHOWN)),
                analyticsEvents,
                "Должно быть залогировано событие SHOWN"
            )
        }

    @Test
    @DisplayName("Ошибка запуска флоу не бросается: milestone пройден, лог ERROR с доменным типом ошибки")
    fun whenFlowLaunchFails_thenErrorLoggedQuietlyAndMilestoneStillPassed() =
        runTest(testDispatcher) {
            // Given: pending запланирован, лаунчер возвращает доменную ошибку
            val counters = createCounters()
            counters.setPendingMilestone(5)
            fakeLauncher =
                FakeReviewFlowLauncher(
                    ReviewFlowOutcome.Error(ReviewException.NotInstalled("RuStore не установлен"))
                )
            val viewModel = createViewModel(counters)

            // When
            advanceTimeBy(1_000)
            runCurrent()

            // Then: ошибка тихая — без UI-состояния и без исключения
            assertEquals(1, fakeLauncher.launchCalls)
            assertEquals(MainScreenState.Success(emptyList()), viewModel.uiState.value)
            val snapshot = counters.state.first()
            assertTrue(snapshot.passedMilestones.contains(5), "Milestone помечается пройденным даже при ошибке")
            assertNull(snapshot.pendingMilestone)
            assertEquals(
                listOf(
                    AnalyticsEvent.ReviewAutoRequest(
                        ReviewAutoRequestOutcome.ERROR,
                        ReviewErrorType.NOT_INSTALLED
                    )
                ),
                analyticsEvents,
                "Должно быть залогировано событие ERROR с типом NOT_INSTALLED"
            )
        }

    @Test
    @DisplayName("Pending появился после создания VM: повторная проверка запускает флоу")
    fun whenPendingAppearsAfterViewModelCreation_thenRetryLaunchesFlow() =
        runTest(testDispatcher) {
            // Given: VM создана без pending (как при старте приложения)
            val counters = createCounters()
            val viewModel = createViewModel(counters)
            runCurrent()
            assertEquals(0, fakeLauncher.launchCalls)

            // When: запись создана — pending записан в DataStore уже после init,
            // затем пользователь вернулся на Events (повторная проверка)
            counters.setPendingMilestone(1)
            viewModel.checkPendingReviewRequest()
            advanceTimeBy(1_000)
            runCurrent()

            // Then: флоу запускается один раз, milestone пройден, pending снят
            assertEquals(1, fakeLauncher.launchCalls, "Повторная проверка должна запустить флоу")
            assertTrue(ReviewSessionHolder.wasAutoRequestShown())
            val snapshot = counters.state.first()
            assertTrue(snapshot.passedMilestones.contains(1))
            assertNull(snapshot.pendingMilestone)
            assertEquals(
                listOf(AnalyticsEvent.ReviewAutoRequest(ReviewAutoRequestOutcome.SHOWN)),
                analyticsEvents
            )
        }

    @Test
    @DisplayName("Без pending флоу не запускается")
    fun whenNoPendingMilestone_thenFlowIsNotLaunched() =
        runTest(testDispatcher) {
            // Given: счётчики без pending
            val counters = createCounters()
            val viewModel = createViewModel(counters)

            // When
            advanceTimeBy(2_000)
            runCurrent()

            // Then
            assertEquals(0, fakeLauncher.launchCalls, "Без pending флоу не должен запускаться")
            assertTrue(analyticsEvents.isEmpty())
        }

    @Test
    @DisplayName("Сбой файла счётчиков при чтении не роняет экран и не запускает флоу")
    fun whenCountersReadFails_thenNoCrashAndNoFlow() =
        runTest(testDispatcher) {
            // Given: хранилище над DataStore, падающим на чтении
            val counters = ReviewCountersDataStore(ThrowingDataStore())
            val viewModel = createViewModel(counters)

            // When
            advanceTimeBy(2_000)
            runCurrent()

            // Then: IOException проглочен, флоу не запускается, аналитика пустая
            assertEquals(0, fakeLauncher.launchCalls, "При сбое чтения счётчиков флоу не должен запускаться")
            assertTrue(analyticsEvents.isEmpty())
        }

    @Test
    @DisplayName("Сбой файла счётчиков при отметке milestone не роняет экран и не запускает флоу")
    fun whenMarkMilestoneFails_thenNoCrashAndNoFlow() =
        runTest(testDispatcher) {
            // Given: pending=1 записан рабочим хранилищем, но отметка milestone
            // падает — обёртка с delegates-доступом на чтение и сбоем на записи
            val scope = CoroutineScope(SupervisorJob() + testDispatcher)
            dataStoreScopes.add(scope)
            val dataStore =
                PreferenceDataStoreFactory.create(scope = scope) {
                    File(tempDir, "review_mark_${System.nanoTime()}.preferences_pb")
                }
            val working = ReviewCountersDataStore(dataStore)
            working.setPendingMilestone(1)
            val counters =
                ReviewCountersDataStore(
                    object : DataStore<Preferences> by dataStore {
                        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                            throw IOException("disk failure")
                    }
                )
            val viewModel = createViewModel(counters)

            // When
            advanceTimeBy(2_000)
            runCurrent()

            // Then: IOException проглочен, флоу не запускается, аналитика пустая
            assertEquals(0, fakeLauncher.launchCalls, "При сбое отметки milestone флоу не должен запускаться")
            assertTrue(analyticsEvents.isEmpty())
        }

    @Test
    @DisplayName("Pending снят за окно ожидания: флоу не запускается и ничего не помечается")
    fun whenPendingClearedDuringDelay_thenFlowIsNotLaunched() =
        runTest(testDispatcher) {
            // Given: pending=1 запланирован
            val counters = createCounters()
            counters.setPendingMilestone(1)
            val viewModel = createViewModel(counters)
            runCurrent()

            // When: в окне ожидания пользователь удалил все данные — счётчики сброшены
            counters.reset()
            advanceTimeBy(1_000)
            runCurrent()

            // Then: флоу не запускается, milestone не помечен пройденным, holder не закрыт
            assertEquals(0, fakeLauncher.launchCalls, "Показ после снятия pending не должен запускаться")
            assertTrue(analyticsEvents.isEmpty())
            val snapshot = counters.state.first()
            assertTrue(
                snapshot.passedMilestones.isEmpty(),
                "Milestone не должен быть помечен пройденным после сброса данных"
            )
            assertFalse(
                ReviewSessionHolder.wasAutoRequestShown(),
                "Флаг сессии не должен закрываться, если pending снят за окно ожидания"
            )
        }

    @Test
    @DisplayName("Повторного авто-запроса в сессии нет: holder блокирует запуск")
    fun whenAutoRequestAlreadyShownThisSession_thenFlowIsNotLaunchedAgain() =
        runTest(testDispatcher) {
            // Given: авто-запрос уже выполнялся в этой сессии (holder пишет только 7.2)
            ReviewSessionHolder.markAutoRequestShown()
            val counters = createCounters()
            counters.setPendingMilestone(1)
            val viewModel = createViewModel(counters)

            // When
            advanceTimeBy(2_000)
            runCurrent()

            // Then
            assertEquals(0, fakeLauncher.launchCalls, "Второй авто-запрос в сессии должен быть заблокирован")
            assertTrue(analyticsEvents.isEmpty())
        }

    @Test
    @DisplayName("Отключённая review-фича (github flavor) не запускает флоу")
    fun whenReviewDisabled_thenFlowIsNotLaunched() =
        runTest(testDispatcher) {
            // Given: фича выключена
            val counters = createCounters()
            counters.setPendingMilestone(1)
            val viewModel = createViewModel(counters, isReviewEnabled = false)

            // When
            advanceTimeBy(2_000)
            runCurrent()

            // Then
            assertEquals(0, fakeLauncher.launchCalls)
            assertTrue(analyticsEvents.isEmpty())
        }

    /**
     * Собирает ViewModel с review-зависимостями поверх fake-лаунчера и сервиса
     * аналитики, пишущего события в [analyticsEvents].
     */
    private fun createViewModel(
        counters: ReviewCountersDataStore,
        isReviewEnabled: Boolean = true
    ): MainScreenViewModel =
        MainScreenViewModel(
            repository = repository,
            dataStore = dataStore,
            logger = logger,
            reviewDeps =
                if (isReviewEnabled) {
                    ReviewTriggerDependencies(
                        counters = counters,
                        flowLauncher = fakeLauncher,
                        isReviewEnabled = true,
                        analytics =
                            AnalyticsService(
                                providers =
                                    listOf(
                                        object : AnalyticsProvider {
                                            override fun log(event: AnalyticsEvent) {
                                                analyticsEvents.add(event)
                                            }
                                        }
                                    )
                            )
                    )
                } else {
                    null
                }
        )

    /** Создаёт реальное хранилище счётчиков над тестовым DataStore (JVM). */
    private fun createCounters(): ReviewCountersDataStore {
        val scope = CoroutineScope(SupervisorJob() + testDispatcher)
        dataStoreScopes.add(scope)
        val dataStore: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope) {
                File(tempDir, "review_${System.nanoTime()}.preferences_pb")
            }
        return ReviewCountersDataStore(dataStore)
    }

    /** Fake-лаунчер, считающий вызовы launch() и возвращающий заданный исход. */
    private class FakeReviewFlowLauncher(
        private val outcome: ReviewFlowOutcome
    ) : ReviewFlowLauncher {
        var launchCalls: Int = 0

        override suspend fun launch(): ReviewFlowOutcome {
            launchCalls += 1
            return outcome
        }
    }

    /** DataStore, имитирующий сбой файла: IOException на любом чтении и записи. */
    private class ThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("disk failure") }

        override suspend fun updateData(
            @Suppress("UNUSED_PARAMETER") transform: suspend (Preferences) -> Preferences
        ): Preferences = throw IOException("disk failure")
    }

    /** Минимальный in-memory репозиторий для наблюдения списка событий. */
    private class FakeItemRepository : ItemRepository {
        private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())

        override fun getAllItems(): Flow<List<Item>> = itemsFlow

        override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> = itemsFlow

        override fun searchItems(query: String): Flow<List<Item>> = flow { emit(emptyList()) }

        override suspend fun getItemById(id: Long): Item? = null

        override fun getItemFlow(id: Long): Flow<Item?> = flow { emit(null) }

        override suspend fun insertItem(item: Item): Long = itemsFlow.value.size + 1L

        override suspend fun updateItem(item: Item) = Unit

        override suspend fun deleteItem(item: Item) = Unit

        override suspend fun deleteAllItems() = Unit

        override suspend fun getItemsCount(): Int = itemsFlow.value.size
    }
}
