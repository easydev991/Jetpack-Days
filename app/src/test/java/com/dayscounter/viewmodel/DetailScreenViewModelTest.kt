package com.dayscounter.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
            displayOption = com.dayscounter.domain.model.DisplayOption.DAY,
        )

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `whenViewModelCreated_thenStartsWithLoadingState`() {
        runTest {
            // Given - ViewModel создан без элемента
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Then - Начальное состояние должно быть Loading
            val initialState = viewModel.uiState.value
            assertTrue(
                initialState is DetailScreenState.Loading,
                "Начальное состояние должно быть Loading",
            )
        }
    }

    @Test
    fun `whenItemNotFound_thenRemainsInLoadingState`() {
        runTest {
            // Given - Repository не содержит элемент
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // When - Пытаемся загрузить несуществующий элемент
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Должно остаться в состоянии Loading
            val currentState = viewModel.uiState.value
            assertTrue(
                currentState is DetailScreenState.Loading,
                "Должно остаться в состоянии Loading",
            )
        }
    }

    @Test
    fun `whenItemLoadedSuccessfully_thenUpdatesToSuccessState`() {
        runTest {
            // Given - Repository содержит элемент
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))

            // When - Создаем ViewModel
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
            // Создаем подписку на StateFlow, чтобы запустить stateIn (WhileSubscribed требует наблюдателей)
            backgroundScope.launch {
                viewModel.uiState.collect {}
            }
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Состояние должно измениться на Success с загруженным элементом
            val currentState = viewModel.uiState.value
            assertTrue(
                currentState is DetailScreenState.Success,
                "Состояние должно быть Success, фактическое: $currentState",
            )
            val successState = currentState as DetailScreenState.Success
            assertEquals(testItem, successState.item, "Элемент должен совпадать")
        }
    }

    @Test
    fun `whenRequestDelete_thenShowsDeleteDialog`() {
        runTest {
            // Given - ViewModel с загруженным элементом
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
            // Создаем подписку на StateFlow, чтобы запустить stateIn
            backgroundScope.launch {
                viewModel.uiState.collect {}
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Запрашиваем удаление
            viewModel.requestDelete()

            // Then - Диалог удаления должен быть показан
            assertTrue(viewModel.showDeleteDialog.value, "Диалог удаления должен быть показан")
        }
    }

    @Test
    fun `whenConfirmDelete_thenDeletesItem`() {
        runTest {
            // Given - ViewModel с загруженным элементом
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
            // Создаем подписку на StateFlow, чтобы запустить stateIn
            backgroundScope.launch {
                viewModel.uiState.collect {}
            }
            // Ждем загрузки элемента
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Подтверждаем удаление
            viewModel.confirmDelete()
            testDispatcher.scheduler.advanceUntilIdle()
            testDispatcher.scheduler.runCurrent()

            // Then - Элемент должен быть удален и диалог скрыт
            assertFalse(repository.containsItem(testItemId), "Элемент должен быть удален из repository")
            assertFalse(viewModel.showDeleteDialog.value, "Диалог удаления должен быть скрыт")
            // stateIn кэширует последнее значение, поэтому состояние остается Success
            val currentState = viewModel.uiState.value
            assertTrue(
                currentState is DetailScreenState.Success,
                "После удаления состояние остается Success " +
                    "(stateIn кэширует последнее значение), фактическое: $currentState",
            )
        }
    }

    @Test
    fun `whenCancelDelete_thenHidesDeleteDialog`() {
        runTest {
            // Given - ViewModel с показанным диалогом
            repository.setItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
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
}
