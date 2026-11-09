package com.dayscounter.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Unit-тесты хранилища счётчиков review-триггеров (openspec: add-rustore-in-app-review,
 * задачи 3.1/3.2, design.md D3).
 *
 * DataStore гоняется на JVM без Android-окружения: инстансы строятся напрямую через
 * PreferenceDataStoreFactory.create над временным файлом, скоуп каждого инстанса
 * привязан к тестовому планировщику runTest.
 */
class ReviewCountersDataStoreTest {
    /** Скоупы инстансов DataStore — закрываются в tearDown, чтобы освободить файлы. */
    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @TempDir
    lateinit var tempDir: File

    @AfterEach
    fun tearDown() {
        dataStoreScopes.forEach { scope -> scope.cancel() }
    }

    @Test
    @DisplayName("Свежее хранилище возвращает нулевой счётчик, пустой набор milestones и null pending")
    fun state_when_file_is_fresh_then_returns_defaults() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))

            // When
            val snapshot = store.state.first()

            // Then
            assertEquals(
                ReviewCountersSnapshot(createdItemsCount = 0, passedMilestones = emptySet(), pendingMilestone = null),
                snapshot,
                "Свежее хранилище должно отдавать значения по умолчанию"
            )
        }

    @Test
    @DisplayName("Инкремент увеличивает счётчик и возвращает новое значение")
    fun incrementCreatedItemsCount_when_called_then_returns_new_value_and_persists() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))

            // When
            val firstIncrement = store.incrementCreatedItemsCount()
            val secondIncrement = store.incrementCreatedItemsCount()

            // Then
            assertEquals(1, firstIncrement, "Первый инкремент должен вернуть 1")
            assertEquals(2, secondIncrement, "Второй инкремент должен вернуть 2")
            assertEquals(
                2,
                store.state.first().createdItemsCount,
                "Счётчик в снимке должен быть равен числу инкрементов"
            )
        }

    @Test
    @DisplayName("setPendingMilestone записывает pending, null — снимает его")
    fun setPendingMilestone_when_set_then_pending_readable_when_null_then_cleared() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))

            // When
            store.setPendingMilestone(5)

            // Then
            assertEquals(5, store.state.first().pendingMilestone, "Pending должен читаться из снимка")

            // When
            store.setPendingMilestone(null)

            // Then
            assertNull(store.state.first().pendingMilestone, "null должен снимать pending")
        }

    @Test
    @DisplayName("markMilestonePassed добавляет milestone в набор пройденных")
    fun markMilestonePassed_when_called_then_milestone_in_passed_set() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))

            // When
            store.markMilestonePassed(1)
            store.markMilestonePassed(15)

            // Then
            assertEquals(
                setOf(1, 15),
                store.state.first().passedMilestones,
                "Оба milestone должны оказаться в наборе пройденных"
            )
        }

    @Test
    @DisplayName("markMilestonePassed снимает pending, если отмечен именно он")
    fun markMilestonePassed_when_marking_pending_then_pending_cleared() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))
            store.setPendingMilestone(5)

            // When
            store.markMilestonePassed(5)

            // Then
            val snapshot = store.state.first()
            assertNull(snapshot.pendingMilestone, "Показанный pending должен быть снят")
            assertEquals(setOf(5), snapshot.passedMilestones, "Отмеченный milestone должен быть пройденным")
        }

    @Test
    @DisplayName("markMilestonePassed не трогает pending другого milestone")
    fun markMilestonePassed_when_marking_other_milestone_then_pending_kept() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))
            store.setPendingMilestone(5)

            // When
            store.markMilestonePassed(1)

            // Then
            val snapshot = store.state.first()
            assertEquals(5, snapshot.pendingMilestone, "Pending другого milestone должен остаться на месте")
            assertEquals(setOf(1), snapshot.passedMilestones, "Отмеченный milestone должен быть пройденным")
        }

    @Test
    @DisplayName("Сброс очищает счётчик, пройденные milestones и pending")
    fun reset_when_called_then_all_counters_cleared() =
        runTest {
            // Given
            val store = ReviewCountersDataStore(createDataStore(testScheduler))
            store.incrementCreatedItemsCount()
            store.incrementCreatedItemsCount()
            store.markMilestonePassed(1)
            store.setPendingMilestone(5)

            // When
            store.reset()

            // Then
            assertEquals(
                ReviewCountersSnapshot(createdItemsCount = 0, passedMilestones = emptySet(), pendingMilestone = null),
                store.state.first(),
                "После сброса хранилище должно вернуться к значениям по умолчанию"
            )
        }

    @Test
    @DisplayName("Новый инстанс хранилища над тем же файлом видит счётчики предыдущего")
    fun state_when_second_instance_over_same_file_then_counters_persist() =
        runTest {
            // Given
            val file = File(tempDir, STORAGE_FILE_NAME)
            val firstScope =
                CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
                    .also { dataStoreScopes.add(it) }
            val firstStore =
                ReviewCountersDataStore(PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file }))
            firstStore.incrementCreatedItemsCount()
            firstStore.setPendingMilestone(5)
            firstStore.markMilestonePassed(1)

            // When: первый инстанс закрывается — два живых DataStore на одном файле запрещены
            firstScope.cancel()
            testScheduler.advanceUntilIdle()
            val secondStore = ReviewCountersDataStore(createDataStore(testScheduler, file))

            // Then
            assertEquals(
                ReviewCountersSnapshot(createdItemsCount = 1, passedMilestones = setOf(1), pendingMilestone = 5),
                secondStore.state.first(),
                "Счётчики должны переживать пересоздание инстанса хранилища"
            )
        }

    /**
     * Строит DataStore над файлом (по умолчанию — файл хранилища счётчиков во
     * временном каталоге теста) со скоупом на тестовом планировщике.
     */
    private fun createDataStore(
        scheduler: TestCoroutineScheduler,
        file: File = File(tempDir, STORAGE_FILE_NAME)
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope =
                CoroutineScope(StandardTestDispatcher(scheduler) + SupervisorJob())
                    .also { dataStoreScopes.add(it) },
            produceFile = { file }
        )

    companion object {
        /** DataStore требует расширение .preferences_pb (проверка внутри фабрики). */
        private const val STORAGE_FILE_NAME = "review_counters.preferences_pb"
    }
}
